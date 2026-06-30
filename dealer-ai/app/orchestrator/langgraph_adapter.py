"""LangGraph 内部编排适配器，只输出项目稳定 AI 事件。"""

from __future__ import annotations

from collections.abc import AsyncIterator
from typing import Any
from uuid import uuid4

from langgraph.graph import END, StateGraph

from app.orchestrator.base import AgentOrchestrator
from app.orchestrator.prompts import SYSTEM_PROMPT
from app.providers.base import ChatMessage, ProviderAdapter
from app.schemas.chat import ChatRunRequest
from app.schemas.events import AiEventType, InternalAiEvent
from app.schemas.proposals import ProposalType
from app.schemas.tools import ToolCallRequest, ToolSchema, validate_tool_arguments
from app.schemas.workflows import WorkflowStatus, WorkflowStepPlan, WorkflowStepStatus
from app.telemetry.run_events import EventSequence
from app.tools.client import ToolClient

BANNED_WORKFLOW_TOOLS = {
    "run_sql",
    "http_request",
    "file_write",
    "shell_exec",
    "approve_transaction",
    "settle_transaction",
    "record_payment",
    "create_invoice",
    "inventory_reserve",
    "inventory_release",
    "inventory_outbound",
    "delete_customer",
    "delete_transaction",
    "batch_delete",
}
DEFAULT_COMMUNICATION_METHOD = "PHONE"
DEFAULT_FOLLOW_TASK_PRIORITY = "NORMAL"
DEFAULT_FOLLOW_TASK_TYPE = "PHONE_FOLLOW_UP"


