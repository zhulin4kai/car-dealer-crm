"""日志初始化入口，保持服务日志格式稳定。"""

from __future__ import annotations

import logging


def configure_logging() -> None:
    """配置基础日志格式，避免输出密钥、令牌和原始模型响应。"""

    logging.basicConfig(
        level=logging.INFO,
        format="%(asctime)s %(levelname)s %(name)s %(message)s",
    )
