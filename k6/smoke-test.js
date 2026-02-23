import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Rate } from 'k6/metrics';

const orderCreationTime = new Trend('order_creation_time');
const errorRate = new Rate('error_rate');

export const options = {
  vus: 10,
  duration: '30s',
  thresholds: {
    // Average response time for POST /api/orders must be under 200ms
    order_creation_time: ['avg<200'],
    // Error rate must be below 5%
    error_rate: ['rate<0.05'],
    // 95th percentile response time under 500ms
    http_req_duration: ['p(95)<500'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

function registerAndLogin() {
  const timestamp = Date.now();
  const email = `perf-test-${timestamp}@example.com`;
  const password = 'PerfTest123!';

  // Register a test user
  const registerRes = http.post(
    `${BASE_URL}/api/auth/register`,
    JSON.stringify({
      email: email,
      password: password,
      tenantName: `perf-tenant-${timestamp}`,
    }),
    { headers: { 'Content-Type': 'application/json' } }
  );

  if (registerRes.status !== 200 && registerRes.status !== 201) {
    console.error(`Registration failed: ${registerRes.status} ${registerRes.body}`);
    return null;
  }

  const loginRes = http.post(
    `${BASE_URL}/api/auth/login`,
    JSON.stringify({ email: email, password: password }),
    { headers: { 'Content-Type': 'application/json' } }
  );

  if (loginRes.status !== 200) {
    console.error(`Login failed: ${loginRes.status}`);
    return null;
  }

  return loginRes.json('token');
}

export function setup() {
  const token = registerAndLogin();
  if (!token) {
    throw new Error('Setup failed: could not obtain auth token');
  }
  return { token };
}

export default function (data) {
  const headers = {
    'Content-Type': 'application/json',
    Authorization: `Bearer ${data.token}`,
  };

  const orderPayload = JSON.stringify({
    description: `Load Test Order ${Date.now()}`,
    destinationAddress: '123 Performance Test Ave, Test City, TC 00000',
    weight: Math.random() * 100 + 1,
  });

  const startTime = Date.now();
  const res = http.post(`${BASE_URL}/api/orders`, orderPayload, { headers });
  const duration = Date.now() - startTime;

  orderCreationTime.add(duration);

  const success = check(res, {
    'status is 200 or 201': (r) => r.status === 200 || r.status === 201,
    'response has id': (r) => {
      try {
        return r.json('id') !== undefined;
      } catch {
        return false;
      }
    },
  });

  errorRate.add(!success);

  sleep(0.5);
}
