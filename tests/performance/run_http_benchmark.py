#!/usr/bin/env python3
"""Run the same HTTP benchmark against the monolith and microservice entrypoints."""

from __future__ import annotations

import argparse
import csv
import json
import os
import platform
import statistics
import sys
import threading
import time
from concurrent.futures import ThreadPoolExecutor
from dataclasses import dataclass
from datetime import datetime, timezone
from pathlib import Path
from typing import Dict, List, Optional, Tuple

import requests


SCENARIOS = ("login", "course_detail", "judge_submit")


@dataclass
class RequestResult:
    started_at: str
    elapsed_ms: float
    status_code: int
    success: bool
    error: str = ""


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--base-url", required=True)
    parser.add_argument("--target", choices=("monolith", "microservices"), required=True)
    parser.add_argument("--scenario", choices=SCENARIOS, required=True)
    parser.add_argument("--run-number", type=int, required=True)
    parser.add_argument("--concurrency", type=int, default=10)
    parser.add_argument("--duration-seconds", type=int, default=60)
    parser.add_argument("--warmup-seconds", type=int, default=15)
    parser.add_argument("--username", default=os.getenv("PERF_USERNAME", "ms_student"))
    parser.add_argument("--password", default=os.getenv("PERF_PASSWORD", "123456"))
    parser.add_argument("--course-id", default=os.getenv("PERF_COURSE_ID", "1001"))
    parser.add_argument("--judge-task-id", default=os.getenv("PERF_JUDGE_TASK_ID", ""))
    parser.add_argument("--output", required=True)
    parser.add_argument("--resource-file", default="")
    return parser.parse_args()


def utc_now() -> datetime:
    return datetime.now(timezone.utc)


def iso_now() -> str:
    return utc_now().isoformat()


def percentile(values: List[float], percentile_value: float) -> float:
    if not values:
        return 0.0
    ordered = sorted(values)
    rank = (len(ordered) - 1) * percentile_value / 100.0
    lower = int(rank)
    upper = min(lower + 1, len(ordered) - 1)
    fraction = rank - lower
    return ordered[lower] + (ordered[upper] - ordered[lower]) * fraction


def response_is_success(scenario: str, response: requests.Response) -> Tuple[bool, str]:
    if response.status_code < 200 or response.status_code >= 400:
        return False, f"http_{response.status_code}"
    if scenario == "login":
        return response.status_code in (200, 302, 303), ""
    if scenario == "course_detail":
        body = response.text
        if response.status_code != 200:
            return False, f"http_{response.status_code}"
        if "student/course_detail" in body or "课程" in body or "course_detail" in body:
            return True, ""
        if "登录" in body or "login" in body.lower():
            return False, "redirected_to_login"
        return True, ""
    try:
        body = response.json()
    except ValueError:
        return False, "invalid_json"
    if isinstance(body, dict) and body.get("code") not in (None, 200):
        return False, f"business_code_{body.get('code')}"
    if scenario == "judge_submit":
        data = body.get("data") if isinstance(body, dict) else None
        if isinstance(data, dict) and data.get("status") in ("IE", "CE", "RE", "TLE"):
            return False, f"judge_{data.get('status')}"
    return True, ""


def login_session(session: requests.Session, args: argparse.Namespace) -> None:
    response = session.post(
        f"{args.base_url.rstrip('/')}/login",
        data={"username": args.username, "password": args.password},
        timeout=10,
        allow_redirects=False,
    )
    if response.status_code not in (200, 302, 303):
        raise RuntimeError(f"setup login failed: HTTP {response.status_code}")


def discover_course_id(session: requests.Session, args: argparse.Namespace) -> str:
    if args.course_id != "auto":
        return args.course_id
    response = session.get(
        f"{args.base_url.rstrip('/')}/student/course/my",
        timeout=10,
    )
    if response.status_code != 200:
        raise RuntimeError(f"course discovery failed: HTTP {response.status_code}")
    import re

    matches = re.findall(r"/student/course/detail/(\d+)", response.text)
    if not matches:
        raise RuntimeError("course discovery found no enrolled course")
    return matches[0]


def request_once(
    args: argparse.Namespace,
    session: requests.Session,
    course_id: str,
) -> RequestResult:
    started_at = iso_now()
    start = time.perf_counter()
    try:
        if args.scenario == "login":
            response = session.post(
                f"{args.base_url.rstrip('/')}/login",
                data={"username": args.username, "password": args.password},
                timeout=10,
                allow_redirects=False,
            )
        elif args.scenario == "course_detail":
            response = session.get(
                f"{args.base_url.rstrip('/')}/student/course/detail/{course_id}",
                timeout=10,
            )
        else:
            payload = {
                "language": "python",
                "code": "print('Hello World')",
            }
            if args.judge_task_id:
                payload["taskId"] = int(args.judge_task_id)
            response = session.post(
                f"{args.base_url.rstrip('/')}/api/v2/judge/submit",
                json=payload,
                timeout=20,
            )
        elapsed_ms = (time.perf_counter() - start) * 1000
        success, error = response_is_success(args.scenario, response)
        return RequestResult(
            started_at=started_at,
            elapsed_ms=elapsed_ms,
            status_code=response.status_code,
            success=success,
            error=error,
        )
    except requests.RequestException as exc:
        return RequestResult(
            started_at=started_at,
            elapsed_ms=(time.perf_counter() - start) * 1000,
            status_code=0,
            success=False,
            error=type(exc).__name__,
        )


