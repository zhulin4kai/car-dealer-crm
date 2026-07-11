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

    TAiMessage selectOwnedUserMessageByNo(@Param("conversationId") Long conversationId,
                                          @Param("messageNo") String messageNo,
                                          @Param("userId") Integer userId);

    List<TAiMessage> selectActiveContextByConversationId(@Param("conversationId") Long conversationId);

    int supersedeIfVersionMatches(@Param("id") Long id,
                                  @Param("expectedVersion") Integer expectedVersion,
                                  @Param("editBy") Integer editBy);

    int withdrawIfVersionMatches(@Param("id") Long id,
                                 @Param("expectedVersion") Integer expectedVersion,
                                 @Param("withdrawnBy") Integer withdrawnBy);

    int excludeContextFromTurn(@Param("conversationId") Long conversationId,
                               @Param("turnNo") Integer turnNo,
                               @Param("editBy") Integer editBy);
}
