package com.autodealer.crm.mapper;

import com.autodealer.crm.model.TFollowTask;
import com.autodealer.crm.query.FollowTaskQuery;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface TFollowTaskMapper {
    int insert(TFollowTask record);

    TFollowTask selectById(@Param("id") Long id);

    TFollowTask selectByIdForUpdate(@Param("id") Long id);

    List<TFollowTask> selectByQuery(@Param("query") FollowTaskQuery query);

    int markOverdue(@Param("now") LocalDateTime now,
                    @Param("dataScopeUserId") Integer dataScopeUserId);

    int updateStatusIfCurrent(@Param("id") Long id,
                              @Param("expectedStatus") String expectedStatus,
                              @Param("targetStatus") String targetStatus,
                              @Param("updateTime") LocalDateTime updateTime,
                              @Param("updateBy") Integer updateBy);

    int postponeIfCurrent(@Param("id") Long id,
                          @Param("expectedStatus") String expectedStatus,
                          @Param("newDueTime") LocalDateTime newDueTime,
                          @Param("remindTime") LocalDateTime remindTime,
                          @Param("postponeReason") String postponeReason,
                          @Param("originalDueTime") LocalDateTime originalDueTime,
                          @Param("updateTime") LocalDateTime updateTime,
                          @Param("updateBy") Integer updateBy);

    int cancelIfCurrent(@Param("id") Long id,
                        @Param("expectedStatus") String expectedStatus,
                        @Param("cancelReason") String cancelReason,
                        @Param("updateTime") LocalDateTime updateTime,
                        @Param("updateBy") Integer updateBy);

    int completeIfCurrent(@Param("id") Long id,
                          @Param("expectedStatus") String expectedStatus,
                          @Param("result") String result,
                          @Param("communicationRecordId") Long communicationRecordId,
                          @Param("completedTime") LocalDateTime completedTime,
                          @Param("completedBy") Integer completedBy,
                          @Param("updateTime") LocalDateTime updateTime,
                          @Param("updateBy") Integer updateBy);
}
