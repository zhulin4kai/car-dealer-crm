"""FastAPI 应用入口，承载 Spring Boot 内部 AI 编排接口。"""

from __future__ import annotations

import logging

from fastapi import FastAPI, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse

from app.api.routes import health, runs
from app.core.errors import ServiceError
from app.core.logging import configure_logging

logger = logging.getLogger(__name__)


def create_app() -> FastAPI:
    """创建 `dealer-ai` 应用，只注册内部路由和稳定错误转换器。"""

    configure_logging()
    app = FastAPI(title="dealer-ai", version="0.1.0")
    app.include_router(health.router)
    app.include_router(runs.router)

    @app.exception_handler(ServiceError)
    async def handle_service_error(_: Request, exc: ServiceError) -> JSONResponse:
        """把服务异常转换为稳定错误码响应，不暴露内部堆栈。"""

        return JSONResponse(
            status_code=exc.status_code,
            content={"code": exc.code, "message": exc.message},
        )

    @app.exception_handler(RequestValidationError)
    async def handle_request_validation_error(
        _: Request, exc: RequestValidationError
    ) -> JSONResponse:
        """输出脱敏校验错误，避免内部契约问题被折叠成不可定位的 422。"""

        details = [
            {
                "loc": list(error.get("loc", [])),
                "type": error.get("type", "validation_error"),
                "message": error.get("msg", "validation error"),
            }
            for error in exc.errors()
        ]
        logger.warning("dealer-ai request validation failed: %s", details)
        return JSONResponse(
            status_code=422,
            content={
                "code": "REQUEST_VALIDATION_FAILED",
                "message": "request validation failed",
                "details": details,
            },
        )

    return app


app = create_app()
