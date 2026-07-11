from __future__ import annotations

from collections.abc import AsyncIterator

from fastapi.testclient import TestClient

from app.api.routes.runs import get_orchestrator
from app.main import create_app
from app.orchestrator.langgraph_adapter import LangGraphAgentOrchestrator
from app.providers.base import ChatCompletionChunk, ChatMessage, ProviderAdapter
from app.schemas.chat import ProviderRuntimeConfig


class MockProvider(ProviderAdapter):
    async def stream_chat(
        self,
        *,
        messages: list[ChatMessage],
        runtime_config: ProviderRuntimeConfig,
    ) -> AsyncIterator[ChatCompletionChunk]:
        assert runtime_config.model_name == "mock-model"
        yield ChatCompletionChunk(content_delta="已完成")
        yield ChatCompletionChunk(is_final=True)


def test_health_route_available() -> None:
    app = create_app()
    client = TestClient(app)

    response = client.get("/health")

    assert response.status_code == 200
    assert response.json() == {"status": "UP", "service": "dealer-ai"}


def test_internal_run_requires_service_token() -> None:
    app = create_app()
    client = TestClient(app)

    response = client.post("/internal/runs", json={"run_id": "run-1", "user_prompt": "你好"})

    assert response.status_code == 401
    assert response.json()["code"] == "SERVICE_UNAUTHORIZED"


def test_internal_run_uses_mock_orchestrator_without_real_model() -> None:
    app = create_app()
    app.dependency_overrides[get_orchestrator] = lambda: LangGraphAgentOrchestrator(MockProvider())
    client = TestClient(app)

    response = client.post(
        "/internal/runs",
        headers={"X-Dealer-AI-Token": "dev-internal-token"},
        json={
            "run_id": "run-1",
            "user_prompt": "你好",
            "provider_runtime_config": {
                "provider_config_no": "AIPC-test",
                "provider_format": "OPENAI_COMPATIBLE",
                "base_url": "https://provider.test",
                "model_name": "mock-model",
                "api_key": "provider-key",
                "timeout_seconds": 15,
                "max_output_tokens": 64,
                "temperature": 0,
            },
        },
    )

    assert response.status_code == 200
    events = response.json()
    assert [event["event_type"] for event in events] == [
        "run_started",
        "message_delta",
        "message_completed",
        "run_completed",
    ]


def test_internal_stream_disables_buffering_and_orders_delta_before_completion() -> None:
    app = create_app()
    app.dependency_overrides[get_orchestrator] = lambda: LangGraphAgentOrchestrator(MockProvider())
    client = TestClient(app)

    with client.stream(
        "POST",
        "/internal/runs/stream",
        headers={"X-Dealer-AI-Token": "dev-internal-token"},
        json={
            "run_id": "run-stream-1",
            "user_prompt": "你好",
            "provider_runtime_config": {
                "provider_config_no": "AIPC-test",
                "provider_format": "OPENAI_COMPATIBLE",
                "base_url": "https://provider.test",
                "model_name": "mock-model",
                "api_key": "provider-key",
                "timeout_seconds": 15,
                "max_output_tokens": 64,
                "temperature": 0,
            },
        },
    ) as response:
        body = "".join(response.iter_text())
        assert response.status_code == 200
        assert response.headers["cache-control"] == "no-cache, no-transform"
        assert response.headers["x-accel-buffering"] == "no"
        assert response.headers["connection"] == "keep-alive"

    assert body.index("event: message_delta") < body.index("event: message_completed")
