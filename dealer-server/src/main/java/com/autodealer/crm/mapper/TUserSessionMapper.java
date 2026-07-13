package com.autodealer.crm.mapper;

import com.autodealer.crm.model.TUserSession;
import org.apache.ibatis.annotations.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface TUserSessionMapper {
    int insert(TUserSession session);
    TUserSession selectBySessionId(String sessionId);
    List<TUserSession> selectActiveByUserId(@Param("userId") Integer userId,
                                             @Param("now") LocalDateTime now);
    List<String> selectAllActiveSessionIdsByUserId(@Param("userId") Integer userId,
                                                    @Param("now") LocalDateTime now);
    int touchBySessionIdAndVersion(@Param("sessionId") String sessionId,
                                   @Param("expectedVersion") Integer expectedVersion,
                                   @Param("lastActivityTime") LocalDateTime lastActivityTime,
                                   @Param("idleExpiresAt") LocalDateTime idleExpiresAt);
    int revokeOne(@Param("sessionId") String sessionId, @Param("userId") Integer userId,
                  @Param("revokedAt") LocalDateTime revokedAt, @Param("revokedBy") Integer revokedBy,
                  @Param("reason") String reason, @Param("revokeType") String revokeType);
    int revokeOthers(@Param("userId") Integer userId, @Param("excludedSessionId") String excludedSessionId,
                     @Param("revokedAt") LocalDateTime revokedAt, @Param("revokedBy") Integer revokedBy,
                     @Param("reason") String reason, @Param("revokeType") String revokeType);
    int revokeAll(@Param("userId") Integer userId, @Param("revokedAt") LocalDateTime revokedAt,
                  @Param("revokedBy") Integer revokedBy, @Param("reason") String reason,
                  @Param("revokeType") String revokeType);
    int revokeExpired(@Param("now") LocalDateTime now);
    int deleteRetainedBefore(@Param("retainedBefore") LocalDateTime retainedBefore);
}
