package com.autodealer.crm.modules.audit.persistence.mapper;

import com.autodealer.crm.modules.audit.persistence.model.TOperationLog;
import com.autodealer.crm.modules.audit.application.api.query.AuditOperationLogQuery;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

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

}
