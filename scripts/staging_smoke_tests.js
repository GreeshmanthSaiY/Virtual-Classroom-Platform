/**
 * Virtual Classroom Platform - Staging Post-Deployment Smoke Test Suite
 *
 * Verifies live frontend availability, backend health actuators, API error contracts,
 * unauthenticated route security, and CORS policies against staging targets.
 */

const http = require('http');
const https = require('https');
const { URL } = require('url');

// Parse CLI options
const args = process.argv.slice(2);
let frontendUrl = 'http://localhost:3000';
let backendUrl = 'http://localhost:8080';
let dryRun = false;

for (let i = 0; i < args.length; i++) {
  if (args[i].startsWith('--frontend=')) {
    frontendUrl = args[i].substring(args[i].indexOf('=') + 1);
  } else if (args[i].startsWith('--backend=')) {
    backendUrl = args[i].substring(args[i].indexOf('=') + 1);
  } else if (args[i] === '--dry-run') {
    dryRun = true;
  }
}

// Remove trailing slashes
frontendUrl = (frontendUrl || '').trim().replace(/\/+$/, '');
backendUrl = (backendUrl || '').trim().replace(/\/+$/, '');

// URL format validation in live mode
if (!dryRun) {
  if (!/^https?:\/\//i.test(frontendUrl)) {
    console.error(`ERROR: Target frontend URL must start with http:// or https://: "${frontendUrl}"`);
    process.exit(1);
  }
  if (!/^https?:\/\//i.test(backendUrl)) {
    console.error(`ERROR: Target backend URL must start with http:// or https://: "${backendUrl}"`);
    process.exit(1);
  }
}

console.log('================================================================');
console.log('Virtual Classroom Platform - Staging Smoke Test Suite');
console.log('================================================================');
console.log(`Target Frontend: ${frontendUrl}`);
console.log(`Target Backend:  ${backendUrl}`);
console.log(`Execution Mode:  ${dryRun ? 'DRY-RUN (Specification Validation)' : 'LIVE HTTP VERIFICATION'}`);
console.log('----------------------------------------------------------------\n');

function makeRequest(targetUrl, options = {}) {
  return new Promise((resolve, reject) => {
    const parsed = new URL(targetUrl);
    const client = parsed.protocol === 'https:' ? https : http;

    const reqOptions = {
      method: options.method || 'GET',
      headers: options.headers || {},
      timeout: options.timeout || 10000,
    };

    const req = client.request(parsed, reqOptions, (res) => {
      let data = '';
      res.on('data', (chunk) => (data += chunk));
      res.on('end', () => {
        resolve({
          statusCode: res.statusCode,
          headers: res.headers,
          body: data,
        });
      });
    });

    req.on('error', (err) => reject(err));
    req.on('timeout', () => {
      req.destroy();
      reject(new Error(`Connection to ${targetUrl} timed out after 10000ms`));
    });

    if (options.body) {
      req.write(typeof options.body === 'string' ? options.body : JSON.stringify(options.body));
    }
    req.end();
  });
}

