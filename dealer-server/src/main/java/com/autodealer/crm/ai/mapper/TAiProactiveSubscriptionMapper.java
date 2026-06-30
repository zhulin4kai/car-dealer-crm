package com.autodealer.crm.ai.mapper;

import com.autodealer.crm.ai.model.TAiProactiveSubscription;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface TAiProactiveSubscriptionMapper {
    int insert(TAiProactiveSubscription record);

    TAiProactiveSubscription selectById(@Param("id") Long id);

    TAiProactiveSubscription selectOwnedBySubscriptionNo(@Param("subscriptionNo") String subscriptionNo,
                                                        @Param("userId") Integer userId);

    List<TAiProactiveSubscription> selectByUserId(@Param("userId") Integer userId);

    List<TAiProactiveSubscription> selectActiveDueByUserId(@Param("userId") Integer userId,
                                                           @Param("now") LocalDateTime now,
                                                           @Param("limit") Integer limit);

    int updateStatusIfCurrent(@Param("id") Long id,
                              @Param("expectedStatus") String expectedStatus,
                              @Param("status") String status,
                              @Param("editBy") Integer editBy);

    int updateTriggerTime(@Param("id") Long id,
                          @Param("lastTriggeredTime") LocalDateTime lastTriggeredTime,
                          @Param("nextTriggerTime") LocalDateTime nextTriggerTime,
                          @Param("editBy") Integer editBy);
}
