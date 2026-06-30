"""模型 Provider 抽象，限定编排器可依赖的最小输出形状。"""

from __future__ import annotations

from abc import ABC, abstractmethod
from collections.abc import AsyncIterator

from pydantic import BaseModel, Field

from app.schemas.chat import ProviderRuntimeConfig


class ChatMessage(BaseModel):
    """发送给模型 Provider 的对话消息，禁止承载认证令牌或业务权限上下文。"""

    role: str = Field(min_length=1, max_length=32)
    content: str = Field(default="", max_length=8000)


class ChatCompletionChunk(BaseModel):
    """模型响应增量片段，只保留可展示文本和完成标记。"""

    content_delta: str = ""
    is_final: bool = False


class ProviderAdapter(ABC):
    """模型 Provider 适配器边界，屏蔽供应商协议和原始响应结构。"""

    @abstractmethod
    async def stream_chat(
        self,
        *,
        messages: list[ChatMessage],
        runtime_config: ProviderRuntimeConfig,
    ) -> AsyncIterator[ChatCompletionChunk]:
        """流式返回已规整的模型文本片段，不暴露供应商原始响应。"""

        raise NotImplementedError
