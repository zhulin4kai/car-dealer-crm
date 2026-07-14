package com.autodealer.crm.modules.dictionary.persistence.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.autodealer.crm.modules.dictionary.application.api.model.TDicValue;


@Mapper
public interface TDicValueMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(TDicValue record);

    int insertSelective(TDicValue record);

    TDicValue selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(TDicValue record);

    int updateByPrimaryKey(TDicValue record);
}
