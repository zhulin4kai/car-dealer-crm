package com.autodealer.crm.mapper;

import com.autodealer.crm.model.TLoginLog;
import com.autodealer.crm.query.AuditLoginLogQuery;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TLoginLogMapper {

    int insert(TLoginLog record);

    List<TLoginLog> selectByQuery(AuditLoginLogQuery query);

    TLoginLog selectById(Integer id);

    List<TLoginLog> selectForExport(@Param("query") AuditLoginLogQuery query,
                                    @Param("limit") Integer limit);
}
