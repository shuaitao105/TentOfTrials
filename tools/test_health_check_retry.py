#!/usr/bin/env python3
"""Unit tests for HTTP probe retry/backoff in health_check."""

from __future__ import annotations

import sys
import unittest
from pathlib import Path
from unittest.mock import patch

TOOLS_DIR = Path(__file__).resolve().parent
if str(TOOLS_DIR) not in sys.path:
    sys.path.insert(0, str(TOOLS_DIR))

import health_check  # noqa: E402


class ProbeHttpWithRetryTests(unittest.TestCase):
    def test_success_after_retry(self) -> None:
        attempts = {"count": 0}
        sleeps: list[float] = []

        def flaky_probe(host: str, port: int, path: str, timeout: int):
            attempts["count"] += 1
            if attempts["count"] < 3:
                raise ConnectionRefusedError("transient network error")
            return "OK", "HTTP 200", 200

        result, detail, code = health_check.probe_http_with_retry(
            "localhost",
            8080,
            "/health",
            5,
            sleep_fn=lambda seconds: sleeps.append(seconds),
            probe_fn=flaky_probe,
        )

        self.assertEqual(("OK", "HTTP 200", 200), (result, detail, code))
        self.assertEqual(3, attempts["count"])
        self.assertEqual([1.0, 2.0], sleeps)

    def test_exhausted_retries_preserve_critical_result(self) -> None:
        attempts = {"count": 0}
        sleeps: list[float] = []

        def always_fail(host: str, port: int, path: str, timeout: int):
            attempts["count"] += 1
            raise ConnectionRefusedError("still down")

        result, detail, code = health_check.probe_http_with_retry(
            "localhost",
            8080,
            "/health",
            5,
            sleep_fn=lambda seconds: sleeps.append(seconds),
            probe_fn=always_fail,
        )

        self.assertEqual("CRITICAL", result)
        self.assertIn("still down", detail)
        self.assertEqual(0, code)
        self.assertEqual(3, attempts["count"])
        self.assertEqual([1.0, 2.0], sleeps)

    def test_warning_status_not_retried(self) -> None:
        attempts = {"count": 0}
        sleeps: list[float] = []

        def warn_once(host: str, port: int, path: str, timeout: int):
            attempts["count"] += 1
            return "WARNING", "HTTP 404: missing", 404

        result, detail, code = health_check.probe_http_with_retry(
            "localhost",
            8080,
            "/health",
            5,
            sleep_fn=lambda seconds: sleeps.append(seconds),
            probe_fn=warn_once,
        )

        self.assertEqual(("WARNING", "HTTP 404: missing", 404), (result, detail, code))
        self.assertEqual(1, attempts["count"])
        self.assertEqual([], sleeps)

    def test_server_error_retried_then_succeeds(self) -> None:
        attempts = {"count": 0}
        sleeps: list[float] = []

        def recover_503(host: str, port: int, path: str, timeout: int):
            attempts["count"] += 1
            if attempts["count"] < 2:
                return "CRITICAL", "HTTP 503: unavailable", 503
            return "OK", "HTTP 200", 200

        result, detail, code = health_check.probe_http_with_retry(
            "localhost",
            8080,
            "/health",
            5,
            sleep_fn=lambda seconds: sleeps.append(seconds),
            probe_fn=recover_503,
        )

        self.assertEqual(("OK", "HTTP 200", 200), (result, detail, code))
        self.assertEqual(2, attempts["count"])
        self.assertEqual([1.0], sleeps)

    @patch("health_check.probe_http_with_retry")
    def test_check_http_service_uses_retry_helper(self, mock_probe) -> None:
        mock_probe.return_value = ("OK", "HTTP 200", 200)

        result = health_check.check_http_service("localhost", 8080, "/health", 5)

        self.assertEqual(("OK", "HTTP 200", 200), result)
        mock_probe.assert_called_once_with("localhost", 8080, "/health", 5)


if __name__ == "__main__":
    unittest.main()
