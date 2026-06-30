"""AI Run 请求和状态模型，字段名与 Spring Boot 内部契约保持一致。"""

from __future__ import annotations

from enum import StrEnum
from typing import Any

from pydantic import AnyHttpUrl, BaseModel, Field


class RunStatus(StrEnum):
    """AI Run 生命周期状态，只表达 Spring Boot 已支持的状态值。"""

    CREATED = "CREATED"
    RUNNING = "RUNNING"
    WAITING_FOR_APPROVAL = "WAITING_FOR_APPROVAL"
    COMPLETED = "COMPLETED"
    FAILED = "FAILED"
    CANCELLED = "CANCELLED"
    EXPIRED = "EXPIRED"


class AiContextObject(BaseModel):
    """前端页面上下文对象，只传对象类型和标识，不作为权限来源。"""

    object_type: str = Field(min_length=1, max_length=64)
    object_id: str = Field(min_length=1, max_length=64)


class ProviderRuntimeConfig(BaseModel):
    """Spring Boot 下发的请求级 Provider 配置，禁止持久化和对前端输出。"""

    provider_config_no: str = Field(min_length=1, max_length=64)
    provider_format: str = Field(pattern="^(OPENAI_COMPATIBLE|ANTHROPIC)$")
    base_url: AnyHttpUrl
    model_name: str = Field(min_length=1, max_length=128)
    api_key: str = Field(min_length=1, max_length=500)
    timeout_seconds: int = Field(default=15, ge=1, le=60)
    max_output_tokens: int = Field(default=512, ge=1, le=4096)
    temperature: float = Field(default=0.7, ge=0.0, le=2.0)


class MessageHistoryItem(BaseModel):
    """Spring Boot 下发的脱敏会话历史消息，只用于本次模型上下文。"""

    role: str = Field(pattern="^(user|assistant|system|tool)$")
    content_summary: str = Field(min_length=1, max_length=2000)


class ChatRunRequest(BaseModel):
    """Spring Boot 发起 AI Run 编排的内部请求。"""

    run_id: str = Field(min_length=1, max_length=64)
    user_prompt: str = Field(min_length=1, max_length=4000)
    conversation_no: str | None = Field(default=None, max_length=64)
    conversation_summary: str | None = Field(default=None, max_length=2000)
    message_history: list[MessageHistoryItem] = Field(default_factory=list, max_length=8)
    context: AiContextObject | None = None
    tool_schemas: list[dict[str, Any]] = Field(default_factory=list)
    allow_proposals: bool = False
    provider_runtime_config: ProviderRuntimeConfig


class ChatRunResponse(BaseModel):
    """AI Run 创建后的内部响应摘要。"""

    run_id: str
    status: RunStatus
