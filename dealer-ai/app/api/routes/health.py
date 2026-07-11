"""健康检查路由，仅用于内部服务存活探测。"""

from __future__ import annotations

from fastapi import APIRouter, Request
from fastapi.responses import JSONResponse

router = APIRouter(tags=["health"])


@router.get("/health")
async def health() -> dict[str, str]:
    """返回 `dealer-ai` 存活状态，不泄露配置和模型供应商信息。"""

    return {"status": "UP", "service": "dealer-ai"}


@router.get("/ready")
async def ready(request: Request) -> JSONResponse:
    """确认进程配置已经校验，不探测 Spring Boot、数据库或模型供应商。"""

    if getattr(request.app.state, "settings", None) is None:
        return JSONResponse(
            status_code=503,
            content={"status": "NOT_READY", "service": "dealer-ai"},
        )
    return JSONResponse(content={"status": "READY", "service": "dealer-ai"})
