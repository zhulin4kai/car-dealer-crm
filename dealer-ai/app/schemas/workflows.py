"""工作流内部模型，不向 Spring Boot 暴露 LangGraph 类型。"""

from __future__ import annotations

from enum import StrEnum

from pydantic import BaseModel, Field


class WorkflowStatus(StrEnum):
    """受控工作流状态，必须与 Spring Boot 工作流状态保持一致。"""

    CREATED = "CREATED"
    RUNNING = "RUNNING"
    PAUSED = "PAUSED"
    WAITING_USER_CONFIRMATION = "WAITING_USER_CONFIRMATION"
    COMPLETED = "COMPLETED"
    FAILED = "FAILED"
    CANCELLED = "CANCELLED"
    EXPIRED = "EXPIRED"


class WorkflowStepStatus(StrEnum):
    """工作流步骤状态，供内部事件载荷使用。"""

    PENDING = "PENDING"
    RUNNING = "RUNNING"
    WAITING_USER_CONFIRMATION = "WAITING_USER_CONFIRMATION"
    COMPLETED = "COMPLETED"
    FAILED = "FAILED"
    CANCELLED = "CANCELLED"
    EXPIRED = "EXPIRED"


class WorkflowStepPlan(BaseModel):
    """固定工作流步骤计划，节点只能调用白名单工具或等待用户确认。"""

    step_no: int = Field(gt=0)
    step_type: str = Field(min_length=1, max_length=64)
    title: str = Field(min_length=1, max_length=128)
    tool_name: str | None = Field(default=None, max_length=128)
    proposal_type: str | None = Field(default=None, max_length=128)


class WorkflowPlan(BaseModel):
    """LangGraph 编排完成后转换出的可持久化工作流计划。"""

    workflow_type: str = Field(min_length=1, max_length=64)
    title: str = Field(min_length=1, max_length=128)
    steps: list[WorkflowStepPlan]
