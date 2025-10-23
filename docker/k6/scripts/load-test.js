import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';
import { BASE_URL, DATA_RANGES, TEST_USERS } from '../utils/config.js';

// Метрики
const errorRate = new Rate('errors');
const loginDuration = new Trend('login_duration');
const requestCounter = new Counter('requests_by_type');

// Конфигурация
export const options = {
    stages: [
        { duration: '2m', target: 10 },   // Разогрев: 0 → 10 VUs
        { duration: '5m', target: 50 },   // Рампа: 10 → 50 VUs
        { duration: '2m', target: 50 },   // Плато: 50 VUs
        { duration: '1m', target: 0 }     // Спад: 50 → 0 VUs
    ],

    thresholds: {
        'http_req_duration': ['p(95)<1000', 'p(99)<2000'],  // 95% < 1s, 99% < 2s
        'http_req_failed': ['rate<0.05'],  // < 5% ошибок
        'errors': ['rate<0.05'],
        'checks': ['rate>0.90']  // 90% проверок успешны
    },

    tags: {
        test_type: 'load',
        environment: 'docker'
    }
};

// Утилиты
function randomInt(min, max) {
    return Math.floor(Math.random() * (max - min + 1)) + min;
}

function randomElement(array) {
    return array[Math.floor(Math.random() * array.length)];
}

// Логин пользователя
function login(user) {
    const payload = JSON.stringify({
        username: user.username,
        password: user.password
    });

    const params = {
        headers: { 'Content-Type': 'application/json' },
        tags: { endpoint: 'login' }
    };

    const start = Date.now();
    const res = http.post(`${BASE_URL}/api/auth/login`, payload, params);
    loginDuration.add(Date.now() - start);

    const success = check(res, {
        'login: status 200': (r) => r.status === 200,
        'login: has token': (r) => {
            try {
                const body = JSON.parse(r.body);
                return body.token && body.token.length > 0;
            } catch (e) {
                return false;
            }
        }
    });

    errorRate.add(!success);

    if (res.status === 200) {
        try {
            return JSON.parse(res.body).token;
        } catch (e) {
            return null;
        }
    }

    return null;
}

// Главная функция
export default function () {
    // 70% - гости, 30% - авторизованные
    const isGuest = Math.random() < 0.70;
    let token = null;

    if (!isGuest) {
        // Логин
        const user = randomElement(TEST_USERS.users);
        token = login(user);

        if (!token) {
            sleep(1);
            return;
        }
    }

    // Сценарии
    const scenario = Math.random();

    // 1. Просмотр списка книг (40%)
    if (scenario < 0.40) {
        group('Books List', () => {
            const page = randomInt(0, 10);
            const res = http.get(`${BASE_URL}/api/books?page=${page}&size=10`, {
                tags: { endpoint: 'books_list', user_type: isGuest ? 'guest' : 'user' }
            });

            check(res, {
                'books list: status 200': (r) => r.status === 200
            });

            errorRate.add(res.status !== 200);
            requestCounter.add(1, { type: 'books_list' });
        });
    }

    // 2. Детали книги + обложка (25%)
    else if (scenario < 0.65) {
        group('Book Details', () => {
            const bookId = randomInt(DATA_RANGES.MIN_BOOK_ID, DATA_RANGES.MAX_BOOK_ID);

            // Детали
            const detailsRes = http.get(`${BASE_URL}/api/books/${bookId}`, {
                tags: { endpoint: 'book_details' }
            });

            check(detailsRes, {
                'book details: status 200': (r) => r.status === 200
            });

            // Обложка
            const coverRes = http.get(`${BASE_URL}/api/books/${bookId}/cover`, {
                tags: { endpoint: 'book_cover' }
            });

            check(coverRes, {
                'cover: valid status': (r) => r.status === 200 || r.status === 404
            });

            errorRate.add(detailsRes.status !== 200 || coverRes.status >= 500);
            requestCounter.add(2, { type: 'book_details' });
        });
    }

    // 3. Поиск книг (15%)
    else if (scenario < 0.80) {
        group('Search Books', () => {
            const searchParams = Math.random() < 0.5
                ? `title=${encodeURIComponent('книга')}`
                : `genreIds=${randomInt(1, DATA_RANGES.TOTAL_GENRES)}`;

            const res = http.get(`${BASE_URL}/api/books/search?${searchParams}&page=0&size=10`, {
                tags: { endpoint: 'book_search' }
            });

            check(res, {
                'search: status 200': (r) => r.status === 200
            });

            errorRate.add(res.status !== 200);
            requestCounter.add(1, { type: 'search' });
        });
    }

    // 4. Комментарии (10%)
    else if (scenario < 0.90) {
        group('Comments', () => {
            const bookId = randomInt(DATA_RANGES.MIN_BOOK_ID, DATA_RANGES.MAX_BOOK_ID);
            const res = http.get(`${BASE_URL}/api/comments/books/${bookId}?page=0&size=10`, {
                tags: { endpoint: 'comments' }
            });

            check(res, {
                'comments: status 200': (r) => r.status === 200
            });

            errorRate.add(res.status !== 200);
            requestCounter.add(1, { type: 'comments' });
        });
    }

    // 5. Авторизованные действия (10%, только если есть токен)
    else if (token) {
        group('Authorized Actions', () => {
            const authHeaders = {
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json'
                }
            };

            // Получить свой профиль
            const profileRes = http.get(`${BASE_URL}/api/users/me`, {
                ...authHeaders,
                tags: { endpoint: 'user_profile' }
            });

            check(profileRes, {
                'profile: status 200': (r) => r.status === 200
            });

            // Мои рейтинги
            const ratingsRes = http.get(`${BASE_URL}/api/ratings/me`, {
                ...authHeaders,
                tags: { endpoint: 'user_ratings' }
            });

            check(ratingsRes, {
                'ratings: status 200': (r) => r.status === 200
            });

            errorRate.add(profileRes.status !== 200 || ratingsRes.status !== 200);
            requestCounter.add(2, { type: 'authorized' });
        });
    }

    // Пауза (think time)
    sleep(randomInt(1, 3));
}

