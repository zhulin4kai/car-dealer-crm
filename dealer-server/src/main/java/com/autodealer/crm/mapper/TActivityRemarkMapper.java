package com.autodealer.crm.mapper;

import com.autodealer.crm.commons.DataScope;
import com.autodealer.crm.model.TActivityRemark;
import com.autodealer.crm.query.ActivityRemarkQuery;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface TActivityRemarkMapper {

    int deleteByPrimaryKey(Integer id);

    int insert(TActivityRemark record);

    int insertSelective(TActivityRemark record);

    TActivityRemark selectByPrimaryKey(Integer id);

    TActivityRemark selectScopedByPrimaryKey(@Param("id") Integer id,
                                             @Param("dataScopeUserId") Integer dataScopeUserId);

    int updateByPrimaryKeySelective(TActivityRemark record);

    int updateByPrimaryKey(TActivityRemark record);

    @DataScope(tableAlias = "tar", tableField = "create_by")
    List<TActivityRemark> selectActivityRemarkByPage(ActivityRemarkQuery activityRemarkQuery);
}
