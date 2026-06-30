"""AI Run 内部事件序列工具，保证事件顺序可恢复。"""

from __future__ import annotations

from collections.abc import AsyncIterator

from app.schemas.events import AiEventType, InternalAiEvent


class EventSequence:
    """为单个 AI Run 生成递增序号的内部事件。"""

    def __init__(self, run_id: str) -> None:
        self._run_id = run_id
        self._next = 1

    def make(
        self,
        event_type: AiEventType,
        payload: dict[str, object] | None = None,
    ) -> InternalAiEvent:
        """按当前序号构造事件，并推进下一条事件序号。"""

        event = InternalAiEvent(
            run_id=self._run_id,
            sequence=self._next,
            event_type=event_type,
            payload=payload or {},
        )
        self._next += 1
        return event


async def collect_events(events: AsyncIterator[InternalAiEvent]) -> list[InternalAiEvent]:
    """收集内部事件流，供非流式内部接口一次性返回。"""

    return [event async for event in events]
