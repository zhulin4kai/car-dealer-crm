"""健康检查路由，仅用于内部服务存活探测。"""

from __future__ import annotations

from fastapi import APIRouter

router = APIRouter(tags=["health"])


@router.get("/health")
async def health() -> dict[str, str]:
    """返回 `dealer-ai` 存活状态，不泄露配置和模型供应商信息。"""

    return {"status": "UP", "service": "dealer-ai"}
