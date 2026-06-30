"""AI 工具 Schema 和模型侧参数校验边界。"""

from __future__ import annotations

from enum import StrEnum
from typing import Any, Literal

from pydantic import BaseModel, ConfigDict, Field, model_validator

TRUSTED_CONTEXT_FIELDS = {
    "userId",
    "user_id",
    "role",
    "roles",
    "permissions",
    "dataScope",
    "data_scope",
    "orgScope",
    "organizationScope",
    "auditOperator",
    "operatorId",
}

CommunicationMethodValue = Literal["PHONE", "STORE_VISIT", "WECHAT", "SMS", "EMAIL", "OTHER"]
FollowRelatedObjectTypeValue = Literal["CLUE", "CUSTOMER", "OPPORTUNITY", "TEST_DRIVE", "ORDER"]
FollowTaskTypeValue = Literal[
    "FIRST_CONTACT",
    "PHONE_FOLLOW_UP",
    "STORE_INVITATION",
    "TEST_DRIVE_CONFIRM",
    "QUOTE_COMMUNICATION",
    "PRICE_NEGOTIATION",
    "CONTRACT_SIGN_REMINDER",
    "PAYMENT_REMINDER",
    "DELIVERY_CONFIRM",
    "POST_DELIVERY_FOLLOW_UP",
    "LONG_TERM_MAINTENANCE",
]
FollowTaskPriorityValue = Literal["LOW", "NORMAL", "HIGH", "URGENT"]


class ToolRiskLevel(StrEnum):
    """Spring Boot ToolRegistry 暴露给模型侧的风险等级视图。"""

    READONLY = "READONLY"
    LOW = "LOW"
    MEDIUM = "MEDIUM"
    HIGH = "HIGH"


class ToolSchema(BaseModel):
    """Spring Boot 下发的白名单工具 Schema 视图。"""

    name: str = Field(min_length=1, max_length=128)
    description: str = Field(min_length=1, max_length=1000)
    risk_level: ToolRiskLevel
    requires_confirmation: bool
    input_schema: dict[str, Any]


class ToolCallRequest(BaseModel):
    """`dealer-ai` 调用 Spring Boot 内部 Tool API 的请求。"""

    run_id: str = Field(min_length=1, max_length=64)
    tool_name: str = Field(min_length=1, max_length=128)
    arguments: dict[str, Any] = Field(default_factory=dict)

    @model_validator(mode="after")
    def reject_trusted_context_fields(self) -> ToolCallRequest:
        """拒绝模型侧伪造用户、权限、数据范围等可信上下文字段。"""

        forbidden = TRUSTED_CONTEXT_FIELDS.intersection(self.arguments.keys())
        if forbidden:
            names = ", ".join(sorted(forbidden))
            raise ValueError(f"trusted context fields are not allowed: {names}")
        return self


class ToolCallResult(BaseModel):
    """Spring Boot ToolRegistry 返回给编排器的工具执行摘要。"""

    tool_name: str
    summary: str
    data: Any = Field(default_factory=dict)


class StrictToolArguments(BaseModel):
    """所有工具参数的共同基类，禁止未声明字段穿透。"""

    model_config = ConfigDict(extra="forbid")


class PageArguments(StrictToolArguments):
    """分页工具参数，限制页大小避免大结果集拖垮业务线程。"""

    page: int = Field(default=1, ge=1)
    size: int = Field(default=10, ge=1, le=20)


class ListMyFollowupsArguments(PageArguments):
    """查询当前用户跟进任务的只读工具参数。"""

    status: str | None = Field(default=None, max_length=64)
    overdueOnly: bool | None = None
    keyword: str | None = Field(default=None, max_length=64)


class SearchCustomersArguments(PageArguments):
    """按关键词查询客户摘要的只读工具参数。"""

    keyword: str | None = Field(default=None, max_length=64)


class GetCustomerProfileArguments(StrictToolArguments):
    """查询单个客户档案的只读工具参数。"""

    customerId: int = Field(gt=0)


class ResolveVehicleProductArguments(StrictToolArguments):
    """按商品 ID 或 SKU 解析车辆商品的只读工具参数。"""

    productId: int | None = Field(default=None, gt=0)
    sku: str | None = Field(default=None, max_length=255)


class GetInventoryAlertsArguments(PageArguments):
    """查询库存预警的只读工具参数。"""

    sku: str | None = Field(default=None, max_length=255)
    name: str | None = Field(default=None, max_length=255)
    categoryId: int | None = Field(default=None, gt=0)


class GetTransactionDetailArguments(StrictToolArguments):
    """查询交易详情的只读工具参数。"""

    tranId: int = Field(gt=0)


class ListPendingTransactionApprovalsArguments(PageArguments):
    """查询待审批交易摘要的谨慎只读工具参数。"""

    pass


class CreateCommunicationRecordProposalArguments(StrictToolArguments):
    """创建沟通记录 Proposal 的模型侧参数边界。"""

    followTaskId: int | None = Field(default=None, gt=0)
    relatedObjectType: FollowRelatedObjectTypeValue
    relatedObjectId: int = Field(gt=0)
    communicationMethod: CommunicationMethodValue
    communicationTime: str | None = Field(default=None, max_length=32)
    summary: str = Field(min_length=1, max_length=500)
    customerFeedback: str | None = Field(default=None, max_length=500)
    nextAction: str | None = Field(default=None, max_length=500)
    nextFollowTime: str | None = Field(default=None, max_length=32)
    createNextTask: bool | None = None
    nextTaskType: FollowTaskTypeValue | None = None
    nextTaskTitle: str | None = Field(default=None, max_length=128)
    nextTaskPriority: FollowTaskPriorityValue | None = None
    nextTaskDueTime: str | None = Field(default=None, max_length=32)
    nextTaskRemindTime: str | None = Field(default=None, max_length=32)


class CreateFollowTaskProposalArguments(StrictToolArguments):
    """创建跟进任务 Proposal 的模型侧参数边界。"""

    title: str = Field(min_length=1, max_length=128)
    taskType: FollowTaskTypeValue
    relatedObjectType: FollowRelatedObjectTypeValue
    relatedObjectId: int = Field(gt=0)
    ownerId: int | None = Field(default=None, gt=0)
    priority: FollowTaskPriorityValue | None = None
    dueTime: str = Field(min_length=1, max_length=32)
    remindTime: str | None = Field(default=None, max_length=32)


TOOL_ARGUMENT_MODELS: dict[str, type[StrictToolArguments]] = {
    "list_my_followups": ListMyFollowupsArguments,
    "search_customers": SearchCustomersArguments,
    "get_customer_profile": GetCustomerProfileArguments,
    "resolve_vehicle_product": ResolveVehicleProductArguments,
    "get_inventory_alerts": GetInventoryAlertsArguments,
    "get_transaction_detail": GetTransactionDetailArguments,
    "list_pending_transaction_approvals": ListPendingTransactionApprovalsArguments,
    "create_communication_record_proposal": CreateCommunicationRecordProposalArguments,
    "create_follow_task_proposal": CreateFollowTaskProposalArguments,
}


def validate_tool_arguments(tool_name: str, arguments: dict[str, Any]) -> dict[str, Any]:
    """按工具名执行模型侧参数校验，最终权限和业务校验仍在 Spring Boot。"""

    model = TOOL_ARGUMENT_MODELS.get(tool_name)
    if model is None:
        raise ValueError(f"unsupported tool: {tool_name}")
    parsed = model.model_validate(arguments)
    return parsed.model_dump(exclude_none=True)
