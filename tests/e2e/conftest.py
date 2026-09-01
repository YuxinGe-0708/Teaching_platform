import json
import os
from pathlib import Path
from typing import Any, Dict, List

import pytest

from api_client import ApiClient, ServiceUrls


def pytest_addoption(parser):
    parser.addoption("--user-url", default=os.getenv("E2E_USER_SERVICE_URL", "http://localhost:8082"))
    parser.addoption("--learning-url", default=os.getenv("E2E_LEARNING_SERVICE_URL", "http://localhost:8083"))
    parser.addoption("--assessment-url", default=os.getenv("E2E_ASSESSMENT_SERVICE_URL", "http://localhost:8084"))
    parser.addoption("--internal-api-key", default=os.getenv("INTERNAL_API_KEY", "dev-internal-key"))
    parser.addoption("--report-file", default=os.getenv("E2E_REPORT_FILE", "ci-artifacts/e2e-report.json"))


@pytest.fixture(scope="session")
def service_urls(pytestconfig) -> ServiceUrls:
    return ServiceUrls(
        user=pytestconfig.getoption("--user-url"),
        learning=pytestconfig.getoption("--learning-url"),
        assessment=pytestconfig.getoption("--assessment-url"),
    )


@pytest.fixture(scope="session")
def api(service_urls, pytestconfig) -> ApiClient:
    client = ApiClient(service_urls, internal_api_key=pytestconfig.getoption("--internal-api-key"))
    client.wait_until_ready()
    return client


@pytest.fixture(scope="session")
def e2e_records() -> List[Dict[str, Any]]:
    records: List[Dict[str, Any]] = []
    return records


@pytest.fixture
def record_case(e2e_records, pytestconfig):
    setattr(pytestconfig, "_e2e_records", e2e_records)

    def add(case_id: str, name: str, mainline: str, status: str, evidence: Dict[str, Any]):
        e2e_records.append(
            {
                "caseId": case_id,
                "name": name,
                "mainline": mainline,
                "status": status,
                "evidence": evidence,
            }
        )
    return add


@pytest.hookimpl(trylast=True)
def pytest_sessionfinish(session, exitstatus):
    records = []
    for item in session.items:
        call = getattr(item, "rep_call", None)
        records.append(
            {
                "test": item.nodeid,
                "outcome": "unknown" if call is None else call.outcome,
            }
        )
    report_path = Path(session.config.getoption("--report-file"))
    cases = getattr(session.config, "_e2e_records", [])
    report_path.parent.mkdir(parents=True, exist_ok=True)
    report_path.write_text(
        json.dumps(
            {
                "exitstatus": exitstatus,
                "summary": records,
                "cases": cases,
            },
            ensure_ascii=False,
            indent=2,
        ),
        encoding="utf-8",
    )


@pytest.hookimpl(hookwrapper=True, tryfirst=True)
def pytest_runtest_makereport(item, call):
    outcome = yield
    report = outcome.get_result()
    setattr(item, "rep_" + report.when, report)
