"""内部 AI 事件模型，由 Spring Boot 转换为外部 SSE 事件。"""

from __future__ import annotations

from datetime import UTC, datetime
from enum import StrEnum
from typing import Any
from uuid import uuid4

from pydantic import BaseModel, Field


class AiEventType(StrEnum):
    """允许输出给 Spring Boot 持久化和转发的内部 AI 事件类型。"""

    RUN_STARTED = "run_started"
    MESSAGE_DELTA = "message_delta"
    MESSAGE_COMPLETED = "message_completed"
    TOOL_CALL_STARTED = "tool_call_started"
    TOOL_CALL_COMPLETED = "tool_call_completed"
    ERROR = "error"
    RUN_COMPLETED = "run_completed"
    RUN_CANCELLED = "run_cancelled"
    PROPOSAL_CREATED = "proposal_created"
    WORKFLOW_STARTED = "workflow_started"
    WORKFLOW_STEP_STARTED = "workflow_step_started"
    WORKFLOW_STEP_COMPLETED = "workflow_step_completed"
    WORKFLOW_WAITING_USER_CONFIRMATION = "workflow_waiting_user_confirmation"
    WORKFLOW_PAUSED = "workflow_paused"
    WORKFLOW_RESUMED = "workflow_resumed"
    WORKFLOW_CANCELLED = "workflow_cancelled"
    WORKFLOW_EXPIRED = "workflow_expired"
    WORKFLOW_FAILED = "workflow_failed"
    WORKFLOW_COMPLETED = "workflow_completed"


class InternalAiEvent(BaseModel):
    """一次 AI Run 中可持久化和恢复的内部事件。"""

    event_id: str = Field(default_factory=lambda: uuid4().hex)
    run_id: str
    sequence: int
    event_type: AiEventType
    occurred_at: datetime = Field(default_factory=lambda: datetime.now(UTC))
    payload: dict[str, Any] = Field(default_factory=dict)


class ErrorPayload(BaseModel):
    """内部错误事件载荷，只携带稳定错误码和安全提示。"""

    code: str
    message: str
