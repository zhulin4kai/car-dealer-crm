package com.autodealer.crm.modules.dictionary.persistence.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.autodealer.crm.modules.dictionary.application.api.model.TDicType;

import java.util.List;


@Mapper
public interface TDicTypeMapper {

    int deleteByPrimaryKey(Integer id);

    int insert(TDicType record);

    int insertSelective(TDicType record);

    TDicType selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(TDicType record);

    int updateByPrimaryKey(TDicType record);

    List<TDicType> selectByAll();
}
