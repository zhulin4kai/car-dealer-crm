from __future__ import annotations

from collections.abc import AsyncIterator

import pytest

from app.orchestrator.base import AgentOrchestrator
from app.orchestrator.langgraph_adapter import LangGraphAgentOrchestrator
from app.providers.base import ChatCompletionChunk, ChatMessage, ProviderAdapter
from app.schemas.chat import (
    AiContextObject,
    ChatRunRequest,
    MessageHistoryItem,
    ProviderRuntimeConfig,
)
from app.schemas.events import AiEventType
from app.schemas.tools import ToolCallRequest, ToolCallResult
from app.telemetry.run_events import collect_events
from app.tools.schema_registry import ToolSchemaRegistry


def runtime_config() -> ProviderRuntimeConfig:
    return ProviderRuntimeConfig(
        provider_config_no="AIPC-test",
        provider_format="OPENAI_COMPATIBLE",
        base_url="https://provider.test",
        model_name="mock-model",
        api_key="test-key",
        timeout_seconds=15,
        max_output_tokens=64,
        temperature=0,
    )


def run_request(**kwargs: object) -> ChatRunRequest:
    return ChatRunRequest(
        run_id=str(kwargs.pop("run_id", "run-1")),
        user_prompt=str(kwargs.pop("user_prompt", "总结客户")),
        provider_runtime_config=runtime_config(),
        **kwargs,
    )


class MockProvider(ProviderAdapter):
    async def stream_chat(
        self,
        *,
        messages: list[ChatMessage],
        runtime_config: ProviderRuntimeConfig,
    ) -> AsyncIterator[ChatCompletionChunk]:
        assert runtime_config.model_name == "mock-model"
        assert messages[0].role == "system"
        yield ChatCompletionChunk(content_delta="客户")
        yield ChatCompletionChunk(content_delta="摘要")
        yield ChatCompletionChunk(is_final=True)


class CapturingProvider(MockProvider):
    def __init__(self) -> None:
        self.messages: list[ChatMessage] = []

    async def stream_chat(
        self,
        *,
        messages: list[ChatMessage],
        runtime_config: ProviderRuntimeConfig,
    ) -> AsyncIterator[ChatCompletionChunk]:
        self.messages = messages
        async for chunk in super().stream_chat(messages=messages, runtime_config=runtime_config):
            yield chunk


class MockToolClient:
    def __init__(self, result_by_tool: dict[str, ToolCallResult]) -> None:
        self.result_by_tool = result_by_tool
        self.calls: list[ToolCallRequest] = []

    async def execute(self, request: ToolCallRequest) -> ToolCallResult:
        self.calls.append(request)
        return self.result_by_tool[request.tool_name]


class FailingToolClient:
    def __init__(self) -> None:
        self.calls: list[ToolCallRequest] = []

    async def execute(self, request: ToolCallRequest) -> ToolCallResult:
        self.calls.append(request)
        raise RuntimeError("tool failed")


def langgraph_orchestrator(tool_client: object | None = None) -> LangGraphAgentOrchestrator:
    return LangGraphAgentOrchestrator(
        MockProvider(),
        tool_client,  # type: ignore[arg-type]
    )


@pytest.mark.asyncio
async def test_langgraph_orchestrator_streams_expected_events_without_real_model() -> None:
    orchestrator = langgraph_orchestrator()
    events = await collect_events(orchestrator.run(run_request(user_prompt="你好")))

    assert [event.event_type for event in events] == [
        AiEventType.RUN_STARTED,
        AiEventType.MESSAGE_DELTA,
        AiEventType.MESSAGE_DELTA,
        AiEventType.MESSAGE_COMPLETED,
        AiEventType.RUN_COMPLETED,
    ]
    assert events[1].payload["content_delta"] == "客户"
    assert events[-2].payload["content"] == "客户摘要"


@pytest.mark.asyncio
async def test_langgraph_orchestrator_includes_conversation_summary_and_history() -> None:
    provider = CapturingProvider()
    orchestrator = LangGraphAgentOrchestrator(provider)

    await collect_events(
        orchestrator.run(
            run_request(
                user_prompt="刚才那笔交易下一步怎么办？",
                conversation_summary="用户上一轮询问了待审批交易。",
                message_history=[
                    MessageHistoryItem(role="user", content_summary="总结待处理交易"),
                    MessageHistoryItem(role="assistant", content_summary="当前有 1 笔待审批交易。"),
                ],
            )
        )
    )

    assert provider.messages[0].role == "system"
    assert "用户上一轮询问了待审批交易" in provider.messages[1].content
    assert provider.messages[2] == ChatMessage(role="user", content="总结待处理交易")
    assert provider.messages[3] == ChatMessage(role="assistant", content="当前有 1 笔待审批交易。")
    assert provider.messages[4] == ChatMessage(role="user", content="刚才那笔交易下一步怎么办？")


