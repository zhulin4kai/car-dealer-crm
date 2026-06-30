"""OpenAI-compatible Provider 适配器，使用请求级 runtime config。"""

from __future__ import annotations

import json
from collections.abc import AsyncIterator

import httpx

from app.core.errors import ExternalServiceError
from app.providers.base import ChatCompletionChunk, ChatMessage, ProviderAdapter
from app.schemas.chat import ProviderRuntimeConfig


class OpenAICompatibleProvider(ProviderAdapter):
    """封装 OpenAI-compatible 流式接口，不向上层传播原始响应或密钥。"""

    async def stream_chat(
        self,
        *,
        messages: list[ChatMessage],
        runtime_config: ProviderRuntimeConfig,
    ) -> AsyncIterator[ChatCompletionChunk]:
        """调用 `/chat/completions` 并把 SSE delta 转换为内部片段。"""

        endpoint = f"{str(runtime_config.base_url).rstrip('/')}/chat/completions"
        payload = {
            "model": runtime_config.model_name,
            "messages": [message.model_dump() for message in messages],
            "stream": True,
            "max_tokens": runtime_config.max_output_tokens,
            "temperature": runtime_config.temperature,
        }
        headers = {
            "Authorization": f"Bearer {runtime_config.api_key}",
            "Content-Type": "application/json",
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
                    chunk = _parse_openai_sse_line(line)
                    if chunk is None:
                        continue
                    yield chunk
                    if chunk.is_final:
                        return
        except httpx.HTTPError as exc:
            raise ExternalServiceError("AI_PROVIDER_FAILED", "model provider failed") from exc


def _parse_openai_sse_line(line: str) -> ChatCompletionChunk | None:
    """解析 OpenAI-compatible SSE 单行，无法识别时忽略。"""

    if not line.startswith("data:"):
        return None
    data = line.removeprefix("data:").strip()
    if not data:
        return None
    if data == "[DONE]":
        return ChatCompletionChunk(is_final=True)
    try:
        payload = json.loads(data)
    except json.JSONDecodeError:
        return None
    choices = payload.get("choices")
    if not isinstance(choices, list) or not choices:
        return None
    first = choices[0]
    if not isinstance(first, dict):
        return None
    delta = first.get("delta")
    if not isinstance(delta, dict):
        return None
    content = delta.get("content")
    if isinstance(content, str) and content:
        return ChatCompletionChunk(content_delta=content)
    return None
