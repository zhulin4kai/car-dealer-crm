package com.autodealer.crm.mapper;

import com.autodealer.crm.model.TOperationLog;
import com.autodealer.crm.query.AuditOperationLogQuery;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.time.LocalDateTime;
import com.autodealer.crm.dto.user.UserHistoryRows.OperationRow;

@Mapper
public interface TOperationLogMapper {
    int insert(TOperationLog record);

    List<TOperationLog> selectByModule(@Param("module") String module,
                                        @Param("offset") Integer offset,
                                        @Param("limit") Integer limit);

    Integer selectCountByModule(@Param("module") String module);

    List<TOperationLog> selectByUserId(@Param("userId") Integer userId,
                                        @Param("offset") Integer offset,
                                        @Param("limit") Integer limit);

    List<TOperationLog> selectByQuery(AuditOperationLogQuery query);

    TOperationLog selectById(Integer id);

    List<TOperationLog> selectForExport(@Param("query") AuditOperationLogQuery query,
                                        @Param("limit") Integer limit);

    List<OperationRow> selectUserHistoryRows(@Param("resourceId") String resourceId,
                                             @Param("actionCodes") List<String> actionCodes,
                                             @Param("startTime") LocalDateTime startTime,
                                             @Param("endTime") LocalDateTime endTime);
}
