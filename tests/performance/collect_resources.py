#!/usr/bin/env python3
"""Sample host and Docker resource usage while a benchmark is running."""

from __future__ import annotations

import argparse
import csv
import json
import re
import subprocess
import time
from datetime import datetime, timezone
from pathlib import Path
from typing import Dict, Iterable, List

import psutil


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", required=True)
    parser.add_argument("--duration-seconds", type=int, required=True)
    parser.add_argument("--interval-seconds", type=float, default=1.0)
    parser.add_argument("--stop-file", default="")
    parser.add_argument(
        "--container-pattern",
        default="",
        help="Regex matched against Docker container names. Empty means all running containers.",
    )
    return parser.parse_args()


def parse_percent(value: str) -> float:
    try:
        return float(value.rstrip("%"))
    except (AttributeError, ValueError):
        return 0.0


def parse_memory(value: str) -> float:
    """Convert Docker's memory usage value to MiB."""
    match = re.match(r"\s*([0-9.]+)\s*([KMGT]?i?B?)", value or "")
    if not match:
        return 0.0
    amount = float(match.group(1))
    unit = match.group(2).lower()
    factors = {"b": 1 / 1024 / 1024, "kib": 1 / 1024, "kb": 1 / 1024}
    if unit in ("mib", "mb", "m"):
        return amount
    if unit in ("gib", "gb", "g"):
        return amount * 1024
    if unit in ("tib", "tb", "t"):
        return amount * 1024 * 1024
    return amount * factors.get(unit, 1.0)


def docker_rows(pattern: re.Pattern[str]) -> Iterable[Dict[str, object]]:
    command = [
        "docker",
        "stats",
        "--no-stream",
        "--format",
        "{{.Name}}\t{{.CPUPerc}}\t{{.MemUsage}}",
    ]
    try:
        completed = subprocess.run(
            command,
            capture_output=True,
            text=True,
            encoding="utf-8",
            errors="replace",
            check=False,
            timeout=20,
        )
    except (OSError, subprocess.SubprocessError):
        return []
    if completed.returncode != 0:
        return []

    rows: List[Dict[str, object]] = []
    for line in completed.stdout.splitlines():
        name, separator, values = line.partition("\t")
        if not separator or not pattern.search(name):
            continue
        cpu, separator, memory = values.partition("\t")
        used = memory.split("/", 1)[0].strip()
        rows.append(
            {
                "container_name": name,
                "container_cpu_pct": round(parse_percent(cpu), 3),
                "container_memory_mib": round(parse_memory(used), 3),
            }
        )
    return rows


def main() -> int:
    args = parse_args()
    output = Path(args.output)
    output.parent.mkdir(parents=True, exist_ok=True)
    pattern = re.compile(args.container_pattern) if args.container_pattern else re.compile(".*")
    deadline = time.monotonic() + args.duration_seconds
    rows: List[Dict[str, object]] = []

    while time.monotonic() < deadline and not (
        args.stop_file and Path(args.stop_file).exists()
    ):
        timestamp = datetime.now(timezone.utc).isoformat()
        host_memory = psutil.virtual_memory()
        base = {
            "timestamp_utc": timestamp,
            "host_cpu_pct": round(psutil.cpu_percent(interval=None), 3),
            "host_memory_used_mib": round(host_memory.used / 1024 / 1024, 3),
            "host_memory_pct": round(host_memory.percent, 3),
        }
        containers = list(docker_rows(pattern))
        if containers:
            for container in containers:
                rows.append({**base, **container})
        else:
            rows.append(
                {
                    **base,
                    "container_name": "",
                    "container_cpu_pct": "",
                    "container_memory_mib": "",
                }
            )
        time.sleep(max(args.interval_seconds, 0.1))

    fields = [
        "timestamp_utc",
        "host_cpu_pct",
        "host_memory_used_mib",
        "host_memory_pct",
        "container_name",
        "container_cpu_pct",
        "container_memory_mib",
    ]
    with output.open("w", newline="", encoding="utf-8") as handle:
        writer = csv.DictWriter(handle, fieldnames=fields)
        writer.writeheader()
        writer.writerows(rows)
    print(json.dumps({"samples": len(rows), "output": str(output)}, ensure_ascii=False))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
