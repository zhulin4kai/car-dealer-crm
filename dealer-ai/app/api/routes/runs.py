"""Spring Boot 内部调用的 AI Run 编排路由。"""

from __future__ import annotations

from fastapi import APIRouter, Depends
from fastapi.responses import StreamingResponse

from app.core.config import get_settings
from app.orchestrator.base import AgentOrchestrator
from app.orchestrator.langgraph_adapter import LangGraphAgentOrchestrator
from app.providers.runtime import RuntimeProviderAdapter
from app.schemas.chat import ChatRunRequest
from app.schemas.events import InternalAiEvent
from app.security.auth import verify_internal_token
from app.streaming.sse import encode_internal_events
from app.telemetry.run_events import collect_events
from app.tools.client import ToolClient

router = APIRouter(
    prefix="/internal/runs",
    tags=["internal-runs"],
    dependencies=[Depends(verify_internal_token)],
)


def get_orchestrator() -> AgentOrchestrator:
    """创建一次请求范围内的 LangGraph 编排器，固定 Tool API 边界。"""

    settings = get_settings()
    return LangGraphAgentOrchestrator(RuntimeProviderAdapter(), ToolClient(settings))


ORCHESTRATOR_DEPENDENCY = Depends(get_orchestrator)


@router.post("", response_model=list[InternalAiEvent])
async def create_run_events(
    request: ChatRunRequest,
    orchestrator: AgentOrchestrator = ORCHESTRATOR_DEPENDENCY,
) -> list[InternalAiEvent]:
    """返回一次 AI Run 的内部事件列表，供 Spring Boot 持久化和转发。"""

    return await collect_events(orchestrator.run(request))


@router.post("/stream")
async def stream_run_events(
    request: ChatRunRequest,
    orchestrator: AgentOrchestrator = ORCHESTRATOR_DEPENDENCY,
) -> StreamingResponse:
    """以 SSE 形式输出内部事件，调用方仍限定为 Spring Boot。"""

    return StreamingResponse(
        encode_internal_events(orchestrator.run(request)),
        media_type="text/event-stream",
        headers={
            "Cache-Control": "no-cache, no-transform",
            "Connection": "keep-alive",
            "X-Accel-Buffering": "no",
        },
    )
