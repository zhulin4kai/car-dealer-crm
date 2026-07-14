package com.autodealer.crm.modules.sales.activity.persistence.mapper;

import com.autodealer.crm.modules.identity.application.api.security.DataScope;
import com.autodealer.crm.modules.sales.activity.application.api.model.TActivityRemark;
import com.autodealer.crm.modules.sales.activity.application.api.query.ActivityRemarkQuery;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;


@Mapper
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
