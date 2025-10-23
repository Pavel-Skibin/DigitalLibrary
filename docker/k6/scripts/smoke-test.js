import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';
import { BASE_URL, DATA_RANGES, THRESHOLDS } from '../utils/config.js';

// Кастомные метрики
const errorRate = new Rate('errors');
const bookDetailsDuration = new Trend('book_details_duration');
const requestCounter = new Counter('custom_requests_total');

// Конфигурация теста
export const options = {
    vus: 5,
    duration: '2m',

    // ОБНОВЛЁННЫЕ пороги (без http_req_failed)
    thresholds: {
        'http_req_duration': ['p(95)<500'],
        'errors': ['rate<0.05'],  // Наша кастомная метрика: <5% ошибок
        'checks': ['rate>0.95']   // 95% проверок должны пройти
    },

    tags: {
        test_type: 'smoke',
        environment: 'docker'
    }
};

// Утилиты
function randomInt(min, max) {
    return Math.floor(Math.random() * (max - min + 1)) + min;
}

export default function () {
    const scenario = Math.random();

    // 1. Список книг (30%)
    if (scenario < 0.30) {
        const page = randomInt(0, 10);
        const res = http.get(`${BASE_URL}/api/books?page=${page}&size=10`, {
            tags: { endpoint: 'books_list' }
        });

        const success = check(res, {
            'books list: status 200': (r) => r.status === 200,
            'books list: has content': (r) => {
                if (r.status !== 200) return false;
                try {
                    const body = JSON.parse(r.body);
                    return body.content && Array.isArray(body.content);
                } catch (e) {
                    return false;
                }
            }
        });

        errorRate.add(res.status !== 200);
        requestCounter.add(1, { endpoint: 'books_list' });
    }

    // 2. Детали книги (25%)
    else if (scenario < 0.55) {
        const bookId = randomInt(DATA_RANGES.MIN_BOOK_ID, DATA_RANGES.MAX_BOOK_ID);
        const start = Date.now();

        const res = http.get(`${BASE_URL}/api/books/${bookId}`, {
            tags: { endpoint: 'book_details' }
        });

        bookDetailsDuration.add(Date.now() - start);

        const success = check(res, {
            'book details: status 200': (r) => r.status === 200,
            'book details: has title': (r) => {
                if (r.status !== 200) return false;
                try {
                    const body = JSON.parse(r.body);
                    return body.title && body.title.length > 0;
                } catch (e) {
                    return false;
                }
            }
        });

        errorRate.add(res.status !== 200);
        requestCounter.add(1, { endpoint: 'book_details' });
    }

    // 3. Обложки книг (20%) — 404 НЕ считаем ошибкой!
    else if (scenario < 0.75) {
        const bookId = randomInt(DATA_RANGES.MIN_BOOK_ID, DATA_RANGES.MAX_BOOK_ID);
        const res = http.get(`${BASE_URL}/api/books/${bookId}/cover`, {
            tags: { endpoint: 'book_cover' }
        });

        check(res, {
            'cover: valid response': (r) => r.status === 200 || r.status === 404
        });

        // Только 5xx считаем реальными ошибками
        errorRate.add(res.status >= 500);
        requestCounter.add(1, { endpoint: 'book_cover' });
    }

    // 4. Комментарии (15%)
    else if (scenario < 0.90) {
        const bookId = randomInt(DATA_RANGES.MIN_BOOK_ID, DATA_RANGES.MAX_BOOK_ID);
        const res = http.get(`${BASE_URL}/api/comments/books/${bookId}?page=0&size=10`, {
            tags: { endpoint: 'comments' }
        });

        const success = check(res, {
            'comments: status 200': (r) => r.status === 200,
            'comments: valid structure': (r) => {
                if (r.status !== 200) return false;
                try {
                    const body = JSON.parse(r.body);
                    return body.content !== undefined;
                } catch (e) {
                    return false;
                }
            }
        });

        errorRate.add(res.status !== 200);
        requestCounter.add(1, { endpoint: 'comments' });
    }

    // 5. Жанры (5%)
    else if (scenario < 0.95) {
        const res = http.get(`${BASE_URL}/api/genres`, {
            tags: { endpoint: 'genres' }
        });

        const success = check(res, {
            'genres: status 200': (r) => r.status === 200,
            'genres: not empty': (r) => {
                if (r.status !== 200) return false;
                try {
                    const body = JSON.parse(r.body);
                    return Array.isArray(body) && body.length > 0;
                } catch (e) {
                    return false;
                }
            }
        });

        errorRate.add(res.status !== 200);
        requestCounter.add(1, { endpoint: 'genres' });
    }

    // 6. Авторы (5%)
    else {
        const page = randomInt(0, 5);
        const res = http.get(`${BASE_URL}/api/authors?page=${page}&size=20`, {
            tags: { endpoint: 'authors' }
        });

        const success = check(res, {
            'authors: status 200': (r) => r.status === 200,
            'authors: valid structure': (r) => {
                if (r.status !== 200) return false;
                try {
                    const body = JSON.parse(r.body);
                    return body.content !== undefined;
                } catch (e) {
                    return false;
                }
            }
        });

        errorRate.add(res.status !== 200);
        requestCounter.add(1, { endpoint: 'authors' });
    }

    sleep(randomInt(1, 3));
}