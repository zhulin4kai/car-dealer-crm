from __future__ import annotations

import asyncio
from collections.abc import AsyncIterator

import pytest

from app.schemas.events import AiEventType, InternalAiEvent
from app.streaming import sse


@pytest.mark.asyncio
async def test_sse_encoder_emits_heartbeat_while_waiting_for_next_event(
    monkeypatch: pytest.MonkeyPatch,
) -> None:
    async def delayed_events() -> AsyncIterator[InternalAiEvent]:
        await asyncio.sleep(0.03)
        yield InternalAiEvent(
            run_id="run-1",
            sequence=1,
            event_type=AiEventType.RUN_STARTED,
        )

    monkeypatch.setattr(sse, "HEARTBEAT_INTERVAL_SECONDS", 0.01)

    frames = [frame async for frame in sse.encode_internal_events(delayed_events())]

    assert frames[0] == ": heartbeat\n\n"
    assert any("event: run_started" in frame for frame in frames)
