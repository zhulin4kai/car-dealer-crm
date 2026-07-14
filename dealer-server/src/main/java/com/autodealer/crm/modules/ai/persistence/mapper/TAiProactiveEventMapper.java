package com.autodealer.crm.modules.ai.persistence.mapper;

import com.autodealer.crm.modules.ai.persistence.model.TAiProactiveEvent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface TAiProactiveEventMapper {
    int insert(TAiProactiveEvent record);

    TAiProactiveEvent selectOwnedByEventNo(@Param("eventNo") String eventNo,
                                           @Param("userId") Integer userId);

    List<TAiProactiveEvent> selectByUserId(@Param("userId") Integer userId,
                                           @Param("offset") Integer offset,
                                           @Param("limit") Integer limit);

    List<TAiProactiveEvent> selectBySubscriptionId(@Param("subscriptionId") Long subscriptionId);

    int countBySubscriptionAfter(@Param("subscriptionId") Long subscriptionId,
                                 @Param("since") LocalDateTime since);

    TAiProactiveEvent selectDuplicateAfter(@Param("subscriptionId") Long subscriptionId,
                                           @Param("objectType") String objectType,
                                           @Param("objectId") String objectId,
                                           @Param("since") LocalDateTime since);
}