class LangGraphAgentOrchestrator(AgentOrchestrator):
    """LangGraph 内部图编排实现，外部只看稳定事件和工具结果。"""

    def __init__(
        self,
        provider: ProviderAdapter,
        tool_client: ToolClient | None = None,
    ) -> None:
        """创建固定节点图，并把自然语言生成交给真实 Provider。"""

        self._provider = provider
        self._tool_client = tool_client
        self._graph = _compile_fixed_graph()

    async def run(self, request: ChatRunRequest) -> AsyncIterator[InternalAiEvent]:
        """执行受控工作流，并把节点结果转换为内部 AI 事件。"""

        sequence = EventSequence(request.run_id)
        yield sequence.make(AiEventType.RUN_STARTED, {"status": WorkflowStatus.RUNNING.value})
        available_tools = _available_tools(request.tool_schemas)
        _reject_banned_tools(available_tools)
        self._graph.invoke({"run_id": request.run_id})

        steps = _workflow_steps(request, available_tools)
        if not steps:
            model_failed: list[bool] = []
            async for event in self._stream_model_message(sequence, request, [], model_failed):
                yield event
            yield sequence.make(
                AiEventType.RUN_COMPLETED,
                {
                    "status": (
                        WorkflowStatus.FAILED.value
                        if model_failed
                        else WorkflowStatus.COMPLETED.value
                    )
                },
            )
            return

        workflow_no = "WFA" + uuid4().hex
        workflow_type = _workflow_type(request)
        yield sequence.make(
            AiEventType.WORKFLOW_STARTED,
            {
                "workflowNo": workflow_no,
                "workflowType": workflow_type,
                "status": WorkflowStatus.RUNNING.value,
                "title": _workflow_title(workflow_type),
            },
        )

        waiting_for_confirmation = False
        workflow_failed = False
        tool_summaries: list[str] = []
        for step in steps:
            async for event in self._run_step(
                sequence, request, workflow_no, step, tool_summaries
            ):
                if event.event_type == AiEventType.WORKFLOW_WAITING_USER_CONFIRMATION:
                    waiting_for_confirmation = True
                if event.event_type == AiEventType.WORKFLOW_FAILED:
                    workflow_failed = True
                yield event
                if waiting_for_confirmation:
                    break
            if waiting_for_confirmation or workflow_failed:
                break

        if workflow_failed:
            yield sequence.make(AiEventType.RUN_COMPLETED, {"status": WorkflowStatus.FAILED.value})
            return

        model_failed: list[bool] = []
        async for event in self._stream_model_message(
            sequence, request, tool_summaries, model_failed
        ):
            yield event
        if model_failed:
            yield sequence.make(AiEventType.RUN_COMPLETED, {"status": WorkflowStatus.FAILED.value})
            return

        if waiting_for_confirmation:
            yield sequence.make(
                AiEventType.RUN_COMPLETED,
                {"status": WorkflowStatus.WAITING_USER_CONFIRMATION.value},
            )
            return

        yield sequence.make(
            AiEventType.WORKFLOW_COMPLETED,
            {"workflowNo": workflow_no, "status": WorkflowStatus.COMPLETED.value},
        )
        yield sequence.make(AiEventType.RUN_COMPLETED, {"status": WorkflowStatus.COMPLETED.value})

    async def _run_step(
        self,
        sequence: EventSequence,
        request: ChatRunRequest,
        workflow_no: str,
        step: WorkflowStepPlan,
        tool_summaries: list[str],
    ) -> AsyncIterator[InternalAiEvent]:
        """执行单个固定步骤，工具调用仍通过 Spring Boot Tool API。"""

        base_payload = {
            "workflowNo": workflow_no,
            "stepNo": step.step_no,
            "stepType": step.step_type,
            "title": step.title,
        }
        yield sequence.make(
            AiEventType.WORKFLOW_STEP_STARTED,
            base_payload | {"status": WorkflowStepStatus.RUNNING.value},
        )
        if step.tool_name and self._tool_client:
            arguments = _tool_arguments(step, request)
            yield sequence.make(AiEventType.TOOL_CALL_STARTED, {"toolName": step.tool_name})
            try:
                result = await self._tool_client.execute(
                    ToolCallRequest(
                        run_id=request.run_id,
                        tool_name=step.tool_name,
                        arguments=arguments,
                    )
                )
            except Exception:
                yield sequence.make(
                    AiEventType.ERROR,
                    {
                        "code": "AI_TOOL_EXECUTION_FAILED",
                        "message": "AI 工具调用失败，请稍后重试",
                    },
                )
                yield sequence.make(
                    AiEventType.WORKFLOW_STEP_COMPLETED,
                    base_payload
                    | {
                        "status": WorkflowStepStatus.FAILED.value,
                        "outputSummary": "工具调用失败",
                    },
                )
                yield sequence.make(
                    AiEventType.WORKFLOW_FAILED,
                    {
                        "workflowNo": workflow_no,
                        "status": WorkflowStatus.FAILED.value,
                    },
                )
                return
            yield sequence.make(
                AiEventType.TOOL_CALL_COMPLETED,
                {
                    "toolName": result.tool_name,
                    "outputSummary": result.summary,
                    "data": result.data,
                },
            )
            if result.summary:
                tool_summaries.append(result.summary)
            if step.proposal_type:
                yield sequence.make(AiEventType.PROPOSAL_CREATED, _proposal_payload(result.data))
                yield sequence.make(
                    AiEventType.WORKFLOW_WAITING_USER_CONFIRMATION,
                    base_payload
                    | {
                        "status": WorkflowStepStatus.WAITING_USER_CONFIRMATION.value,
                        "proposalType": step.proposal_type,
                    },
                )
                return
            yield sequence.make(
                AiEventType.WORKFLOW_STEP_COMPLETED,
                base_payload
                | {
                    "status": WorkflowStepStatus.COMPLETED.value,
                    "outputSummary": result.summary,
                },
            )
            return

        yield sequence.make(
            AiEventType.WORKFLOW_STEP_COMPLETED,
            base_payload
            | {
                "status": WorkflowStepStatus.COMPLETED.value,
                "outputSummary": "已完成工作流计划步骤",
            },
        )

    async def _stream_model_message(
        self,
        sequence: EventSequence,
        request: ChatRunRequest,
        tool_summaries: list[str],
        model_failed: list[bool],
    ) -> AsyncIterator[InternalAiEvent]:
        """把用户输入和工具摘要交给 Provider，输出真正的模型消息。"""

        messages = _model_messages(request)
        if tool_summaries:
            joined = "\n".join(f"- {summary}" for summary in tool_summaries)
            messages.append(
                ChatMessage(
                    role="system",
                    content=(
                        "以下是已通过 Spring Boot 权限和数据范围校验的工具摘要，"
                        "请只基于这些摘要给用户自然语言结论，不展示内部字段名：\n"
                        f"{joined}"
                    ),
                )
            )
        completed_text: list[str] = []
        try:
            async for chunk in self._provider.stream_chat(
                messages=messages,
                runtime_config=request.provider_runtime_config,
            ):
                if chunk.content_delta:
                    completed_text.append(chunk.content_delta)
                    yield sequence.make(
                        AiEventType.MESSAGE_DELTA,
                        {"content_delta": chunk.content_delta},
                    )
        except Exception:
            model_failed.append(True)
            yield sequence.make(
                AiEventType.ERROR,
                {
                    "code": "MODEL_PROVIDER_FAILED",
                    "message": "模型服务调用失败，请检查 Provider 配置或稍后重试",
                },
            )
            return

        yield sequence.make(
            AiEventType.MESSAGE_COMPLETED,
            {"content": "".join(completed_text)},
        )