@pytest.mark.asyncio
async def test_langgraph_orchestrator_emits_workflow_and_tool_events() -> None:
    tool_client = MockToolClient(
        {
            "get_inventory_alerts": ToolCallResult(
                tool_name="get_inventory_alerts",
                summary="库存预警 1 条",
                data={"items": [{"sku": "AUDI-Q5L", "stock": 0}]},
            )
        }
    )
    orchestrator = langgraph_orchestrator(tool_client)

    events = await collect_events(
        orchestrator.run(
            run_request(
                user_prompt="查看库存风险",
                tool_schemas=[
                    schema.model_dump() for schema in ToolSchemaRegistry().list_schemas()
                ],
            )
        )
    )

    event_types = [event.event_type for event in events]
    assert AiEventType.WORKFLOW_STARTED in event_types
    assert AiEventType.WORKFLOW_STEP_STARTED in event_types
    assert AiEventType.TOOL_CALL_STARTED in event_types
    assert AiEventType.TOOL_CALL_COMPLETED in event_types
    tool_completed = next(
        event for event in events if event.event_type == AiEventType.TOOL_CALL_COMPLETED
    )
    assert tool_completed.payload["data"] == {"items": [{"sku": "AUDI-Q5L", "stock": 0}]}
    assert AiEventType.WORKFLOW_COMPLETED in event_types
    assert tool_client.calls[0].tool_name == "get_inventory_alerts"
    assert tool_client.calls[0].arguments == {"page": 1, "size": 10}


@pytest.mark.asyncio
async def test_langgraph_orchestrator_waits_after_low_risk_proposal() -> None:
    tool_client = MockToolClient(
        {
            "get_customer_profile": ToolCallResult(
                tool_name="get_customer_profile",
                summary="客户档案摘要",
                data={"customerId": 12},
            ),
            "create_communication_record_proposal": ToolCallResult(
                tool_name="create_communication_record_proposal",
                summary="已生成沟通记录提议",
                data={
                    "proposalId": 11,
                    "proposalType": "create_communication_record_proposal",
                    "impactSummary": "确认后将创建沟通记录",
                },
            ),
        }
    )
    orchestrator = langgraph_orchestrator(tool_client)

    events = await collect_events(
        orchestrator.run(
            run_request(
                user_prompt="给这个客户创建沟通记录",
                context=AiContextObject(object_type="CUSTOMER", object_id="12"),
                allow_proposals=True,
                tool_schemas=[
                    schema.model_dump()
                    for schema in ToolSchemaRegistry().list_schemas(include_proposals=True)
                ],
            )
        )
    )

    assert [call.tool_name for call in tool_client.calls] == [
        "get_customer_profile",
        "create_communication_record_proposal",
    ]
    proposal_call = tool_client.calls[1]
    assert proposal_call.arguments["communicationMethod"] == "PHONE"
    assert proposal_call.arguments["relatedObjectType"] == "CUSTOMER"
    assert AiEventType.PROPOSAL_CREATED in [event.event_type for event in events]
    assert events[-1].payload["status"] == "WAITING_USER_CONFIRMATION"


@pytest.mark.asyncio
async def test_langgraph_orchestrator_tool_failure_returns_safe_error_event() -> None:
    tool_client = FailingToolClient()
    orchestrator = langgraph_orchestrator(tool_client)

    events = await collect_events(
        orchestrator.run(
            run_request(
                user_prompt="查看今日跟进任务",
                tool_schemas=[
                    schema.model_dump() for schema in ToolSchemaRegistry().list_schemas()
                ],
            )
        )
    )

    assert tool_client.calls[0].tool_name == "list_my_followups"
    event_types = [event.event_type for event in events]
    assert AiEventType.ERROR in event_types
    assert AiEventType.WORKFLOW_FAILED in event_types
    assert events[-1].payload["status"] == "FAILED"


@pytest.mark.asyncio
async def test_langgraph_orchestrator_rejects_dangerous_tools() -> None:
    orchestrator = langgraph_orchestrator()

    with pytest.raises(ValueError, match="workflow tool is not allowed"):
        await collect_events(
            orchestrator.run(
                run_request(
                    user_prompt="执行 SQL",
                    tool_schemas=[
                        {
                            "name": "run_sql",
                            "description": "危险工具",
                            "risk_level": "HIGH",
                            "requires_confirmation": True,
                            "input_schema": {},
                        }
                    ],
                )
            )
        )


def test_langgraph_type_does_not_leak_to_public_orchestrator_contract() -> None:
    annotations = AgentOrchestrator.run.__annotations__

    assert "LangGraph" not in repr(annotations)
