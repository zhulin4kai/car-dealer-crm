from __future__ import annotations

import pytest
from pydantic import ValidationError

from app.core.config import Settings, ensure_url_allowed
from app.core.errors import ConfigurationError, UnsafeOutboundTargetError
from app.schemas.chat import ChatRunRequest


def test_ensure_url_allowed_accepts_same_origin() -> None:
    ensure_url_allowed(
        "https://provider.example/v1/chat/completions", ["https://provider.example/v1"]
    )


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


def test_chat_run_request_accepts_camel_case_assistant_policy() -> None:
    request = ChatRunRequest.model_validate(
        {
            "run_id": "run-1",
            "user_prompt": "总结客户",
            "assistantPolicy": {
                "proposalsEnabled": False,
                "maxToolCallsPerRun": 2,
                "safetyMode": "STRICT",
                "networkMode": "DISABLED",
                "contextMessageLimit": 4,
                "summaryMaxChars": 800,
                "maxRunSeconds": 30,
                "businessInstruction": "回答保持简洁",
            },
            "provider_runtime_config": {
                "provider_config_no": "AIPC-test",
                "provider_format": "OPENAI_COMPATIBLE",
                "base_url": "https://provider.test",
                "model_name": "mock-model",
                "api_key": "test-key",
            },
        }
    )

    assert request.assistant_policy is not None
    assert request.assistant_policy.max_tool_calls_per_run == 2
    assert request.assistant_policy.safety_mode == "STRICT"
    assert request.assistant_policy.business_instruction == "回答保持简洁"


def test_assistant_policy_rejects_unimplemented_search_only_mode() -> None:
    with pytest.raises(ValidationError):
        ChatRunRequest.model_validate(
            {
                "run_id": "run-1",
                "user_prompt": "联网搜索",
                "assistantPolicy": {"networkMode": "SEARCH_ONLY"},
            }
        )
