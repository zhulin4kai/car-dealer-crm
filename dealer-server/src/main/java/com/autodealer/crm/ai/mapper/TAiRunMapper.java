package com.autodealer.crm.ai.mapper;

import com.autodealer.crm.ai.model.TAiRun;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TAiRunMapper {
    int insert(TAiRun record);

    TAiRun selectById(@Param("id") Long id);

    TAiRun selectByRunNo(@Param("runNo") String runNo);

    TAiRun selectOwnedByRunNo(@Param("runNo") String runNo, @Param("userId") Integer userId);

    TAiRun selectLatestByConversationId(@Param("conversationId") Long conversationId);

    List<TAiRun> selectByConversationId(@Param("conversationId") Long conversationId);

    Integer selectMaxTurnNoByConversationId(@Param("conversationId") Long conversationId);

    List<TAiRun> selectByUserId(@Param("userId") Integer userId,
                                @Param("offset") Integer offset,
                                @Param("limit") Integer limit);

    int updateStatus(@Param("id") Long id,
                     @Param("status") String status,
                     @Param("errorCode") String errorCode,
                     @Param("errorMessage") String errorMessage);

    int updateStatusIfNotTerminal(@Param("id") Long id,
                                  @Param("status") String status,
                                  @Param("errorCode") String errorCode,
                                  @Param("errorMessage") String errorMessage);

    int cancelIfCancellable(@Param("id") Long id,
                            @Param("errorCode") String errorCode,
                            @Param("errorMessage") String errorMessage);
}
