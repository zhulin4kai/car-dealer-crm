"""请求级 Provider Adapter 路由。"""

from __future__ import annotations

from collections.abc import AsyncIterator

from app.core.errors import ExternalServiceError
from app.providers.anthropic import AnthropicProvider
from app.providers.base import ChatCompletionChunk, ChatMessage, ProviderAdapter
from app.providers.openai_compatible import OpenAICompatibleProvider
from app.schemas.chat import ProviderRuntimeConfig


class RuntimeProviderAdapter(ProviderAdapter):
    """按 Spring Boot 下发的 Provider 格式选择具体适配器。"""

    def __init__(self) -> None:
        self._openai = OpenAICompatibleProvider()
        self._anthropic = AnthropicProvider()

    async def stream_chat(
        self,
        *,
        messages: list[ChatMessage],
        runtime_config: ProviderRuntimeConfig,
    ) -> AsyncIterator[ChatCompletionChunk]:
        """根据 runtime config 执行流式模型调用。"""

        if runtime_config.provider_format == "OPENAI_COMPATIBLE":
            async for chunk in self._openai.stream_chat(
                messages=messages,
                runtime_config=runtime_config,
            ):
                yield chunk
            return
        if runtime_config.provider_format == "ANTHROPIC":
            async for chunk in self._anthropic.stream_chat(
                messages=messages,
                runtime_config=runtime_config,
            ):
                yield chunk
            return
        raise ExternalServiceError(
            "AI_PROVIDER_UNSUPPORTED_FORMAT",
            "unsupported provider format",
            400,
        )
