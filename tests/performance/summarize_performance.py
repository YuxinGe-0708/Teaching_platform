#!/usr/bin/env python3
"""Summarize raw benchmark and resource files without inventing missing values."""

from __future__ import annotations

import argparse
import csv
import json
import statistics
from collections import defaultdict
from pathlib import Path
from typing import Dict, Iterable, List


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--results-root", required=True)
    return parser.parse_args()


def number(value: object) -> float:
    try:
        return float(value)
    except (TypeError, ValueError):
        return 0.0


def resource_metrics(path: Path) -> Dict[str, float]:
    rows: List[dict] = []
    if path.exists():
        with path.open(encoding="utf-8", newline="") as handle:
            rows = list(csv.DictReader(handle))
    by_timestamp: Dict[str, List[dict]] = defaultdict(list)
    for row in rows:
        by_timestamp[row.get("timestamp_utc", "")].append(row)

    host_cpu = [number(row.get("host_cpu_pct")) for row in rows]
    host_memory = [number(row.get("host_memory_pct")) for row in rows]
    container_cpu: List[float] = []
    container_memory: List[float] = []
    for timestamp_rows in by_timestamp.values():
        container_cpu.append(
            sum(number(row.get("container_cpu_pct")) for row in timestamp_rows)
        )
        container_memory.append(
            sum(number(row.get("container_memory_mib")) for row in timestamp_rows)
        )

    return {
        "host_cpu_avg_pct": round(statistics.fmean(host_cpu), 3) if host_cpu else 0.0,
        "host_cpu_max_pct": round(max(host_cpu), 3) if host_cpu else 0.0,
        "host_memory_avg_pct": round(statistics.fmean(host_memory), 3)
        if host_memory
        else 0.0,
        "host_memory_max_pct": round(max(host_memory), 3) if host_memory else 0.0,
        "container_cpu_avg_pct": round(statistics.fmean(container_cpu), 3)
        if container_cpu
        else 0.0,
        "container_cpu_max_pct": round(max(container_cpu), 3)
        if container_cpu
        else 0.0,
        "container_memory_avg_mib": round(statistics.fmean(container_memory), 3)
        if container_memory
        else 0.0,
        "container_memory_max_mib": round(max(container_memory), 3)
        if container_memory
        else 0.0,
        "resource_samples": len(rows),
    }


def raw_files(root: Path) -> Iterable[Path]:
    return sorted(root.glob("*/*/run-*/benchmark.json"))


def main() -> int:
    args = parse_args()
    root = Path(args.results_root)
    detailed: List[dict] = []
    for path in raw_files(root):
        raw = json.loads(path.read_text(encoding="utf-8"))
        resources = resource_metrics(path.with_name("resources.csv"))
        metrics = raw.get("metrics", {})
        detailed.append(
            {
                "target": raw.get("target", ""),
                "scenario": raw.get("scenario", ""),
                "run": raw.get("run_number", ""),
                "base_url": raw.get("base_url", ""),
                "concurrency": raw.get("concurrency", ""),
                "duration_seconds": raw.get("duration_seconds", ""),
                "requests": metrics.get("requests", 0),
                "throughput_rps": metrics.get("throughput_rps", 0),
                "average_response_ms": metrics.get("average_response_ms", 0),
                "p95_response_ms": metrics.get("p95_response_ms", 0),
                "error_rate_pct": metrics.get("error_rate_pct", 0),
                **resources,
                "raw_file": str(path),
                "resource_file": str(path.with_name("resources.csv")),
            }
        )

    root.mkdir(parents=True, exist_ok=True)
    fields = list(detailed[0].keys()) if detailed else [
        "target", "scenario", "run", "base_url", "concurrency",
        "duration_seconds", "requests", "throughput_rps",
        "average_response_ms", "p95_response_ms", "error_rate_pct",
        "host_cpu_avg_pct", "host_cpu_max_pct", "host_memory_avg_pct",
        "host_memory_max_pct", "container_cpu_avg_pct",
        "container_cpu_max_pct", "container_memory_avg_mib",
        "container_memory_max_mib", "resource_samples", "raw_file",
        "resource_file",
    ]
    with (root / "detailed-summary.csv").open("w", newline="", encoding="utf-8") as handle:
        writer = csv.DictWriter(handle, fieldnames=fields)
        writer.writeheader()
        writer.writerows(detailed)

    grouped: Dict[tuple, List[dict]] = defaultdict(list)
    for row in detailed:
        grouped[(row["target"], row["scenario"])].append(row)
    comparison: List[dict] = []
    for (target, scenario), rows in sorted(grouped.items()):
        def mean(field: str) -> float:
            return round(statistics.fmean(number(row[field]) for row in rows), 3)

        comparison.append(
            {
                "target": target,
                "scenario": scenario,
                "runs": len(rows),
                "concurrency": rows[0]["concurrency"],
                "throughput_rps_avg": mean("throughput_rps"),
                "average_response_ms_avg": mean("average_response_ms"),
                "p95_response_ms_avg": mean("p95_response_ms"),
                "error_rate_pct_avg": mean("error_rate_pct"),
                "host_cpu_max_pct_peak": max(number(row["host_cpu_max_pct"]) for row in rows),
                "host_memory_max_pct_peak": max(number(row["host_memory_max_pct"]) for row in rows),
                "container_cpu_max_pct_peak": max(number(row["container_cpu_max_pct"]) for row in rows),
                "container_memory_max_mib_peak": max(
                    number(row["container_memory_max_mib"]) for row in rows
                ),
            }
        )
    comparison_fields = list(comparison[0].keys()) if comparison else [
        "target", "scenario", "runs", "concurrency", "throughput_rps_avg",
        "average_response_ms_avg", "p95_response_ms_avg", "error_rate_pct_avg",
        "host_cpu_max_pct_peak", "host_memory_max_pct_peak",
        "container_cpu_max_pct_peak", "container_memory_max_mib_peak",
    ]
    with (root / "comparison.csv").open("w", newline="", encoding="utf-8") as handle:
        writer = csv.DictWriter(handle, fieldnames=comparison_fields)
        writer.writeheader()
        writer.writerows(comparison)
    print(json.dumps({"runs": len(detailed), "root": str(root)}, ensure_ascii=False))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
