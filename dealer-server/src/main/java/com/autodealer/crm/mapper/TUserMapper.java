package com.autodealer.crm.mapper;

import com.autodealer.crm.commons.DataScope;
import com.autodealer.crm.model.TUser;
import com.autodealer.crm.query.BaseQuery;

import java.util.List;

public interface TUserMapper {

    int deleteByPrimaryKey(Integer id);

    int insert(TUser record);

    int insertSelective(TUser record);

    TUser selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(TUser record);

    int updateByPrimaryKey(TUser record);

    TUser selectByLoginAct(String username);

    @DataScope(tableAlias = "tu", tableField = "id")
    List<TUser> selectUserByPage(BaseQuery query);

    TUser selectAuthUserById(Integer id);

    int deleteByIds(List<Integer> ids);

    List<TUser> selectByOwner();
}