def write_resource_file(path: str, resources: List[dict]) -> None:
    if not path:
        return
    resource_path = Path(path)
    resource_path.parent.mkdir(parents=True, exist_ok=True)
    with resource_path.open("w", newline="", encoding="utf-8") as handle:
        writer = csv.DictWriter(handle, fieldnames=sorted(resources[0]) if resources else ["timestamp"])
        writer.writeheader()
        writer.writerows(resources)


def main() -> int:
    args = parse_args()
    if args.concurrency < 1 or args.duration_seconds < 1:
        raise SystemExit("concurrency and duration-seconds must be positive")

    output_path = Path(args.output)
    output_path.parent.mkdir(parents=True, exist_ok=True)
    resource_path = args.resource_file or str(output_path.with_name("resources.csv"))

    setup_sessions = []
    discovered_course_id = args.course_id
    for _ in range(args.concurrency):
        session = requests.Session()
        if args.scenario != "login":
            login_session(session, args)
            if args.scenario == "course_detail" and discovered_course_id == "auto":
                discovered_course_id = discover_course_id(session, args)
        setup_sessions.append(session)

    if args.warmup_seconds:
        warmup_end = time.monotonic() + args.warmup_seconds
        while time.monotonic() < warmup_end:
            session = setup_sessions[int(time.monotonic() * 1000) % len(setup_sessions)]
            try:
                session.get(f"{args.base_url.rstrip('/')}/actuator/health", timeout=3)
            except requests.RequestException:
                pass

    benchmark_started = time.monotonic()
    deadline = benchmark_started + args.duration_seconds
    results: List[RequestResult] = []
    lock = threading.Lock()

    def worker(worker_index: int) -> None:
        session = setup_sessions[worker_index]
        while time.monotonic() < deadline:
            result = request_once(args, session, discovered_course_id)
            with lock:
                results.append(result)

    with ThreadPoolExecutor(max_workers=args.concurrency) as executor:
        futures = [executor.submit(worker, index) for index in range(args.concurrency)]
        for future in futures:
            future.result()

    benchmark_finished = time.monotonic()
    elapsed_window = max(benchmark_finished - benchmark_started, 0.001)
    latencies = [item.elapsed_ms for item in results]
    successful = [item for item in results if item.success]
    error_counts: Dict[str, int] = {}
    status_counts: Dict[str, int] = {}
    for item in results:
        error_counts[item.error or "none"] = error_counts.get(item.error or "none", 0) + 1
        key = str(item.status_code)
        status_counts[key] = status_counts.get(key, 0) + 1

    summary = {
        "target": args.target,
        "scenario": args.scenario,
        "run_number": args.run_number,
        "base_url": args.base_url,
        "concurrency": args.concurrency,
        "warmup_seconds": args.warmup_seconds,
        "duration_seconds": args.duration_seconds,
        "data": {
            "username": args.username,
            "course_id": discovered_course_id,
            "judge_task_id": args.judge_task_id,
            "judge_payload": {
                "language": "python",
                "code": "print('Hello World')",
                **({"taskId": int(args.judge_task_id)} if args.judge_task_id else {}),
            },
        },
        "host": {
            "platform": platform.platform(),
            "python": sys.version.split()[0],
            "cpu_count": os.cpu_count(),
        },
        "started_at_utc": min(item.started_at for item in results) if results else iso_now(),
        "finished_at_utc": iso_now(),
        "actual_duration_seconds": round(elapsed_window, 3),
        "metrics": {
            "requests": len(results),
            "successful_requests": len(successful),
            "errors": len(results) - len(successful),
            "error_rate_pct": round((len(results) - len(successful)) * 100 / len(results), 4)
            if results
            else 0.0,
            "throughput_rps": round(len(results) / elapsed_window, 4),
            "average_response_ms": round(statistics.fmean(latencies), 3) if latencies else 0.0,
            "p95_response_ms": round(percentile(latencies, 95), 3),
            "p99_response_ms": round(percentile(latencies, 99), 3),
            "min_response_ms": round(min(latencies), 3) if latencies else 0.0,
            "max_response_ms": round(max(latencies), 3) if latencies else 0.0,
        },
        "status_counts": status_counts,
        "error_counts": error_counts,
        "requests_raw": [item.__dict__ for item in results],
    }
    output_path.write_text(json.dumps(summary, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps(summary["metrics"], ensure_ascii=False))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
