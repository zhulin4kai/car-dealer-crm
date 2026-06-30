from __future__ import annotations

import json

import pytest
import respx
from httpx import Response

from app.providers.anthropic import AnthropicProvider
from app.providers.base import ChatMessage
from app.providers.openai_compatible import OpenAICompatibleProvider
from app.schemas.chat import ProviderRuntimeConfig


def runtime_config(
    provider_format: str,
    base_url: str = "https://api.example.com",
) -> ProviderRuntimeConfig:
    return ProviderRuntimeConfig(
        provider_config_no="AIPC-test",
        provider_format=provider_format,
        base_url=base_url,
        model_name="model-test",
        api_key="provider-key-for-test",
        timeout_seconds=15,
        max_output_tokens=64,
        temperature=0,
    )


@pytest.mark.asyncio
@respx.mock
async def test_openai_compatible_provider_parses_streaming_chunks() -> None:
    route = respx.post("https://api.example.com/chat/completions").mock(
        return_value=Response(
            200,
            content=(
                'data: {"choices":[{"delta":{"content":"你"}}]}\n\n'
                'data: {"choices":[{"delta":{"content":"好"}}]}\n\n'
                "data: [DONE]\n\n"
            ),
        )
    )

    chunks = [
        chunk
        async for chunk in OpenAICompatibleProvider().stream_chat(
            messages=[ChatMessage(role="user", content="只回复你好")],
            runtime_config=runtime_config("OPENAI_COMPATIBLE"),
        )
    ]

    assert route.called
    payload = json.loads(route.calls.last.request.content)
    assert payload["model"] == "model-test"
    assert payload["stream"] is True
    assert payload["max_tokens"] == 64
    assert route.calls.last.request.headers["Authorization"] == "Bearer provider-key-for-test"
    assert [chunk.content_delta for chunk in chunks if chunk.content_delta] == ["你", "好"]
    assert chunks[-1].is_final is True


@pytest.mark.asyncio
@respx.mock
async def test_anthropic_provider_parses_streaming_chunks() -> None:
    route = respx.post("https://api.example.com/v1/messages").mock(
        return_value=Response(
            200,
            content=(
                'data: {"type":"content_block_delta","delta":{"text":"你"}}\n\n'
                'data: {"type":"content_block_delta","delta":{"text":"好"}}\n\n'
                'data: {"type":"message_stop"}\n\n'
            ),
        )
    )

    chunks = [
        chunk
        async for chunk in AnthropicProvider().stream_chat(
            messages=[
                ChatMessage(role="system", content="系统提示"),
                ChatMessage(role="user", content="只回复你好"),
            ],
            runtime_config=runtime_config("ANTHROPIC"),
        )
    ]

    assert route.called
    payload = json.loads(route.calls.last.request.content)
    assert payload["model"] == "model-test"
    assert payload["stream"] is True
    assert payload["system"] == "系统提示"
    assert route.calls.last.request.headers["anthropic-version"] == "2023-06-01"
    assert [chunk.content_delta for chunk in chunks if chunk.content_delta] == ["你", "好"]
    assert chunks[-1].is_final is True
