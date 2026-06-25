package com.autodealer.crm.mapper;

import com.autodealer.crm.commons.DataScope;
import com.autodealer.crm.model.TActivity;
import com.autodealer.crm.query.ActivityQuery;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface TActivityMapper {

    int deleteByPrimaryKey(Integer id);

    int insert(TActivity record);

    int insertSelective(TActivity record);

    TActivity selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(TActivity record);

    int updateByPrimaryKey(TActivity record);

    @DataScope(tableAlias = "ta", tableField = "owner_id")
    List<TActivity> selectActivityByPage(ActivityQuery query);

    TActivity selectDetailByPrimaryKey(@Param("id") Integer id,
                                       @Param("dataScopeUserId") Integer dataScopeUserId);

    List<TActivity> selecOngoingActivity(Integer dataScopeUserId);

    Integer selectByCount(@Param("dataScopeUserId") Integer dataScopeUserId);

    int batchDeleteByIds(List<Integer> ids);
}
