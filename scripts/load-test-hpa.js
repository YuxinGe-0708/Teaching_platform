import http from 'k6/http';
import { check } from 'k6';

const baseUrl = __ENV.BASE_URL || 'http://user-service:8082';
const baseVus = Number(__ENV.BASE_VUS || 2);
const peakVus = Number(__ENV.PEAK_VUS || 120);

export const options = {
  // A Kubernetes Service selects a backend for each new connection. Closing
  // connections between iterations lets Pods created by HPA receive traffic.
  noConnectionReuse: true,
  stages: [
    { duration: __ENV.BASELINE_DURATION || '30s', target: baseVus },
    { duration: __ENV.RAMP_DURATION || '30s', target: peakVus },
    { duration: __ENV.PEAK_DURATION || '150s', target: peakVus },
    { duration: __ENV.RAMP_DOWN_DURATION || '15s', target: 0 },
  ],
  thresholds: {
    http_req_failed: ['rate<0.05'],
    http_req_duration: ['p(95)<2000'],
  },
  summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
};

export default function () {
  const response = http.get(`${baseUrl}/actuator/health`, {
    tags: { endpoint: 'user-service-health' },
    timeout: '5s',
  });

  check(response, {
    'health endpoint returns 200': (r) => r.status === 200,
    'health response is UP': (r) => r.body && r.body.includes('"status":"UP"'),
  });
}
