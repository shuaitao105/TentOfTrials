import importlib.util
from pathlib import Path


def load_health_check():
    path = Path(__file__).resolve().parents[1] / "tools" / "health_check.py"
    spec = importlib.util.spec_from_file_location("health_check", path)
    module = importlib.util.module_from_spec(spec)
    assert spec.loader is not None
    spec.loader.exec_module(module)
    return module


def test_http_probe_succeeds_after_retry(monkeypatch):
    health_check = load_health_check()
    attempts = {"count": 0}
    sleeps = []

    def fake_request(host, port, path, timeout):
        attempts["count"] += 1
        if attempts["count"] == 1:
            raise TimeoutError("temporary timeout")
        return "OK", "HTTP 200", 200

    monkeypatch.setattr(health_check, "request_http_service_once", fake_request)

    result = health_check.check_http_service(
        "example.test", 8080, "/health", 5, max_attempts=3, backoff_seconds=1, sleep_fn=sleeps.append
    )

    assert result == ("OK", "HTTP 200", 200)
    assert attempts["count"] == 2
    assert sleeps == [1]


def test_http_probe_exhausts_retries_with_exponential_backoff(monkeypatch):
    health_check = load_health_check()
    attempts = {"count": 0}
    sleeps = []

    def fake_request(host, port, path, timeout):
        attempts["count"] += 1
        raise ConnectionError("connection refused")

    monkeypatch.setattr(health_check, "request_http_service_once", fake_request)

    result = health_check.check_http_service(
        "example.test", 8080, "/health", 5, max_attempts=3, backoff_seconds=1, sleep_fn=sleeps.append
    )

    assert result == ("CRITICAL", "connection refused", 0)
    assert attempts["count"] == 3
    assert sleeps == [1, 2]
