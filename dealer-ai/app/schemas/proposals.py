"""低风险 Proposal 候选模型，实际保存和执行由 Spring Boot 控制。"""

from __future__ import annotations

from enum import StrEnum
from typing import Any

from pydantic import BaseModel, Field


class ProposalType(StrEnum):
    """允许模型提出的低风险 Proposal 类型。"""

    CREATE_COMMUNICATION_RECORD = "create_communication_record_proposal"
    CREATE_FOLLOW_TASK = "create_follow_task_proposal"


class ProposalCandidate(BaseModel):
    """模型侧 Proposal 候选参数，不能替代后端保存的规范化参数。"""

    proposal_type: ProposalType
    related_object_type: str = Field(min_length=1, max_length=64)
    related_object_id: str = Field(min_length=1, max_length=64)
    normalized_arguments: dict[str, Any] = Field(default_factory=dict)
    summary: str = Field(min_length=1, max_length=1000)
