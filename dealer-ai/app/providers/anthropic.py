"""Anthropic Messages API Provider 适配器，使用请求级 runtime config。"""

from __future__ import annotations

import json
from collections.abc import AsyncIterator

import httpx

from app.core.errors import ExternalServiceError
from app.providers.base import ChatCompletionChunk, ChatMessage, ProviderAdapter
from app.schemas.chat import ProviderRuntimeConfig


class AnthropicProvider(ProviderAdapter):
    """封装 Anthropic Messages 流式接口，不向上层传播原始响应或密钥。"""

    async def stream_chat(
        self,
        *,
        messages: list[ChatMessage],
        runtime_config: ProviderRuntimeConfig,
    ) -> AsyncIterator[ChatCompletionChunk]:
        """调用 `/v1/messages` 并把 SSE delta 转换为内部片段。"""

        endpoint = f"{str(runtime_config.base_url).rstrip('/')}/v1/messages"
        payload = {
            "model": runtime_config.model_name,
            "messages": [
                message.model_dump()
                for message in messages
                if message.role != "system"
            ],
            "system": "\n".join(
                message.content for message in messages if message.role == "system"
            ),
            "max_tokens": runtime_config.max_output_tokens,
            "temperature": runtime_config.temperature,
            "stream": True,
        }
        headers = {
            "x-api-key": runtime_config.api_key,
            "anthropic-version": "2023-06-01",
            "content-type": "application/json",
        }
        try:
            async with httpx.AsyncClient(
                timeout=runtime_config.timeout_seconds,
                trust_env=False,
            ) as client, client.stream(
                "POST",
                endpoint,
                json=payload,
                headers=headers,
            ) as response:
                response.raise_for_status()
                async for line in response.aiter_lines():
                    chunk = _parse_anthropic_sse_line(line)
                    if chunk is None:
                        continue
                    yield chunk
                    if chunk.is_final:
                        return
        except httpx.HTTPError as exc:
            raise ExternalServiceError("AI_PROVIDER_FAILED", "model provider failed") from exc


def _parse_anthropic_sse_line(line: str) -> ChatCompletionChunk | None:
    """解析 Anthropic SSE 单行，无法识别时忽略。"""

    if not line.startswith("data:"):
        return None
    data = line.removeprefix("data:").strip()
    if not data:
        return None
    try:
        payload = json.loads(data)
    except json.JSONDecodeError:
        return None
    event_type = payload.get("type")
    if event_type == "message_stop":
        return ChatCompletionChunk(is_final=True)
    if event_type != "content_block_delta":
        return None
    delta = payload.get("delta")
    if not isinstance(delta, dict):
        return None
    text = delta.get("text")
    if isinstance(text, str) and text:
        return ChatCompletionChunk(content_delta=text)
    return None
