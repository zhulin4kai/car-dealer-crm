"""`dealer-ai` 运行配置和 Spring Boot 内部出站地址校验。"""

from __future__ import annotations

from functools import lru_cache
from typing import Any
from urllib.parse import urljoin, urlparse

from pydantic import AliasChoices, AnyHttpUrl, Field
from pydantic_settings import BaseSettings, SettingsConfigDict

from app.core.errors import ConfigurationError, UnsafeOutboundTargetError

DEFAULT_LOCAL_TOKEN = "dev-internal-token"
LOCAL_ENVIRONMENTS = {"local", "dev", "test", "smoke"}


class Settings(BaseSettings):
    """服务配置对象，只保存服务间通信和本地运行安全上限。"""

    model_config = SettingsConfigDict(
        env_prefix="DEALER_AI_",
        env_file=(".env", ".env.local"),
        extra="ignore",
        populate_by_name=True,
    )

    app_name: str = "dealer-ai"
    environment: str = Field(default="local", min_length=1)
    internal_token: str = Field(default=DEFAULT_LOCAL_TOKEN, min_length=8)
    spring_tool_base_url: AnyHttpUrl = "http://localhost:8089/internal/ai"
    spring_tool_token: str = Field(default=DEFAULT_LOCAL_TOKEN, min_length=8)
    request_timeout_seconds: float = Field(
        default=10.0,
        gt=0,
        le=60,
        validation_alias=AliasChoices(
            "DEALER_AI_REQUEST_TIMEOUT_SECONDS",
            "AI_PROVIDER_TIMEOUT_SECONDS",
        ),
    )
    max_runtime_timeout_seconds: int = Field(default=60, gt=0, le=60)
    max_runtime_output_tokens: int = Field(default=4096, gt=0, le=4096)

    def model_post_init(self, __context: Any) -> None:
        """完成配置后校验非本地环境服务令牌。"""

        self._ensure_service_tokens()

    def _ensure_service_tokens(self) -> None:
        """禁止非本地环境继续使用开发默认内部令牌。"""

        if self.environment.strip().lower() in LOCAL_ENVIRONMENTS:
            return
        if (
            self.internal_token == DEFAULT_LOCAL_TOKEN
            or self.spring_tool_token == DEFAULT_LOCAL_TOKEN
        ):
            raise ConfigurationError(
                "DEALER_AI_INTERNAL_TOKEN and DEALER_AI_SPRING_TOOL_TOKEN are required"
            )


def _origin(value: str) -> str:
    """提取 URL 源站，供白名单比较使用。"""

    parsed = urlparse(value)
    if not parsed.scheme or not parsed.netloc:
        raise ConfigurationError("URL must include scheme and host")
    return f"{parsed.scheme.lower()}://{parsed.netloc.lower()}"


def ensure_url_allowed(target_url: str, allowed_base_urls: list[str]) -> None:
    """校验目标 URL 的源站必须存在于配置白名单中。"""

    target_origin = _origin(target_url)
    allowed_origins = {_origin(url) for url in allowed_base_urls}
    if target_origin not in allowed_origins:
        raise UnsafeOutboundTargetError()


def join_allowed_url(base_url: str, path: str, allowed_base_urls: list[str]) -> str:
    """拼接内部访问地址，并在返回前重新执行白名单校验。"""

    target = urljoin(base_url.rstrip("/") + "/", path.lstrip("/"))
    ensure_url_allowed(target, allowed_base_urls)
    return target


@lru_cache
def get_settings() -> Settings:
    """返回进程级缓存配置，避免每次请求重复读取环境变量。"""

    return Settings()
