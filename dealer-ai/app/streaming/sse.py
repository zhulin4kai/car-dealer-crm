"""内部事件到 SSE 文本帧的编码工具。"""

from __future__ import annotations

import asyncio
import json
from collections.abc import AsyncIterator

from app.schemas.events import InternalAiEvent

HEARTBEAT_INTERVAL_SECONDS = 15.0


async def encode_internal_events(events: AsyncIterator[InternalAiEvent]) -> AsyncIterator[str]:
    """编码内部事件，并在长耗时节点期间发送不会进入业务事件流的心跳注释。"""

    iterator = events.__aiter__()
    pending = asyncio.create_task(anext(iterator))
    try:
        while True:
            done, _ = await asyncio.wait({pending}, timeout=HEARTBEAT_INTERVAL_SECONDS)
            if not done:
                yield ": heartbeat\n\n"
                continue
            try:
                event = pending.result()
            except StopAsyncIteration:
                return
            payload = event.model_dump(mode="json")
            data = json.dumps(payload, ensure_ascii=False)
            pending = asyncio.create_task(anext(iterator))
            yield f"id: {event.event_id}\nevent: {event.event_type.value}\ndata: {data}\n\n"
    finally:
        if not pending.done():
            pending.cancel()
            await asyncio.gather(pending, return_exceptions=True)
