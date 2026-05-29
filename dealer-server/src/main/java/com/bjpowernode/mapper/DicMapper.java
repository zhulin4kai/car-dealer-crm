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
    
    // Delete operations for single dictionary type
    int deleteRemarksByDicTypeId(@Param("typeId") Integer typeId);
    int deleteDicValuesByTypeId(@Param("typeId") Integer typeId);
    int deleteDicType(@Param("id") Integer id);
    
    // Delete operations for single dictionary value
    int deleteRemarksByDicValueId(@Param("id") Integer id);
    int deleteDicValue(@Param("id") Integer id);
    
    // Delete operations for multiple dictionary types
    int deleteRemarksByDicTypeIds(@Param("ids") List<Integer> ids);
    int deleteDicValuesByTypeIds(@Param("ids") List<Integer> ids);
    int deleteDicTypesByIds(@Param("ids") List<Integer> ids);
    
    // Delete operations for multiple dictionary values
    int deleteRemarksByDicValueIds(@Param("ids") List<Integer> ids);
    int deleteDicValuesByIds(@Param("ids") List<Integer> ids);
    
    List<TDicValue> selectDicValuesByTypeId(@Param("typeId") Integer typeId);
    
    TDicType selectDicTypeByCode(@Param("typeCode") String typeCode);
    
    List<TDicType> selectDicTypesByIds(@Param("ids") List<Integer> ids);
    
    // Additional query methods to handle foreign key constraints
    List<Integer> selectDicValueIdsByTypeId(@Param("typeId") Integer typeId);
    List<Integer> selectDicValueIdsByTypeCode(@Param("typeCode") String typeCode);
    String selectTypeCodeById(@Param("id") Integer id);
    List<String> selectTypeCodesByIds(@Param("ids") List<Integer> ids);
} 