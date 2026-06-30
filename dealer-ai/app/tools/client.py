"""调用 Spring Boot 内部 Tool API 的受控客户端。"""

from __future__ import annotations

import logging

import httpx

from app.core.config import Settings, join_allowed_url
from app.core.errors import ExternalServiceError
from app.schemas.tools import ToolCallRequest, ToolCallResult

logger = logging.getLogger(__name__)


class ToolClient:
    """只访问配置白名单内 Spring Boot Tool API 的工具客户端。"""

    def __init__(self, settings: Settings) -> None:
        """固定 Spring Boot Tool API 基址，后续请求仍逐次执行白名单校验。"""

        self._settings = settings
        self._allowed_urls = [str(settings.spring_tool_base_url)]

    async def execute(self, request: ToolCallRequest) -> ToolCallResult:
        """执行工具调用并返回脱敏摘要，禁止拼接白名单外地址。"""

        endpoint = join_allowed_url(
            str(self._settings.spring_tool_base_url),
            f"tools/{request.tool_name}/execute",
            self._allowed_urls,
        )
        headers = {"X-Dealer-AI-Tool-Token": self._settings.spring_tool_token}
        body = {"runNo": request.run_id, "arguments": request.arguments}
        try:
            async with httpx.AsyncClient(
                timeout=self._settings.request_timeout_seconds,
                trust_env=False,
            ) as client:
                response = await client.post(endpoint, json=body, headers=headers)
                response.raise_for_status()
        except httpx.HTTPStatusError as exc:
            logger.warning(
                "spring tool api returned non-success status: status=%s body=%s",
                exc.response.status_code,
                _response_summary(exc.response),
            )
            raise ExternalServiceError("SPRING_TOOL_FAILED", "spring tool api failed") from exc
        except httpx.HTTPError as exc:
            raise ExternalServiceError("SPRING_TOOL_FAILED", "spring tool api failed") from exc

        data = response.json()
        if isinstance(data, dict) and isinstance(data.get("data"), dict):
            data = data["data"]
        return ToolCallResult(
            tool_name=data.get("toolName", request.tool_name),
            summary=data.get("outputSummary", ""),
            data=data.get("data", {}),
        )


def _response_summary(response: httpx.Response) -> str:
    """提取 Spring Tool API 脱敏错误摘要，不记录请求头或完整响应体。"""

    try:
        data = response.json()
    except ValueError:
        return response.text[:200]
    if not isinstance(data, dict):
        return str(data)[:200]
    code = data.get("code")
    message = data.get("msg") or data.get("message")
    return f"code={code}, message={message}"
