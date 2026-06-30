"""AI 编排器抽象接口，隔离具体编排实现。"""

from __future__ import annotations

from abc import ABC, abstractmethod
from collections.abc import AsyncIterator

from app.schemas.chat import ChatRunRequest
from app.schemas.events import InternalAiEvent


class AgentOrchestrator(ABC):
    """把用户请求转换为内部 AI 事件流的编排器边界。"""

    @abstractmethod
    async def run(self, request: ChatRunRequest) -> AsyncIterator[InternalAiEvent]:
        """执行一次 AI Run，并流式返回内部事件。"""

        raise NotImplementedError
