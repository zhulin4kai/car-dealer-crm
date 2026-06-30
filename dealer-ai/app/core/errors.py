"""服务内部稳定错误类型，避免向 Spring Boot 泄露实现细节。"""

from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True)
class ServiceError(Exception):
    """可转换为 HTTP 响应的服务异常基类。"""

    code: str
    message: str
    status_code: int = 500


class ConfigurationError(ServiceError):
    """配置缺失或不安全时抛出的启动期错误。"""

    def __init__(self, message: str) -> None:
        super().__init__("CONFIGURATION_ERROR", message, 500)


class UnauthorizedServiceError(ServiceError):
    """内部服务令牌校验失败时抛出的认证错误。"""

    def __init__(self) -> None:
        super().__init__("SERVICE_UNAUTHORIZED", "service authentication failed", 401)


class ExternalServiceError(ServiceError):
    """模型 Provider 或 Spring Boot Tool API 调用失败时抛出的错误。"""

    def __init__(self, code: str, message: str, status_code: int = 502) -> None:
        super().__init__(code, message, status_code)


class UnsafeOutboundTargetError(ServiceError):
    """出站地址不在白名单中时抛出的安全边界错误。"""

    def __init__(self) -> None:
        super().__init__("UNSAFE_OUTBOUND_TARGET", "outbound target is not allowed", 403)
