package com.autodealer.crm.modules.sales.testdrive.persistence.mapper;

import com.autodealer.crm.modules.sales.testdrive.application.api.model.TTestDriveStatusHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TTestDriveStatusHistoryMapper {
    int insert(TTestDriveStatusHistory history);

    List<TTestDriveStatusHistory> selectByTestDriveId(@Param("testDriveId") Long testDriveId);
}
