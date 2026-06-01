package com.autodealer.crm.mapper;

import com.autodealer.crm.commons.DataScope;
import com.autodealer.crm.model.TActivity;
import com.autodealer.crm.query.ActivityQuery;

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

    TActivity selectDetailByPrimaryKey(Integer id);

    List<TActivity> selecOngoingActivity();

    Integer selectByCount();

    int batchDeleteByIds(List<Integer> ids);
}