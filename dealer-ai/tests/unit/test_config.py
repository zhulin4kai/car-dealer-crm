from __future__ import annotations

import pytest

from app.core.config import Settings, ensure_url_allowed
from app.core.errors import ConfigurationError, UnsafeOutboundTargetError


def test_ensure_url_allowed_accepts_same_origin() -> None:
    ensure_url_allowed("https://provider.example/v1/chat/completions", ["https://provider.example/v1"])


def test_ensure_url_allowed_rejects_non_whitelisted_origin() -> None:
    with pytest.raises(UnsafeOutboundTargetError):
        ensure_url_allowed(
            "https://attacker.example/v1/chat/completions",
            ["https://provider.example/v1"],
        )


def test_local_defaults_use_matching_internal_and_spring_tool_token() -> None:
    settings = Settings()

    assert settings.internal_token == "dev-internal-token"
    assert settings.spring_tool_token == "dev-internal-token"


def test_non_local_environment_requires_explicit_service_tokens() -> None:
    with pytest.raises(ConfigurationError):
        Settings(environment="prod")


def test_settings_accepts_request_timeout_alias(monkeypatch: pytest.MonkeyPatch) -> None:
    monkeypatch.setenv("AI_PROVIDER_TIMEOUT_SECONDS", "20")

    settings = Settings()

    assert settings.request_timeout_seconds == 20
    assert settings.max_runtime_timeout_seconds == 60
    assert settings.max_runtime_output_tokens == 4096
