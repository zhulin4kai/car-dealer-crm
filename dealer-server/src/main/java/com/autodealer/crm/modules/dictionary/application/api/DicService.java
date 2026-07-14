package com.autodealer.crm.modules.dictionary.application.api;

import com.autodealer.crm.modules.dictionary.application.api.model.TDicType;
import com.autodealer.crm.modules.dictionary.application.api.model.TDicValue;
import com.autodealer.crm.modules.dictionary.application.api.query.DicQuery;
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

    boolean updateDicValue(Integer id, TDicValue dicValue);

    boolean deleteDicType(Integer id);

    boolean deleteDicValue(Integer id);

    List<TDicValue> getDicValuesByTypeId(Integer typeId);

    TDicType getDicTypeByCode(String typeCode);

    void evictDictionaryCaches();

    boolean deleteDicTypesByIds(List<Integer> ids);

    boolean deleteDicValuesByIds(List<Integer> ids);

    /**
     * 刷新字典类型缓存
     */
    void refreshTypeCache();

    /**
     * 刷新字典值缓存
     */
    void refreshValueCache();
}
