package com.autodealer.crm.ai.mapper;

import com.autodealer.crm.ai.model.TAiMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TAiMessageMapper {
    int insert(TAiMessage record);

    List<TAiMessage> selectByRunId(@Param("runId") Long runId);

    List<TAiMessage> selectByConversationId(@Param("conversationId") Long conversationId);

    List<TAiMessage> selectRecentVisibleByConversationId(@Param("conversationId") Long conversationId,
                                                         @Param("excludeRunId") Long excludeRunId,
                                                         @Param("limit") Integer limit);
}
