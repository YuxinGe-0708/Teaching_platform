import json
import time
from dataclasses import dataclass
from typing import Any, Dict, Optional

import requests


@dataclass
class ServiceUrls:
    user: str
    learning: str
    assessment: str


class ApiClient:
    def __init__(self, urls: ServiceUrls, timeout: int = 10, internal_api_key: str = "dev-internal-key"):
        self.urls = urls
        self.timeout = timeout
        self.internal_api_key = internal_api_key
        self.session = requests.Session()
        self.token: Optional[str] = None
        self.current_user: Dict[str, Any] = {}

    def login(self, username: str, password: str = "123456") -> Dict[str, Any]:
        payload = {"username": username, "password": password}
        data = self.post("user", "/api/auth/login", json=payload)
        self.token = data["token"]
        self.current_user = data["user"]
        return data

    def get(self, service: str, path: str, *, params: Optional[Dict[str, Any]] = None, internal: bool = False) -> Any:
        response = self.session.get(
            self._url(service, path),
            params=params,
            headers=self._headers(internal),
            timeout=self.timeout,
        )
        return self._data(response)

    def post(
        self,
        service: str,
        path: str,
        *,
        data: Optional[Dict[str, Any]] = None,
        json: Optional[Dict[str, Any]] = None,
        internal: bool = False,
    ) -> Any:
        response = self.session.post(
            self._url(service, path),
            data=data,
            json=json,
            headers=self._headers(internal),
            timeout=self.timeout,
        )
        return self._data(response)

    def put(self, service: str, path: str, *, data: Optional[Dict[str, Any]] = None, internal: bool = False) -> Any:
        response = self.session.put(
            self._url(service, path),
            data=data,
            headers=self._headers(internal),
            timeout=self.timeout,
        )
        return self._data(response)

    def delete(self, service: str, path: str, *, params: Optional[Dict[str, Any]] = None, internal: bool = False) -> Any:
        response = self.session.delete(
            self._url(service, path),
            params=params,
            headers=self._headers(internal),
            timeout=self.timeout,
        )
        return self._data(response)

    def wait_until_ready(self, retries: int = 60, interval: float = 2.0) -> None:
        last_error = ""
        for _ in range(retries):
            try:
                probes = {
                    "user": self.get("user", "/actuator/health"),
                    "learning": self.get("learning", "/actuator/health"),
                    "assessment": self.get("assessment", "/actuator/health"),
                }
                missing = [name for name, value in probes.items() if not value]
                if not missing:
                    return
            except Exception as exc:
                last_error = str(exc)
            time.sleep(interval)
        raise AssertionError(f"microservices are not ready: {last_error}")

    def _url(self, service: str, path: str) -> str:
        base = getattr(self.urls, service).rstrip("/")
        return base + path

    def _headers(self, internal: bool) -> Dict[str, str]:
        headers: Dict[str, str] = {"Accept": "application/json"}
        if self.token:
            headers["Authorization"] = f"Bearer {self.token}"
            if self.current_user:
                headers["X-User-Id"] = str(self.current_user.get("id", ""))
                headers["X-User-Role"] = str(self.current_user.get("role", ""))
                headers["X-User-Name"] = str(self.current_user.get("username", ""))
        if internal:
            headers["X-Internal-Api-Key"] = self.internal_api_key
        return headers

    def _data(self, response: requests.Response) -> Any:
        try:
            body = response.json()
        except json.JSONDecodeError:
            body = {"raw": response.text}
        assert response.status_code < 500, f"{response.request.method} {response.url} -> {response.status_code}: {body}"
        assert 200 <= response.status_code < 300, f"{response.request.method} {response.url} -> {response.status_code}: {body}"
        if isinstance(body, dict) and "code" in body:
            assert body.get("code") == 200, f"{response.request.method} {response.url} business failure: {body}"
            return body.get("data")
        return body
