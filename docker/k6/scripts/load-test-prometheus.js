import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';
import { BASE_URL, DATA_RANGES } from '../utils/config.js';

const errorRate = new Rate('errors');
const requestDuration = new Trend('request_duration');

export const options = {
    stages: [
        { duration: '1m', target: 10 },
        { duration: '3m', target: 30 },
        { duration: '1m', target: 0 }
    ],

    thresholds: {
        'http_req_duration': ['p(95)<500'],
        'errors': ['rate<0.05']
    }
};

function randomInt(min, max) {
    return Math.floor(Math.random() * (max - min + 1)) + min;
}

export default function () {
    const bookId = randomInt(DATA_RANGES.MIN_BOOK_ID, DATA_RANGES.MAX_BOOK_ID);

    const start = Date.now();
    const res = http.get(`${BASE_URL}/api/books/${bookId}`);
    requestDuration.add(Date.now() - start);

    check(res, {
        'status 200': (r) => r.status === 200
    });

    errorRate.add(res.status !== 200);

    sleep(1);
}