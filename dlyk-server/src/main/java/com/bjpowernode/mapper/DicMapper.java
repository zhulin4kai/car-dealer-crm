package com.bjpowernode.mapper;

import com.bjpowernode.model.TDicType;
import com.bjpowernode.model.TDicValue;
import com.bjpowernode.query.DicQuery;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface DicMapper {
    List<TDicType> selectDicTypes(DicQuery query);
    
    List<TDicValue> selectDicValues(DicQuery query);
    
    TDicType selectDicTypeById(@Param("id") Integer id);
    
    TDicValue selectDicValueById(@Param("id") Integer id);
    
    int insertDicType(TDicType dicType);
    
    int insertDicValue(TDicValue dicValue);
    
    int updateDicType(@Param("id") Integer id, @Param("dicType") TDicType dicType);
    
    int updateDicValue(TDicValue dicValue);
    
    int deleteDicType(@Param("id") Integer id);
    
    int deleteDicValue(@Param("id") Integer id);
    
    int deleteDicValuesByTypeId(@Param("typeId") Integer typeId);
    
    List<TDicValue> selectDicValuesByTypeId(@Param("typeId") Integer typeId);
    
    TDicType selectDicTypeByCode(@Param("typeCode") String typeCode);
    
    int deleteDicTypesByIds(@Param("ids") List<Integer> ids);
    
    List<TDicType> selectDicTypesByIds(@Param("ids") List<Integer> ids);
} 