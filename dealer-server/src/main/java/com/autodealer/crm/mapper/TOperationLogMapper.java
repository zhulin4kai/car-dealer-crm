package com.autodealer.crm.mapper;

import com.autodealer.crm.model.TOperationLog;
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
}
