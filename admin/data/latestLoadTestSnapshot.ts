export interface IsolatedLoadTestStageSnapshot {
  concurrency: number;
  studentsSimulated: number;
  requestsSent: number;
  errorsCount: number;
  cache304Hits: number;
  cache304RatePct: number;
  sqlQueriesExecuted: number;
  sqlAvgPerRequest: number;
  durationMs: number;
  throughputReqPerSec: number;
  p50LatencyMs: number;
  p95LatencyMs: number;
  p99LatencyMs: number;
}

export interface IsolatedLoadTestSnapshot {
  measured: boolean;
  environmentLabel: string;
  disclaimer: string;
  recordedAtIso: string | null;
  stages: IsolatedLoadTestStageSnapshot[];
}

export const LATEST_LOAD_TEST_SNAPSHOT: IsolatedLoadTestSnapshot = {
  "measured": true,
  "environmentLabel": "Isolated Local SQLite + Worker Router (In-Process Benchmark)",
  "disclaimer": "Measured in an isolated local test environment (not live Cloudflare edge network or production D1). Use these benchmarks strictly to verify router latency overhead, ETag 304 hit efficiency, SQL queries/request, and error-free concurrency.",
  "recordedAtIso": "2026-11-16T05:19:56.008Z",
  "stages": [
    {
      "concurrency": 10,
      "studentsSimulated": 10,
      "requestsSent": 62,
      "errorsCount": 0,
      "cache304Hits": 20,
      "cache304RatePct": 32.3,
      "sqlQueriesExecuted": 371,
      "sqlAvgPerRequest": 5.98,
      "durationMs": 84.41,
      "throughputReqPerSec": 734.5,
      "p50LatencyMs": 14.12,
      "p95LatencyMs": 23.47,
      "p99LatencyMs": 84.6
    },
    {
      "concurrency": 25,
      "studentsSimulated": 25,
      "requestsSent": 152,
      "errorsCount": 0,
      "cache304Hits": 50,
      "cache304RatePct": 32.9,
      "sqlQueriesExecuted": 922,
      "sqlAvgPerRequest": 6.07,
      "durationMs": 139.22,
      "throughputReqPerSec": 1091.8,
      "p50LatencyMs": 17.2,
      "p95LatencyMs": 40.32,
      "p99LatencyMs": 42.66
    },
    {
      "concurrency": 50,
      "studentsSimulated": 50,
      "requestsSent": 302,
      "errorsCount": 0,
      "cache304Hits": 100,
      "cache304RatePct": 33.1,
      "sqlQueriesExecuted": 1837,
      "sqlAvgPerRequest": 6.08,
      "durationMs": 219.13,
      "throughputReqPerSec": 1378.2,
      "p50LatencyMs": 30.46,
      "p95LatencyMs": 64.4,
      "p99LatencyMs": 68.84
    },
    {
      "concurrency": 100,
      "studentsSimulated": 100,
      "requestsSent": 602,
      "errorsCount": 0,
      "cache304Hits": 200,
      "cache304RatePct": 33.2,
      "sqlQueriesExecuted": 3669,
      "sqlAvgPerRequest": 6.09,
      "durationMs": 446.84,
      "throughputReqPerSec": 1347.2,
      "p50LatencyMs": 57.04,
      "p95LatencyMs": 136.04,
      "p99LatencyMs": 144.29
    }
  ]
};