// Отчёт
export function handleSummary(data) {
    return {
        'stdout': textSummary(data),
        'load-test-results.json': JSON.stringify(data, null, 2)
    };
}

function textSummary(data) {
    const metrics = data.metrics;

    // Безопасное получение значения метрики
    const getMetricValue = (metric, field, defaultValue = 0) => {
        try {
            return metric && metric.values && metric.values[field] !== undefined
                ? metric.values[field]
                : defaultValue;
        } catch (e) {
            return defaultValue;
        }
    };

    let summary = '\n\n';
    summary += '═══════════════════════════════════════════════════════════\n';
    summary += '                    LOAD TEST RESULTS                      \n';
    summary += '═══════════════════════════════════════════════════════════\n\n';

    // HTTP Requests
    const httpReqsCount = getMetricValue(metrics.http_reqs, 'count');
    const httpReqsRate = getMetricValue(metrics.http_reqs, 'rate');
    const httpReqFailedRate = getMetricValue(metrics.http_req_failed, 'rate');

    summary += `  Total Requests:      ${httpReqsCount}\n`;
    summary += `  Requests/sec:        ${httpReqsRate.toFixed(2)}\n`;
    summary += `  Failed Requests:     ${(httpReqFailedRate * 100).toFixed(2)}%\n\n`;

    // Response Times
    const avgDuration = getMetricValue(metrics.http_req_duration, 'avg');
    const p95Duration = getMetricValue(metrics.http_req_duration, 'p(95)');
    const p99Duration = getMetricValue(metrics.http_req_duration, 'p(99)');
    const maxDuration = getMetricValue(metrics.http_req_duration, 'max');

    summary += `  Response Time (avg): ${avgDuration.toFixed(2)}ms\n`;
    summary += `  Response Time (p95): ${p95Duration.toFixed(2)}ms\n`;
    summary += `  Response Time (p99): ${p99Duration.toFixed(2)}ms\n`;
    summary += `  Response Time (max): ${maxDuration.toFixed(2)}ms\n\n`;

    // Checks
    const checksCount = getMetricValue(metrics.checks, 'count');
    const checksPasses = getMetricValue(metrics.checks, 'passes');
    const checksRate = checksCount > 0 ? (checksPasses / checksCount * 100) : 0;

    summary += `  Checks Passed:       ${checksRate.toFixed(2)}%\n`;

    // Errors
    const errorsRate = getMetricValue(metrics.errors, 'rate');
    summary += `  Error Rate:          ${(errorsRate * 100).toFixed(2)}%\n\n`;

    // Data Transfer
    const dataReceived = getMetricValue(metrics.data_received, 'count');
    const dataSent = getMetricValue(metrics.data_sent, 'count');

    summary += `  Data Received:       ${(dataReceived / 1024 / 1024).toFixed(2)} MB\n`;
    summary += `  Data Sent:           ${(dataSent / 1024).toFixed(2)} KB\n\n`;

    summary += '═══════════════════════════════════════════════════════════\n';

    // Thresholds
    if (data.thresholds) {
        const allPassed = Object.keys(data.thresholds).every(key =>
            data.thresholds[key] && data.thresholds[key].ok
        );

        if (allPassed) {
            summary += '  ✅ ALL THRESHOLDS PASSED!\n';
        } else {
            summary += '  ❌ SOME THRESHOLDS FAILED:\n';
            Object.keys(data.thresholds).forEach(key => {
                if (data.thresholds[key] && !data.thresholds[key].ok) {
                    summary += `     - ${key}\n`;
                }
            });
        }
    }

    summary += '═══════════════════════════════════════════════════════════\n\n';

    return summary;
}