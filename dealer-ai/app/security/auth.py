"""内部服务认证校验，只接受 Spring Boot 配置的共享令牌。"""

from __future__ import annotations

from hmac import compare_digest

from fastapi import Header

from app.core.config import Settings, get_settings
from app.core.errors import UnauthorizedServiceError


async def verify_internal_token(
    x_dealer_ai_token: str | None = Header(default=None, alias="X-Dealer-AI-Token"),
) -> None:
    """校验 Spring Boot 调用 `dealer-ai` 的内部服务令牌。"""

    settings = get_settings()
    verify_token_value(x_dealer_ai_token, settings)


def verify_token_value(token: str | None, settings: Settings) -> None:
    """使用常量时间比较校验令牌，避免把用户 Bearer Token 带入服务。"""

    if not token or not compare_digest(token, settings.internal_token):
        raise UnauthorizedServiceError()
