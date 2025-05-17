package com.bjpowernode.service;

import com.bjpowernode.model.TDicType;
import com.bjpowernode.model.TDicValue;
import com.bjpowernode.query.DicQuery;
import com.github.pagehelper.PageInfo;

import java.util.List;

public interface DicService {
    PageInfo<TDicType> getDicTypes(DicQuery query);
    
    PageInfo<TDicValue> getDicValues(DicQuery query);
    
    TDicType getDicTypeById(Integer id);
    
    TDicValue getDicValueById(Integer id);
    
    boolean addDicType(TDicType dicType);
    
    boolean addDicValue(TDicValue dicValue);
    
    boolean updateDicType(Integer id, TDicType dicType);
    
    boolean updateDicValue(TDicValue dicValue);
    
    boolean deleteDicType(Integer id);
    
    boolean deleteDicValue(Integer id);
    
    List<TDicValue> getDicValuesByTypeId(Integer typeId);
    
    TDicType getDicTypeByCode(String typeCode);
    
    void clearCache(String pattern);
    
    boolean deleteDicTypesByIds(List<Integer> ids);
} 