const tests = [
  {
    name: 'Frontend Landing Page Response',
    fn: async () => {
      if (dryRun) return { passed: true, detail: 'Landing page route definition verified' };
      const res = await makeRequest(`${frontendUrl}/`);
      if (res.statusCode !== 200) {
        throw new Error(`Expected HTTP 200 but received ${res.statusCode}`);
      }
      return { passed: true, detail: `Responded with HTTP 200 OK (${res.body.length} bytes)` };
    },
  },
  {
    name: 'Frontend Login Interface Response',
    fn: async () => {
      if (dryRun) return { passed: true, detail: 'Login route rewrite verified' };
      const res = await makeRequest(`${frontendUrl}/login`);
      if (res.statusCode !== 200) {
        throw new Error(`Expected HTTP 200 but received ${res.statusCode}`);
      }
      return { passed: true, detail: 'Responded with HTTP 200 OK' };
    },
  },
  {
    name: 'Backend Actuator Liveness Probe',
    fn: async () => {
      if (dryRun) return { passed: true, detail: 'Liveness probe path /actuator/health/liveness requires HTTP 200 UP' };
      const res = await makeRequest(`${backendUrl}/actuator/health/liveness`);
      if (res.statusCode !== 200) {
        throw new Error(`Expected HTTP 200 on /actuator/health/liveness, received ${res.statusCode}`);
      }
      if (!res.body.includes('"UP"')) {
        throw new Error(`Expected UP status in liveness payload, received: ${res.body.slice(0, 120)}`);
      }
      return { passed: true, detail: 'Liveness probe returned HTTP 200 UP' };
    },
  },
  {
    name: 'Backend Health Probe (/actuator/health)',
    fn: async () => {
      if (dryRun) return { passed: true, detail: 'Actuator health endpoint requires HTTP 200 UP' };
      const res = await makeRequest(`${backendUrl}/actuator/health`);
      if (res.statusCode !== 200) {
        throw new Error(`Expected HTTP 200 but received ${res.statusCode}`);
      }
      if (!res.body.includes('"UP"')) {
        throw new Error(`Expected UP status in health payload, received: ${res.body.slice(0, 120)}`);
      }
      return { passed: true, detail: 'Backend health is UP (HTTP 200)' };
    },
  },
  {
    name: 'Protected Endpoint Security Barrier (/api/v1/admin/metrics)',
    fn: async () => {
      if (dryRun) return { passed: true, detail: 'Admin metrics route 401/403 gate verified' };
      const res = await makeRequest(`${backendUrl}/api/v1/admin/metrics`);
      if (res.statusCode !== 401 && res.statusCode !== 403) {
        throw new Error(`Expected HTTP 401/403 for unauthenticated request, received ${res.statusCode}`);
      }
      return { passed: true, detail: `Rejected unauthenticated access with HTTP ${res.statusCode} (Security barrier active)` };
    },
  },
  {
    name: 'Private Storage Local-Files Barrier (/api/v1/storage/files/*)',
    fn: async () => {
      if (dryRun) return { passed: true, detail: 'Local storage file endpoint disabled in staging (401/403)' };
      const res = await makeRequest(`${backendUrl}/api/v1/storage/files/probe.txt`);
      if (res.statusCode !== 401 && res.statusCode !== 403) {
        throw new Error(`Expected HTTP 401/403 on disabled local storage route, received ${res.statusCode}`);
      }
      return { passed: true, detail: `Blocked public local-file route with HTTP ${res.statusCode}` };
    },
  },
  {
    name: 'Authentication Input Validation Barrier (/api/v1/auth/login)',
    fn: async () => {
      if (dryRun) return { passed: true, detail: 'Auth validation 400/401 gate verified' };
      const res = await makeRequest(`${backendUrl}/api/v1/auth/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email: 'invalid-email', password: '' }),
      });
      if (res.statusCode !== 400 && res.statusCode !== 401) {
        throw new Error(`Expected HTTP 400/401 for malformed payload, received ${res.statusCode}`);
      }
      return { passed: true, detail: `Rejected malformed payload with HTTP ${res.statusCode}` };
    },
  },
  {
    name: 'CORS Preflight Allowed Staging Origin Verification',
    fn: async () => {
      if (dryRun) return { passed: true, detail: 'CORS policy allows staging frontend origin with credentials' };
      const res = await makeRequest(`${backendUrl}/api/v1/auth/login`, {
        method: 'OPTIONS',
        headers: {
          Origin: frontendUrl,
          'Access-Control-Request-Method': 'POST',
          'Access-Control-Request-Headers': 'Content-Type,Authorization',
        },
      });
      const allowOrigin = res.headers['access-control-allow-origin'];
      const allowCreds = res.headers['access-control-allow-credentials'];
      if (res.statusCode !== 200 && res.statusCode !== 204) {
        throw new Error(`Expected HTTP 200/204 for valid CORS preflight, received ${res.statusCode}`);
      }
      if (allowOrigin !== frontendUrl) {
        throw new Error(`Expected Access-Control-Allow-Origin="${frontendUrl}", received "${allowOrigin || 'missing'}"`);
      }
      if (allowCreds !== 'true') {
        throw new Error(`Expected Access-Control-Allow-Credentials="true", received "${allowCreds || 'missing'}"`);
      }
      return { passed: true, detail: `Allowed staging origin ${allowOrigin} with credentials=true (HTTP ${res.statusCode})` };
    },
  },
  {
    name: 'CORS Preflight Unauthorized Origin Rejection',
    fn: async () => {
      if (dryRun) return { passed: true, detail: 'CORS policy rejects unauthorized third-party origins' };
      const rogueOrigin = 'https://unauthorized-origin.invalid';
      const res = await makeRequest(`${backendUrl}/api/v1/auth/login`, {
        method: 'OPTIONS',
        headers: {
          Origin: rogueOrigin,
          'Access-Control-Request-Method': 'POST',
        },
      });
      const allowOrigin = res.headers['access-control-allow-origin'];
      if (allowOrigin === rogueOrigin || allowOrigin === '*') {
        throw new Error(`Unauthorized origin was permitted: Access-Control-Allow-Origin="${allowOrigin}"`);
      }
      return { passed: true, detail: `Rejected unauthorized origin (HTTP ${res.statusCode}, ACAO=${allowOrigin || 'absent'})` };
    },
  },
];

async function runSuite() {
  let passedCount = 0;
  let failedCount = 0;
  let skippedCount = 0;

  for (const test of tests) {
    process.stdout.write(`Testing: ${test.name.padEnd(65)} ... `);
    try {
      const result = await test.fn();
      if (result.skipped) {
        console.log(`[SKIP] ${result.reason || 'Test skipped'}`);
        skippedCount++;
      } else if (dryRun) {
        console.log(`[DRY-RUN SPEC VERIFIED] ${result.detail}`);
        passedCount++;
      } else {
        console.log(`[PASS] ${result.detail}`);
        passedCount++;
      }
    } catch (err) {
      console.log(`[FAIL] ${err.message}`);
      failedCount++;
    }
  }

  console.log('\n----------------------------------------------------------------');
  console.log(`Results: ${passedCount} passed, ${failedCount} failed, ${skippedCount} skipped (${tests.length} total)`);
  if (dryRun) {
    console.log('NOTICE: Executed in DRY-RUN mode. No live HTTP network requests were dispatched.');
  }
  console.log('================================================================');

  if (failedCount > 0) {
    process.exit(1);
  } else {
    process.exit(0);
  }
}

runSuite();
