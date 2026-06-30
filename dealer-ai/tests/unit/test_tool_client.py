from __future__ import annotations

import json

import pytest
import respx
from httpx import Response

from app.core.config import Settings
from app.schemas.tools import ToolCallRequest, validate_tool_arguments
from app.tools.client import ToolClient


@pytest.mark.asyncio
@respx.mock
async def test_tool_client_calls_only_configured_spring_tool_api() -> None:
    settings = Settings(
        internal_token="internal-token",
        spring_tool_base_url="http://spring.test/internal/ai",
        spring_tool_token="tool-token",
    )
    route = respx.post("http://spring.test/internal/ai/tools/search_customers/execute").mock(
        return_value=Response(
            200,
            json={
                "data": {
                    "toolName": "search_customers",
                    "outputSummary": "2 customers",
                    "data": {"items": []},
                }
            },
        )
    )

    result = await ToolClient(settings).execute(
        ToolCallRequest(run_id="run-1", tool_name="search_customers", arguments={"keyword": "王"})
    )

    assert route.called
    request = route.calls.last.request
    assert request.headers["X-Dealer-AI-Tool-Token"] == "tool-token"
    assert json.loads(request.content) == {"runNo": "run-1", "arguments": {"keyword": "王"}}
    assert result.tool_name == "search_customers"
    assert result.summary == "2 customers"


@pytest.mark.asyncio
@respx.mock
async def test_tool_client_sends_supported_proposal_arguments() -> None:
    settings = Settings(
        internal_token="internal-token",
        spring_tool_base_url="http://spring.test/internal/ai",
        spring_tool_token="tool-token",
    )
    route = respx.post(
        "http://spring.test/internal/ai/tools/create_follow_task_proposal/execute"
    ).mock(
        return_value=Response(
            200,
            json={
                "data": {
                    "toolName": "create_follow_task_proposal",
                    "outputSummary": "proposal created",
                    "data": {"proposalId": 8},
                }
            },
        )
    )

    request = ToolCallRequest(
        run_id="run-1",
        tool_name="create_follow_task_proposal",
        arguments={
            "title": "电话跟进客户",
            "taskType": "PHONE_FOLLOW_UP",
            "relatedObjectType": "CUSTOMER",
            "relatedObjectId": 12,
            "priority": "NORMAL",
            "dueTime": "2026-07-01T10:00:00",
        },
    )

    await ToolClient(settings).execute(request)

    assert route.called
    content = json.loads(route.calls.last.request.content)
    assert content["arguments"]["taskType"] == "PHONE_FOLLOW_UP"
    assert content["arguments"]["relatedObjectType"] == "CUSTOMER"
    assert "userId" not in content["arguments"]


@pytest.mark.asyncio
async def test_tool_client_disables_env_proxy_for_internal_tool_api(
    monkeypatch: pytest.MonkeyPatch,
) -> None:
    captured: dict[str, object] = {}

    class FakeResponse:
        def raise_for_status(self) -> None:
            return None

        def json(self) -> dict[str, object]:
            return {
                "data": {
                    "toolName": "get_inventory_alerts",
                    "outputSummary": "库存预警 0 条",
                    "data": {"items": []},
                }
            }

    class FakeClient:
        def __init__(self, **kwargs: object) -> None:
            captured.update(kwargs)

        async def __aenter__(self) -> FakeClient:
            return self

        async def __aexit__(self, *_: object) -> None:
            return None

        async def post(
            self,
            endpoint: str,
            *,
            json: dict[str, object],
            headers: dict[str, str],
        ) -> FakeResponse:
            captured["endpoint"] = endpoint
            captured["json"] = json
            captured["headers"] = headers
            return FakeResponse()

    monkeypatch.setattr("app.tools.client.httpx.AsyncClient", FakeClient)
    settings = Settings(
        internal_token="internal-token",
        spring_tool_base_url="http://localhost:8089/internal/ai",
        spring_tool_token="tool-token",
    )

    await ToolClient(settings).execute(
        ToolCallRequest(run_id="run-1", tool_name="get_inventory_alerts", arguments={})
    )

    assert captured["trust_env"] is False
    assert captured["endpoint"] == "http://localhost:8089/internal/ai/tools/get_inventory_alerts/execute"


def test_tool_call_request_rejects_trusted_context_fields() -> None:
    with pytest.raises(ValueError):
        ToolCallRequest(
            run_id="run-1",
            tool_name="search_customers",
            arguments={"keyword": "王", "userId": 1},
        )


def test_proposal_argument_validation_rejects_unsupported_business_values() -> None:
    with pytest.raises(ValueError):
        validate_tool_arguments(
            "create_communication_record_proposal",
            {
                "relatedObjectType": "CUSTOMER",
                "relatedObjectId": 12,
                "communicationMethod": "UNSUPPORTED_METHOD",
                "summary": "沟通记录",
            },
        )

    with pytest.raises(ValueError):
        validate_tool_arguments(
            "create_follow_task_proposal",
            {
                "title": "跟进任务",
                "taskType": "UNSUPPORTED_TASK",
                "relatedObjectType": "CUSTOMER",
                "relatedObjectId": 12,
                "dueTime": "2026-07-01T10:00:00",
            },
        )
