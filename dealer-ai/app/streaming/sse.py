"""内部事件到 SSE 文本帧的编码工具。"""

from __future__ import annotations

import json
from collections.abc import AsyncIterator

from app.schemas.events import InternalAiEvent


async def encode_internal_events(events: AsyncIterator[InternalAiEvent]) -> AsyncIterator[str]:
    """把内部 AI 事件编码为 SSE 帧，不附加供应商原始响应。"""

    async for event in events:
        payload = event.model_dump(mode="json")
        data = json.dumps(payload, ensure_ascii=False)
        yield f"id: {event.event_id}\nevent: {event.event_type.value}\ndata: {data}\n\n"
