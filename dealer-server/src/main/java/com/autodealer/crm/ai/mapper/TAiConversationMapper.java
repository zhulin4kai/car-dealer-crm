package com.autodealer.crm.ai.mapper;

import com.autodealer.crm.ai.model.TAiConversation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface TAiConversationMapper {
    int insert(TAiConversation record);

    TAiConversation selectById(@Param("id") Long id);

    TAiConversation selectOwnedByConversationNo(@Param("conversationNo") String conversationNo,
                                                @Param("userId") Integer userId);

    TAiConversation selectActiveByContext(@Param("userId") Integer userId,
                                          @Param("contextObjectType") String contextObjectType,
                                          @Param("contextObjectId") String contextObjectId);

    List<TAiConversation> selectByUserId(@Param("userId") Integer userId,
                                         @Param("includeArchived") boolean includeArchived);

    int updateTitle(@Param("id") Long id,
                    @Param("title") String title,
                    @Param("editTime") LocalDateTime editTime,
                    @Param("editBy") Integer editBy);

    int archive(@Param("id") Long id,
                @Param("editTime") LocalDateTime editTime,
                @Param("editBy") Integer editBy);

    int updateAfterRun(@Param("id") Long id,
                       @Param("lastRunNo") String lastRunNo,
                       @Param("summaryText") String summaryText,
                       @Param("lastMessageTime") LocalDateTime lastMessageTime,
                       @Param("editTime") LocalDateTime editTime,
                       @Param("editBy") Integer editBy);
}
