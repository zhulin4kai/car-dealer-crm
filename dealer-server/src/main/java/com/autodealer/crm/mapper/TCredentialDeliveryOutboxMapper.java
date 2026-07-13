package com.autodealer.crm.mapper;

import com.autodealer.crm.model.TCredentialDeliveryOutbox;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface TCredentialDeliveryOutboxMapper {
    int insert(TCredentialDeliveryOutbox value);
    TCredentialDeliveryOutbox selectById(Long id);
    TCredentialDeliveryOutbox selectByMessageId(String messageId);
    List<TCredentialDeliveryOutbox> selectDue(@Param("now") LocalDateTime now,
                                              @Param("leaseExpiredBefore") LocalDateTime leaseExpiredBefore,
                                              @Param("limit") int limit);
    int claimByIdAndVersion(@Param("id") Long id,@Param("expectedVersion") Integer expectedVersion,
                            @Param("claimedAt") LocalDateTime claimedAt);
    int markDelivered(@Param("id") Long id,@Param("expectedVersion") Integer expectedVersion,
                      @Param("deliveredAt") LocalDateTime deliveredAt);
    int markRetry(@Param("id") Long id,@Param("expectedVersion") Integer expectedVersion,
                  @Param("nextAttemptAt") LocalDateTime nextAttemptAt,@Param("errorCode") String errorCode,
                  @Param("editTime") LocalDateTime editTime);
    int markFailed(@Param("id") Long id,@Param("expectedVersion") Integer expectedVersion,
                   @Param("failedAt") LocalDateTime failedAt,@Param("errorCode") String errorCode);
}