def _compile_fixed_graph() -> Any:
    """构建固定 LangGraph 节点图，只作为内部执行骨架。"""

    graph = StateGraph(dict)
    graph.add_node("controlled_plan", lambda state: state)
    graph.set_entry_point("controlled_plan")
    graph.add_edge("controlled_plan", END)
    return graph.compile()


def _model_messages(request: ChatRunRequest) -> list[ChatMessage]:
    """按固定顺序组装模型上下文，避免 Run 被误当成完整会话。"""

    messages = [ChatMessage(role="system", content=SYSTEM_PROMPT)]
    if request.conversation_summary:
        messages.append(
            ChatMessage(
                role="system",
                content=(
                    "以下是 Spring Boot 生成的脱敏会话摘要，只用于理解多轮上下文，"
                    "不得把摘要中的内部编号、字段名或权限信息展示给用户：\n"
                    f"{request.conversation_summary}"
                ),
            )
        )
    for item in request.message_history[-8:]:
        messages.append(ChatMessage(role=item.role, content=item.content_summary))
    messages.append(ChatMessage(role="user", content=request.user_prompt))
    return messages


def _available_tools(tool_schemas: list[dict[str, Any]]) -> set[str]:
    """从 Spring Boot 下发的工具 Schema 中提取工具名。"""

    names: set[str] = set()
    for schema in tool_schemas:
        try:
            names.add(ToolSchema.model_validate(schema).name)
        except ValueError:
            continue
    return names


def _reject_banned_tools(available_tools: set[str]) -> None:
    """阻断高风险工具进入工作流节点图。"""

    banned = BANNED_WORKFLOW_TOOLS.intersection(available_tools)
    if banned:
        names = ", ".join(sorted(banned))
        raise ValueError(f"workflow tool is not allowed: {names}")


def _workflow_type(request: ChatRunRequest) -> str:
    """根据上下文选择固定工作流类型，不生成动态流程类型。"""

    if request.context and request.context.object_type.upper() in {"TRAN", "TRANSACTION"}:
        return "TRANSACTION_GAP_REVIEW"
    if _contains_any(request.user_prompt, {"库存", "预警"}):
        return "INVENTORY_RISK_REVIEW"
    if _contains_any(request.user_prompt, {"交易", "审批", "履约", "缺口"}):
        return "TRANSACTION_GAP_REVIEW"
    return "CUSTOMER_FOLLOW_UP"


def _workflow_title(workflow_type: str) -> str:
    """把工作流类型转换为用户可见标题。"""

    titles = {
        "CUSTOMER_FOLLOW_UP": "客户跟进辅助工作流",
        "TRANSACTION_GAP_REVIEW": "交易履约缺口工作流",
        "INVENTORY_RISK_REVIEW": "库存风险解释工作流",
    }
    return titles.get(workflow_type, "AI 受控工作流")


def _workflow_steps(request: ChatRunRequest, available_tools: set[str]) -> list[WorkflowStepPlan]:
    """按固定场景生成步骤，步骤只能引用白名单工具。"""

    workflow_type = _workflow_type(request)
    if workflow_type == "TRANSACTION_GAP_REVIEW":
        if _has_context(request, {"TRAN", "TRANSACTION"}):
            return [
                _step(
                    1,
                    "READ_TRANSACTION",
                    "查询交易详情",
                    "get_transaction_detail",
                    available_tools,
                ),
                _step(2, "EXPLAIN_GAP", "解释履约缺口", None, available_tools),
            ]
        if "list_pending_transaction_approvals" in available_tools:
            return [
                _step(
                    1,
                    "READ_TRANSACTION_APPROVALS",
                    "查询待处理交易",
                    "list_pending_transaction_approvals",
                    available_tools,
                ),
                _step(2, "EXPLAIN_GAP", "解释履约缺口", None, available_tools),
            ]
        return []
    if workflow_type == "INVENTORY_RISK_REVIEW":
        return [
            _step(1, "READ_INVENTORY", "查询库存预警", "get_inventory_alerts", available_tools),
            _step(2, "EXPLAIN_RISK", "解释库存风险", None, available_tools),
        ]
    if _has_context(request, {"CUSTOMER"}):
        return [
            _step(1, "READ_CUSTOMER", "查询客户档案", "get_customer_profile", available_tools),
            _step(
                2,
                "CREATE_COMMUNICATION_PROPOSAL",
                "生成沟通记录提议",
                ProposalType.CREATE_COMMUNICATION_RECORD.value,
                available_tools,
                ProposalType.CREATE_COMMUNICATION_RECORD.value,
            ),
            _step(
                3,
                "CREATE_FOLLOW_TASK_PROPOSAL",
                "生成跟进任务提议",
                ProposalType.CREATE_FOLLOW_TASK.value,
                available_tools,
                ProposalType.CREATE_FOLLOW_TASK.value,
            ),
        ]
    if (
        _contains_any(request.user_prompt, {"跟进", "任务", "今日"})
        and "list_my_followups" in available_tools
    ):
        return [
            _step(1, "READ_FOLLOWUPS", "查询跟进任务", "list_my_followups", available_tools),
            _step(2, "EXPLAIN_FOLLOWUPS", "解释跟进重点", None, available_tools),
        ]
    if (
        _contains_any(request.user_prompt, {"客户", "线索"})
        and "search_customers" in available_tools
    ):
        return [
            _step(1, "SEARCH_CUSTOMERS", "查询客户摘要", "search_customers", available_tools),
            _step(2, "EXPLAIN_CUSTOMERS", "解释客户重点", None, available_tools),
        ]
    return []


