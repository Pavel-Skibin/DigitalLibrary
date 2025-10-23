import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';
import { BASE_URL, DATA_RANGES, TEST_USERS } from '../utils/config.js';

// Метрики
const errorRate = new Rate('errors');
const requestCounter = new Counter('requests_by_endpoint');
const slowRequests = new Counter('slow_requests'); // > 1s

// Конфигурация Stress Test
export const options = {
    stages: [
        { duration: '2m', target: 50 },    // Разогрев
        { duration: '3m', target: 100 },   // Умеренная нагрузка
        { duration: '3m', target: 200 },   // Высокая нагрузка
        { duration: '3m', target: 300 },   // Очень высокая
        { duration: '2m', target: 400 },   // Экстремальная
        { duration: '2m', target: 500 },   // Максимум
        { duration: '3m', target: 0 }      // Восстановление
    ],

    thresholds: {
        'http_req_duration': ['p(95)<2000', 'p(99)<5000'],  // Более мягкие лимиты
        'http_req_failed': ['rate<0.10'],  // До 10% ошибок допустимо
        'errors': ['rate<0.15'],
        'checks': ['rate>0.80']  // 80% проверок успешны
    },

    tags: {
        test_type: 'stress',
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

// Логин (с retry)
function login(user, retries = 2) {
    for (let i = 0; i < retries; i++) {
        const payload = JSON.stringify({
            username: user.username,
            password: user.password
        });

        const params = {
            headers: { 'Content-Type': 'application/json' },
            tags: { endpoint: 'login' },
            timeout: '10s'
        };

        const res = http.post(`${BASE_URL}/api/auth/login`, payload, params);

        if (res.status === 200) {
            try {
                return JSON.parse(res.body).token;
            } catch (e) {
                // Ignore
            }
        }

        if (i < retries - 1) {
            sleep(0.5);
        }
    }

    return null;
}

export default function () {
    // 80% гости, 20% авторизованные (упрощаем под высокой нагрузкой)
    const isGuest = Math.random() < 0.80;
    let token = null;

    if (!isGuest) {
        const user = randomElement(TEST_USERS.users);
        token = login(user);

        if (!token) {
            sleep(0.5);
            return;
        }
    }

    // Упрощенные сценарии (меньше разнообразия = стабильнее)
    const scenario = Math.random();

    // 1. Список книг (50%)
    if (scenario < 0.50) {
        const page = randomInt(0, 5); // Меньше страниц
        const start = Date.now();

        const res = http.get(`${BASE_URL}/api/books?page=${page}&size=10`, {
            tags: { endpoint: 'books_list' },
            timeout: '10s'
        });

        const duration = Date.now() - start;
        if (duration > 1000) slowRequests.add(1, { endpoint: 'books_list' });

        check(res, {
            'books list: status 2xx': (r) => r.status >= 200 && r.status < 300
        });

        errorRate.add(res.status !== 200);
        requestCounter.add(1, { endpoint: 'books_list' });
    }

    // 2. Детали книги (30%)
    else if (scenario < 0.80) {
        const bookId = randomInt(DATA_RANGES.MIN_BOOK_ID, DATA_RANGES.MAX_BOOK_ID);
        const start = Date.now();

        const res = http.get(`${BASE_URL}/api/books/${bookId}`, {
            tags: { endpoint: 'book_details' },
            timeout: '10s'
        });

        const duration = Date.now() - start;
        if (duration > 1000) slowRequests.add(1, { endpoint: 'book_details' });

        check(res, {
            'book details: status 2xx': (r) => r.status >= 200 && r.status < 300
        });

        errorRate.add(res.status !== 200);
        requestCounter.add(1, { endpoint: 'book_details' });
    }

    // 3. Жанры (10%)
    else if (scenario < 0.90) {
        const start = Date.now();

        const res = http.get(`${BASE_URL}/api/genres`, {
            tags: { endpoint: 'genres' },
            timeout: '10s'
        });

        const duration = Date.now() - start;
        if (duration > 1000) slowRequests.add(1, { endpoint: 'genres' });

        check(res, {
            'genres: status 2xx': (r) => r.status >= 200 && r.status < 300
        });

        errorRate.add(res.status !== 200);
        requestCounter.add(1, { endpoint: 'genres' });
    }

    // 4. Авторы (10%)
    else {
        const start = Date.now();

        const res = http.get(`${BASE_URL}/api/authors?page=0&size=20`, {
            tags: { endpoint: 'authors' },
            timeout: '10s'
        });

        const duration = Date.now() - start;
        if (duration > 1000) slowRequests.add(1, { endpoint: 'authors' });

        check(res, {
            'authors: status 2xx': (r) => r.status >= 200 && r.status < 300
        });

        errorRate.add(res.status !== 200);
        requestCounter.add(1, { endpoint: 'authors' });
    }

    // Короткая пауза (меньше под высокой нагрузкой)
    sleep(Math.random() * 0.5);
}

// Отчёт
export function handleSummary(data) {
    return {
        'stdout': textSummary(data),
        'stress-test-results.json': JSON.stringify(data, null, 2)
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