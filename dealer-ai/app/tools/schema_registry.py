"""模型侧工具 Schema 注册表，最终权限仍由 Spring Boot 决定。"""

from __future__ import annotations

from app.schemas.proposals import ProposalType
from app.schemas.tools import ToolRiskLevel, ToolSchema

READONLY_TOOL_NAMES = (
    "list_my_followups",
    "search_customers",
    "get_customer_profile",
    "resolve_vehicle_product",
    "get_inventory_alerts",
    "get_transaction_detail",
    "list_pending_transaction_approvals",
)


class ToolSchemaRegistry:
    """本地测试用工具 Schema 注册表，正式 Run 以 Spring Boot 下发为准。"""

    def list_schemas(self, *, include_proposals: bool = False) -> list[ToolSchema]:
        """列出本地 mock 工具 Schema，实际生产运行不以此为真源。"""

        schemas = [_readonly_tool_schema(name) for name in READONLY_TOOL_NAMES]
        if include_proposals:
            schemas.extend(
                [
                    _proposal_schema(ProposalType.CREATE_COMMUNICATION_RECORD.value),
                    _proposal_schema(ProposalType.CREATE_FOLLOW_TASK.value),
                ]
            )
        return schemas


def _readonly_tool_schema(name: str) -> ToolSchema:
    """构造只读工具 Schema，实际入参边界由 Spring Boot 再校验。"""

    return ToolSchema(
        name=name,
        description=f"Read-only CRM tool: {name}",
        risk_level=ToolRiskLevel.READONLY,
        requires_confirmation=False,
        input_schema={"type": "object", "additionalProperties": False},
    )


def _proposal_schema(name: str) -> ToolSchema:
    """构造低风险 Proposal 工具 Schema，确认执行由 Spring Boot 控制。"""

    return ToolSchema(
        name=name,
        description=f"Low-risk proposal tool: {name}",
        risk_level=ToolRiskLevel.LOW,
        requires_confirmation=True,
        input_schema={"type": "object", "additionalProperties": False},
    )