def _step(
    step_no: int,
    step_type: str,
    title: str,
    tool_name: str | None,
    available_tools: set[str],
    proposal_type: str | None = None,
) -> WorkflowStepPlan:
    """构造步骤计划，不可用工具会降级为说明步骤。"""

    safe_tool = tool_name if tool_name in available_tools else None
    return WorkflowStepPlan(
        step_no=step_no,
        step_type=step_type,
        title=title,
        tool_name=safe_tool,
        proposal_type=proposal_type if safe_tool else None,
    )


def _tool_arguments(step: WorkflowStepPlan, request: ChatRunRequest) -> dict[str, Any]:
    """为固定工具生成后端已支持的参数值。"""

    if step.tool_name == "get_customer_profile" and _has_context(request, {"CUSTOMER"}):
        return validate_tool_arguments(
            step.tool_name,
            {"customerId": _context_object_id(request)},
        )
    if step.tool_name == "get_transaction_detail" and _has_context(
        request, {"TRAN", "TRANSACTION"}
    ):
        return validate_tool_arguments(
            step.tool_name,
            {"tranId": _context_object_id(request)},
        )
    if step.tool_name == "get_inventory_alerts":
        return validate_tool_arguments(step.tool_name, {"page": 1, "size": 10})
    if step.tool_name == "list_my_followups":
        return validate_tool_arguments(step.tool_name, {"page": 1, "size": 10})
    if step.tool_name == "search_customers":
        return validate_tool_arguments(
            step.tool_name,
            {"keyword": _summary(request.user_prompt, ""), "page": 1, "size": 5},
        )
    if step.tool_name == "list_pending_transaction_approvals":
        return validate_tool_arguments(step.tool_name, {"page": 1, "size": 10})
    if step.tool_name == ProposalType.CREATE_COMMUNICATION_RECORD.value and _has_context(
        request, {"CUSTOMER"}
    ):
        return validate_tool_arguments(
            step.tool_name,
            {
                "relatedObjectType": "CUSTOMER",
                "relatedObjectId": _context_object_id(request),
                "communicationMethod": DEFAULT_COMMUNICATION_METHOD,
                "summary": request.user_prompt[:128] or "AI 业务助手建议创建沟通记录",
            },
        )
    if step.tool_name == ProposalType.CREATE_FOLLOW_TASK.value and _has_context(
        request, {"CUSTOMER"}
    ):
        return validate_tool_arguments(
            step.tool_name,
            {
                "title": request.user_prompt[:128] or "AI 跟进任务",
                "taskType": DEFAULT_FOLLOW_TASK_TYPE,
                "relatedObjectType": "CUSTOMER",
                "relatedObjectId": _context_object_id(request),
                "priority": DEFAULT_FOLLOW_TASK_PRIORITY,
                "dueTime": "2026-07-01T10:00:00",
            },
        )
    return {}


def _proposal_payload(data: Any) -> dict[str, Any]:
    """转换 Proposal 工具结果，避免输出内部原始对象。"""

    return data if isinstance(data, dict) else {"summary": str(data)}


def _has_context(request: ChatRunRequest, object_types: set[str]) -> bool:
    """判断当前请求是否携带指定类型的页面上下文。"""

    return request.context is not None and request.context.object_type.upper() in object_types


def _context_object_id(request: ChatRunRequest) -> int:
    """读取页面上下文对象 ID，调用方必须先确认上下文类型。"""

    if request.context is None:
        raise ValueError("context object id is required")
    return int(request.context.object_id)


def _contains_any(value: str, keywords: set[str]) -> bool:
    """执行简单中文意图匹配，只用于选择固定工作流入口。"""

    return any(keyword in value for keyword in keywords)


def _summary(value: str, fallback: str) -> str:
    """生成工具入参摘要，避免把长提示词直接作为查询词。"""

    text = value.strip()
    if not text:
        return fallback
    return text[:64]
