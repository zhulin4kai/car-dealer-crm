package com.autodealer.crm.service;

import com.autodealer.crm.manager.RedisManager;
import com.autodealer.crm.mapper.DicMapper;
import com.autodealer.crm.model.TDicType;
import com.autodealer.crm.model.TDicValue;
import com.autodealer.crm.query.DicQuery;
import com.autodealer.crm.audit.OperationAuditRecorder;
import com.autodealer.crm.exception.BusinessException;
import com.autodealer.crm.result.CodeEnum;
import com.autodealer.crm.service.impl.DicServiceImpl;
import com.github.pagehelper.PageInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DicServiceImplTest {

    @InjectMocks
    private DicServiceImpl dicService;

    @Mock
    private DicMapper dicMapper;

    @Mock
    private RedisManager redisManager;

    @Mock
    private OperationAuditRecorder auditRecorder;

    @BeforeEach
    void setUp() {
        lenient().when(redisManager.deletePattern(anyString())).thenReturn(true);
    }

    @Test
    void testGetDicTypes() {
        DicQuery query = new DicQuery();
        query.setPage(1);
        query.setSize(10);
        TDicType type = new TDicType();
        type.setId(1);
        type.setTypeCode("industry");
        type.setTypeName("Industry");

        when(dicMapper.selectDicTypes(query)).thenReturn(Collections.singletonList(type));

        PageInfo<TDicType> result = dicService.getDicTypes(query);

        assertNotNull(result);
        assertEquals(1, result.getList().size());
        assertEquals("industry", result.getList().get(0).getTypeCode());
        verify(dicMapper).selectDicTypes(query);
    }

    @Test
    void testGetDicTypesEmpty() {
        DicQuery query = new DicQuery();
        query.setPage(1);
        query.setSize(10);
        when(dicMapper.selectDicTypes(query)).thenReturn(Collections.emptyList());

        PageInfo<TDicType> result = dicService.getDicTypes(query);

        assertNotNull(result);
        assertTrue(result.getList().isEmpty());
    }

    @Test
    void testGetDicValues() {
        DicQuery query = new DicQuery();
        query.setPage(1);
        query.setSize(10);
        TDicValue value = new TDicValue();
        value.setId(1);
        value.setTypeCode("industry");
        value.setTypeValue("IT");

        when(dicMapper.selectDicValues(query)).thenReturn(Collections.singletonList(value));

        PageInfo<TDicValue> result = dicService.getDicValues(query);

        assertNotNull(result);
        assertEquals(1, result.getList().size());
        assertEquals("IT", result.getList().get(0).getTypeValue());
    }

    @Test
    void testGetDicTypeById() {
        TDicType type = new TDicType();
        type.setId(1);
        type.setTypeCode("industry");

        when(redisManager.get("cdrm:dict:type:1")).thenReturn(null);
        when(dicMapper.selectDicTypeById(1)).thenReturn(type);

        TDicType result = dicService.getDicTypeById(1);

        assertNotNull(result);
        assertEquals(1, result.getId());
        assertEquals("industry", result.getTypeCode());
        verify(dicMapper).selectDicTypeById(1);
        verify(redisManager).set(eq("cdrm:dict:type:1"), eq(type), anyLong());
    }

    @Test
    void testGetDicTypeByIdFromCache() {
        TDicType type = new TDicType();
        type.setId(1);
        type.setTypeCode("industry");

        when(redisManager.get("cdrm:dict:type:1")).thenReturn(type);

        TDicType result = dicService.getDicTypeById(1);

        assertNotNull(result);
        assertEquals("industry", result.getTypeCode());
        verify(dicMapper, never()).selectDicTypeById(any());
    }

    @Test
    void testGetDicValueById() {
        TDicValue value = new TDicValue();
        value.setId(1);
        value.setTypeCode("industry");
        value.setTypeValue("IT");

        when(redisManager.get("cdrm:dict:value:1")).thenReturn(null);
        when(dicMapper.selectDicValueById(1)).thenReturn(value);

        TDicValue result = dicService.getDicValueById(1);

        assertNotNull(result);
        assertEquals(1, result.getId());
        assertEquals("IT", result.getTypeValue());
        verify(dicMapper).selectDicValueById(1);
    }

    @Test
    void testGetDicValueByIdFromCache() {
        TDicValue value = new TDicValue();
        value.setId(1);
        value.setTypeCode("industry");
        value.setTypeValue("IT");

        when(redisManager.get("cdrm:dict:value:1")).thenReturn(value);

        TDicValue result = dicService.getDicValueById(1);

        assertNotNull(result);
        assertEquals("IT", result.getTypeValue());
        verify(dicMapper, never()).selectDicValueById(any());
    }

    @Test
    void testAddDicType() {
        TDicType dicType = new TDicType();
        dicType.setTypeCode("industry");
        dicType.setTypeName("Industry");

        when(dicMapper.insertDicType(dicType)).thenReturn(1);

        boolean result = dicService.addDicType(dicType);

        assertTrue(result);
        verify(dicMapper).insertDicType(dicType);
        verify(redisManager).deletePattern("cdrm:dict:type:*");
        verify(redisManager).deletePattern("cdrm:dict:value:*");
    }

    @Test
    void testAddDicTypeFail() {
        TDicType dicType = new TDicType();
        dicType.setTypeCode("industry");
        dicType.setTypeName("Industry");

        when(dicMapper.insertDicType(dicType)).thenReturn(0);

        boolean result = dicService.addDicType(dicType);

        assertFalse(result);
        verify(redisManager, never()).deletePattern(anyString());
    }

    @Test
    void testAddDicValue() {
        TDicValue dicValue = new TDicValue();
        dicValue.setTypeCode("industry");
        dicValue.setTypeValue("IT");

        TDicType existingType = new TDicType();
        existingType.setId(1);
        existingType.setTypeCode("industry");

        when(dicMapper.selectDicTypeByCode("industry")).thenReturn(existingType);
        when(dicMapper.insertDicValue(dicValue)).thenReturn(1);

        boolean result = dicService.addDicValue(dicValue);

        assertTrue(result);
        verify(dicMapper).selectDicTypeByCode("industry");
        verify(dicMapper).insertDicValue(dicValue);
    }

    @Test
    void testAddDicValueTypeNotFound() {
        TDicValue dicValue = new TDicValue();
        dicValue.setTypeCode("nonexistent");
        dicValue.setTypeValue("IT");

        when(dicMapper.selectDicTypeByCode("nonexistent")).thenReturn(null);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> dicService.addDicValue(dicValue));
        assertTrue(ex.getMessage().contains("nonexistent"));
        verify(dicMapper, never()).insertDicValue(any());
    }

    @Test
    void testUpdateDicType() {
        TDicType oldType = new TDicType();
        oldType.setId(1);
        oldType.setTypeCode("industry");
        oldType.setTypeName("Old Name");

        TDicType newType = new TDicType();
        newType.setTypeCode("industry");
        newType.setTypeName("New Name");

        when(redisManager.get("cdrm:dict:type:1")).thenReturn(oldType);
        when(dicMapper.updateDicType(1, newType)).thenReturn(1);

        boolean result = dicService.updateDicType(1, newType);

        assertTrue(result);
        verify(dicMapper).updateDicType(1, newType);
        verify(redisManager, atLeast(1)).deletePattern("cdrm:dict:type:*");
    }

    @Test
    void updateDicType_shouldRejectStableCodeChange() {
        TDicType oldType = new TDicType();
        oldType.setId(1);
        oldType.setTypeCode("industry");
        oldType.setTypeName("Industry");

        TDicType newType = new TDicType();
        newType.setTypeCode("changed_industry");
        newType.setTypeName("Industry");

        when(redisManager.get("cdrm:dict:type:1")).thenReturn(oldType);

        BusinessException exception = assertThrows(BusinessException.class,
            () -> dicService.updateDicType(1, newType));

        assertEquals(CodeEnum.PARAM_ERROR, exception.getCodeEnum());
        verify(dicMapper, never()).updateDicType(anyInt(), any());
        verify(redisManager, never()).deletePattern(anyString());
    }

    @Test
    void testUpdateDicTypeNotFound() {
        TDicType newType = new TDicType();
        newType.setTypeCode("new_code");

        when(redisManager.get("cdrm:dict:type:999")).thenReturn(null);
        when(dicMapper.selectDicTypeById(999)).thenReturn(null);

        boolean result = dicService.updateDicType(999, newType);

        assertFalse(result);
        verify(dicMapper, never()).updateDicType(anyInt(), any());
    }

    @Test
    void testUpdateDicValue() {
        TDicValue oldValue = new TDicValue();
        oldValue.setId(1);
        oldValue.setTypeCode("industry");
        oldValue.setTypeValue("IT");
        oldValue.setValueCode("it");

        TDicValue newValue = new TDicValue();
        newValue.setTypeCode("industry");
        newValue.setTypeValue("IT Updated");
        newValue.setValueCode("it");

        when(redisManager.get("cdrm:dict:value:1")).thenReturn(oldValue);
        when(dicMapper.updateDicValue(any(TDicValue.class))).thenReturn(1);

        boolean result = dicService.updateDicValue(1, newValue);

        assertTrue(result);
        verify(dicMapper).updateDicValue(argThat(v -> v.getId().equals(1)));
    }

    @Test
    void updateDicValue_shouldRejectStableCodeChange() {
        TDicValue oldValue = new TDicValue();
        oldValue.setId(1);
        oldValue.setTypeCode("industry");
        oldValue.setTypeValue("IT");
        oldValue.setValueCode("it");

        TDicValue newValue = new TDicValue();
        newValue.setTypeCode("industry");
        newValue.setTypeValue("IT");
        newValue.setValueCode("changed_it");

        when(redisManager.get("cdrm:dict:value:1")).thenReturn(oldValue);

        BusinessException exception = assertThrows(BusinessException.class,
            () -> dicService.updateDicValue(1, newValue));

        assertEquals(CodeEnum.PARAM_ERROR, exception.getCodeEnum());
        verify(dicMapper, never()).updateDicValue(any());
        verify(redisManager, never()).deletePattern(anyString());
    }

    @Test
    void updateDicValue_shouldRejectTypeCodeChange() {
        TDicValue oldValue = new TDicValue();
        oldValue.setId(1);
        oldValue.setTypeCode("source");
        oldValue.setTypeValue("网络广告");
        oldValue.setValueCode("online_ad");

        TDicValue newValue = new TDicValue();
        newValue.setTypeCode("changed_source");
        newValue.setTypeValue("网络广告");
        newValue.setValueCode("online_ad");

        when(redisManager.get("cdrm:dict:value:1")).thenReturn(oldValue);

        BusinessException exception = assertThrows(BusinessException.class,
            () -> dicService.updateDicValue(1, newValue));

        assertEquals(CodeEnum.PARAM_ERROR, exception.getCodeEnum());
        verify(dicMapper, never()).updateDicValue(any());
    }

    @Test
    void testUpdateDicValueNotFound() {
        TDicValue newValue = new TDicValue();
        newValue.setTypeCode("industry");

        when(redisManager.get("cdrm:dict:value:999")).thenReturn(null);
        when(dicMapper.selectDicValueById(999)).thenReturn(null);

        boolean result = dicService.updateDicValue(999, newValue);

        assertFalse(result);
        verify(dicMapper, never()).updateDicValue(any());
    }

    @Test
    void testDeleteDicType() {
        TDicType dicType = new TDicType();
        dicType.setId(1);
        dicType.setTypeCode("industry");

        when(dicMapper.selectDicTypeById(1)).thenReturn(dicType);
        when(dicMapper.selectDicValueIdsByTypeCode("industry")).thenReturn(Arrays.asList(10, 20));
        when(dicMapper.selectRemarkCountByDicValueIds(Arrays.asList(10, 20))).thenReturn(0); // 没有业务引用
        when(dicMapper.deleteDicValuesByIds(Arrays.asList(10, 20))).thenReturn(2);
        when(dicMapper.deleteDicType(1)).thenReturn(1);

        boolean result = dicService.deleteDicType(1);

        assertTrue(result);
        verify(dicMapper).selectDicTypeById(1);
        verify(dicMapper).selectDicValueIdsByTypeCode("industry");
        verify(dicMapper).selectRemarkCountByDicValueIds(Arrays.asList(10, 20));
        verify(dicMapper).deleteDicValuesByIds(Arrays.asList(10, 20));
        verify(dicMapper).deleteDicType(1);
        verify(redisManager).deletePattern("cdrm:dict:type:*");
        verify(redisManager).deletePattern("cdrm:dict:value:*");
    }

    @Test
    void testDeleteDicType_hasBusinessReferences_shouldThrowException() {
        TDicType dicType = new TDicType();
        dicType.setId(1);
        dicType.setTypeCode("industry");

        when(dicMapper.selectDicTypeById(1)).thenReturn(dicType);
        when(dicMapper.selectDicValueIdsByTypeCode("industry")).thenReturn(Arrays.asList(10, 20));
        when(dicMapper.selectRemarkCountByDicValueIds(Arrays.asList(10, 20))).thenReturn(5); // 有5条业务引用

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> dicService.deleteDicType(1));

        assertEquals("该字典类型下有业务数据引用，无法删除", exception.getMessage());
        verify(dicMapper).selectDicTypeById(1);
        verify(dicMapper).selectDicValueIdsByTypeCode("industry");
        verify(dicMapper).selectRemarkCountByDicValueIds(Arrays.asList(10, 20));
        verify(dicMapper, never()).deleteDicValuesByIds(anyList());
        verify(dicMapper, never()).deleteDicType(anyInt());
    }

    @Test
    void testDeleteDicTypeNotFound() {
        when(dicMapper.selectDicTypeById(999)).thenReturn(null);

        boolean result = dicService.deleteDicType(999);

        assertFalse(result);
        verify(dicMapper, never()).deleteDicType(anyInt());
    }

    @Test
    void testDeleteDicTypeNoValues() {
        TDicType dicType = new TDicType();
        dicType.setId(1);
        dicType.setTypeCode("empty_type");

        when(dicMapper.selectDicTypeById(1)).thenReturn(dicType);
        when(dicMapper.selectDicValueIdsByTypeCode("empty_type")).thenReturn(Collections.emptyList());
        when(dicMapper.deleteDicType(1)).thenReturn(1);

        boolean result = dicService.deleteDicType(1);

        assertTrue(result);
        verify(dicMapper, never()).deleteRemarksByDicValueIds(anyList());
        verify(dicMapper, never()).deleteDicValuesByIds(anyList());
    }

    @Test
    void testDeleteDicValue() {
        TDicValue dicValue = new TDicValue();
        dicValue.setId(1);
        dicValue.setTypeCode("industry");

        when(dicMapper.selectDicValueById(1)).thenReturn(dicValue);
        when(dicMapper.selectRemarkCountByDicValueIds(Collections.singletonList(1))).thenReturn(0);
        when(dicMapper.deleteDicValue(1)).thenReturn(1);

        boolean result = dicService.deleteDicValue(1);

        assertTrue(result);
        verify(dicMapper, never()).deleteRemarksByDicValueId(anyInt());
        verify(dicMapper).deleteDicValue(1);
        verify(redisManager).deletePattern("cdrm:dict:type:*");
        verify(redisManager).deletePattern("cdrm:dict:value:*");
        verify(redisManager).deletePattern("cdrm:dict:values:type:*");
    }

    @Test
    void deleteDicValue_withBusinessReference_shouldThrowAndKeepHistory() {
        TDicValue dicValue = new TDicValue();
        dicValue.setId(1);
        dicValue.setTypeCode("noteWay");

        when(dicMapper.selectDicValueById(1)).thenReturn(dicValue);
        when(dicMapper.selectRemarkCountByDicValueIds(Collections.singletonList(1))).thenReturn(1);

        BusinessException exception = assertThrows(BusinessException.class,
            () -> dicService.deleteDicValue(1));

        assertEquals(CodeEnum.RESOURCE_IN_USE, exception.getCodeEnum());
        verify(dicMapper, never()).deleteRemarksByDicValueId(anyInt());
        verify(dicMapper, never()).deleteDicValue(anyInt());
        verify(redisManager, never()).deletePattern(anyString());
    }

    @Test
    void testDeleteDicValueNotFound() {
        when(dicMapper.selectDicValueById(999)).thenReturn(null);

        boolean result = dicService.deleteDicValue(999);

        assertFalse(result);
        verify(dicMapper, never()).deleteDicValue(anyInt());
    }

    @Test
    void testEvictDictionaryCaches() {
        dicService.evictDictionaryCaches();

        verify(redisManager).deletePattern("cdrm:dict:type:*");
        verify(redisManager).deletePattern("cdrm:dict:value:*");
        verify(redisManager).deletePattern("cdrm:dict:values:type:*");
    }

    @Test
    void testGetDicValuesByTypeId() {
        TDicValue value = new TDicValue();
        value.setId(1);
        value.setTypeCode("industry");
        value.setTypeValue("IT");
        List<TDicValue> values = Collections.singletonList(value);

        when(redisManager.get("cdrm:dict:values:type:1")).thenReturn(null);
        when(dicMapper.selectDicValuesByTypeId(1)).thenReturn(values);

        List<TDicValue> result = dicService.getDicValuesByTypeId(1);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("IT", result.get(0).getTypeValue());
        verify(dicMapper).selectDicValuesByTypeId(1);
    }

    @Test
    void testGetDicValuesByTypeIdFromCache() {
        TDicValue value = new TDicValue();
        value.setId(1);
        value.setTypeValue("IT");
        List<TDicValue> cachedValues = Collections.singletonList(value);

        when(redisManager.get("cdrm:dict:values:type:1")).thenReturn(cachedValues);

        List<TDicValue> result = dicService.getDicValuesByTypeId(1);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(dicMapper, never()).selectDicValuesByTypeId(any());
    }

    @Test
    void testGetDicTypeByCode() {
        TDicType type = new TDicType();
        type.setId(1);
        type.setTypeCode("industry");

        when(redisManager.get("cdrm:dict:type:code:industry")).thenReturn(null);
        when(dicMapper.selectDicTypeByCode("industry")).thenReturn(type);

        TDicType result = dicService.getDicTypeByCode("industry");

        assertNotNull(result);
        assertEquals("industry", result.getTypeCode());
        verify(dicMapper).selectDicTypeByCode("industry");
    }

    @Test
    void testGetDicTypeByCodeFromCache() {
        TDicType type = new TDicType();
        type.setId(1);
        type.setTypeCode("industry");

        when(redisManager.get("cdrm:dict:type:code:industry")).thenReturn(type);

        TDicType result = dicService.getDicTypeByCode("industry");

        assertNotNull(result);
        assertEquals("industry", result.getTypeCode());
        verify(dicMapper, never()).selectDicTypeByCode(anyString());
    }

    @Test
    void testDeleteDicTypesByIds() {
        List<Integer> ids = Arrays.asList(1, 2);
        when(dicMapper.selectTypeCodesByIds(ids)).thenReturn(Arrays.asList("type_a", "type_b"));
        when(dicMapper.selectDicValueIdsByTypeCodes(Arrays.asList("type_a", "type_b"))).thenReturn(Arrays.asList(10, 20));
        when(dicMapper.selectRemarkCountByDicValueIds(Arrays.asList(10, 20))).thenReturn(0);
        when(dicMapper.deleteDicValuesByIds(anyList())).thenReturn(1);
        when(dicMapper.deleteDicTypesByIds(ids)).thenReturn(2);

        boolean result = dicService.deleteDicTypesByIds(ids);

        assertTrue(result);
        verify(dicMapper).selectTypeCodesByIds(ids);
        verify(dicMapper).selectDicValueIdsByTypeCodes(Arrays.asList("type_a", "type_b"));
        verify(dicMapper, never()).deleteRemarksByDicValueIds(anyList());
        verify(dicMapper).deleteDicValuesByIds(Arrays.asList(10, 20));
        verify(dicMapper).deleteDicTypesByIds(ids);
        verify(redisManager).deletePattern("cdrm:dict:type:*");
        verify(redisManager).deletePattern("cdrm:dict:value:*");
        verify(redisManager).deletePattern("cdrm:dict:values:type:*");
    }

    @Test
    void testDeleteDicTypesByIdsEmpty() {
        boolean result = dicService.deleteDicTypesByIds(Collections.emptyList());

        assertFalse(result);
        verify(dicMapper, never()).selectTypeCodesByIds(anyList());
    }

    @Test
    void testDeleteDicTypesByIdsNull() {
        boolean result = dicService.deleteDicTypesByIds(null);

        assertFalse(result);
        verify(dicMapper, never()).selectTypeCodesByIds(anyList());
    }

    @Test
    void testDeleteDicValuesByIds() {
        List<Integer> ids = Arrays.asList(1, 2, 3);
        when(dicMapper.selectRemarkCountByDicValueIds(ids)).thenReturn(0);
        when(dicMapper.deleteDicValuesByIds(ids)).thenReturn(3);

        boolean result = dicService.deleteDicValuesByIds(ids);

        assertTrue(result);
        verify(dicMapper, never()).deleteRemarksByDicValueIds(anyList());
        verify(dicMapper).deleteDicValuesByIds(ids);
        verify(redisManager).deletePattern("cdrm:dict:type:*");
        verify(redisManager).deletePattern("cdrm:dict:value:*");
        verify(redisManager).deletePattern("cdrm:dict:values:type:*");
    }

    @Test
    void deleteDicValuesByIds_withBusinessReference_shouldThrowAndKeepHistory() {
        List<Integer> ids = Arrays.asList(1, 2, 3);
        when(dicMapper.selectRemarkCountByDicValueIds(ids)).thenReturn(2);

        BusinessException exception = assertThrows(BusinessException.class,
            () -> dicService.deleteDicValuesByIds(ids));

        assertEquals(CodeEnum.RESOURCE_IN_USE, exception.getCodeEnum());
        verify(dicMapper, never()).deleteRemarksByDicValueIds(anyList());
        verify(dicMapper, never()).deleteDicValuesByIds(anyList());
        verify(redisManager, never()).deletePattern(anyString());
    }

    @Test
    void testDeleteDicValuesByIdsEmpty() {
        boolean result = dicService.deleteDicValuesByIds(Collections.emptyList());

        assertFalse(result);
        verify(dicMapper, never()).deleteRemarksByDicValueIds(anyList());
    }

    @Test
    void testDeleteDicValuesByIdsNull() {
        boolean result = dicService.deleteDicValuesByIds(null);

        assertFalse(result);
        verify(dicMapper, never()).deleteRemarksByDicValueIds(anyList());
    }

    @Test
    void testRefreshTypeCache() {
        TDicType type1 = new TDicType();
        type1.setId(1);
        type1.setTypeCode("industry");
        TDicType type2 = new TDicType();
        type2.setId(2);
        type2.setTypeCode("source");

        when(dicMapper.selectDicTypes(any(DicQuery.class))).thenReturn(Arrays.asList(type1, type2));

        dicService.refreshTypeCache();

        verify(redisManager).deletePattern("cdrm:dict:type:*");
        verify(redisManager).set(eq("cdrm:dict:type:code:industry"), eq(type1), eq(24 * 60 * 60L));
        verify(redisManager).set(eq("cdrm:dict:type:code:source"), eq(type2), eq(24 * 60 * 60L));
    }

    @Test
    void testRefreshValueCache() {
        TDicValue value1 = new TDicValue();
        value1.setId(1);
        value1.setTypeCode("industry");
        TDicValue value2 = new TDicValue();
        value2.setId(2);
        value2.setTypeCode("source");

        when(dicMapper.selectDicValues(any(DicQuery.class))).thenReturn(Arrays.asList(value1, value2));

        dicService.refreshValueCache();

        verify(redisManager).deletePattern("cdrm:dict:value:*");
        verify(redisManager).set(eq("cdrm:dict:value:1"), eq(value1), eq(24 * 60 * 60L));
        verify(redisManager).set(eq("cdrm:dict:value:2"), eq(value2), eq(24 * 60 * 60L));
    }
}
