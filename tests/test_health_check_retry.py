import pathlib
import sys
import unittest
from unittest import mock


ROOT = pathlib.Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "tools"))

import health_check  # noqa: E402


class FakeResponse:
    def __init__(self, status, body=b"ok"):
        self.status = status
        self._body = body

    def read(self):
        return self._body


class FakeConnection:
    attempts = []
    outcomes = []

    def __init__(self, host, port, timeout):
        self.host = host
        self.port = port
        self.timeout = timeout
        FakeConnection.attempts.append((host, port, timeout))

    def request(self, method, path):
        self.method = method
        self.path = path

    def getresponse(self):
        outcome = FakeConnection.outcomes.pop(0)
        if isinstance(outcome, Exception):
            raise outcome
        return outcome

    def close(self):
        pass


class HealthCheckRetryTests(unittest.TestCase):
    def setUp(self):
        FakeConnection.attempts = []
        FakeConnection.outcomes = []

    def test_http_probe_succeeds_after_retry(self):
        FakeConnection.outcomes = [
            TimeoutError("temporary timeout"),
            FakeResponse(200, b"healthy"),
        ]

        with mock.patch("health_check.http.client.HTTPConnection", FakeConnection):
            with self.assertLogs("health_check", level="WARNING") as logs:
                status, body = health_check.request_http_with_retries(
                    "example.test",
                    8080,
                    "/health",
                    timeout=5,
                    sleep=lambda _delay: None,
                )

        self.assertEqual(status, 200)
        self.assertEqual(body, "healthy")
        self.assertEqual(len(FakeConnection.attempts), 2)
        self.assertIn("attempt 1/3", logs.output[0])

    def test_http_probe_exhausts_retries_with_exponential_backoff(self):
        FakeConnection.outcomes = [
            OSError("connection reset"),
            OSError("still down"),
            OSError("still down"),
        ]
        delays = []

        with mock.patch("health_check.http.client.HTTPConnection", FakeConnection):
            with self.assertLogs("health_check", level="WARNING") as logs:
                with self.assertRaises(OSError):
                    health_check.request_http_with_retries(
                        "example.test",
                        8080,
                        "/health",
                        timeout=5,
                        sleep=delays.append,
                    )

        self.assertEqual(delays, [1.0, 2.0])
        self.assertEqual(len(FakeConnection.attempts), 3)
        self.assertEqual(len(logs.output), 2)


if __name__ == "__main__":
    unittest.main()
