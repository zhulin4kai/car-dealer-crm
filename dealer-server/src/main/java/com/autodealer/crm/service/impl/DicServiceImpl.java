package com.autodealer.crm.service.impl;

import com.autodealer.crm.audit.AuditActionEnum;
import com.autodealer.crm.audit.OperationAuditRecorder;
import com.autodealer.crm.config.security.CurrentUserProvider;
import com.autodealer.crm.constant.RedisKeys;
import com.autodealer.crm.manager.RedisManager;
import com.autodealer.crm.mapper.DicMapper;
import com.autodealer.crm.model.TDicType;
import com.autodealer.crm.model.TDicValue;
import com.autodealer.crm.query.DicQuery;
import com.autodealer.crm.service.DicService;
import com.autodealer.crm.util.CacheUtils;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import com.autodealer.crm.exception.BusinessException;
import com.autodealer.crm.result.CodeEnum;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
public class DicServiceImpl implements DicService {

    @Resource
    private DicMapper dicMapper;

    @Resource
    private RedisManager redisManager;

    @Resource
    private OperationAuditRecorder auditRecorder;

    @Resource
    private CurrentUserProvider currentUserProvider;

    private static final long CACHE_EXPIRE_SECONDS = 24 * 60 * 60; // 24小时

    @Override
    public PageInfo<TDicType> getDicTypes(DicQuery query) {
        PageHelper.startPage(query.getPage(), query.getSize());
        List<TDicType> list = dicMapper.selectDicTypes(query);
        return new PageInfo<>(list);
    }

    @Override
    public PageInfo<TDicValue> getDicValues(DicQuery query) {
        PageHelper.startPage(query.getPage(), query.getSize());
        List<TDicValue> list = dicMapper.selectDicValues(query);
        return new PageInfo<>(list);
    }

    @Override
    public TDicType getDicTypeById(Integer id) {
        String cacheKey = RedisKeys.dictTypeDetail(id);
        return CacheUtils.getCacheData(
            () -> redisManager.get(cacheKey),
            () -> dicMapper.selectDicTypeById(id),
            data -> redisManager.set(cacheKey, data, CACHE_EXPIRE_SECONDS)
        );
    }

    @Override
    public TDicValue getDicValueById(Integer id) {
        String cacheKey = RedisKeys.dictValueDetail(id);
        return CacheUtils.getCacheData(
            () -> redisManager.get(cacheKey),
            () -> dicMapper.selectDicValueById(id),
            data -> redisManager.set(cacheKey, data, CACHE_EXPIRE_SECONDS)
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addDicType(TDicType dicType) {
        dicType.setEnabled(defaultTrue(dicType.getEnabled()));
        dicType.setBuiltIn(Boolean.FALSE);
        boolean result = dicMapper.insertDicType(dicType) > 0;
        if (result) {
            evictDictionaryCaches();
            auditRecorder.record(AuditActionEnum.DICT_TYPE_SAVE, dicType.getTypeCode());
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addDicValue(TDicValue dicValue) {
        // 验证字典类型是否存在
        TDicType existingType = dicMapper.selectDicTypeByCode(dicValue.getTypeCode());
        if (existingType == null) {
            throw new BusinessException(CodeEnum.NOT_FOUND, "无法添加字典值：字典类型 " + dicValue.getTypeCode() + " 不存在");
        }
        if (Boolean.FALSE.equals(existingType.getEnabled())) {
            throw new BusinessException(CodeEnum.RESOURCE_IN_USE, "字典类型已停用，不能新增字典值");
        }

        dicValue.setEnabled(defaultTrue(dicValue.getEnabled()));
        dicValue.setBuiltIn(Boolean.FALSE);
        boolean result = dicMapper.insertDicValue(dicValue) > 0;
        if (result) {
            evictDictionaryCaches();
            auditRecorder.record(AuditActionEnum.DICT_VALUE_SAVE, dicValue.getTypeCode());
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateDicType(Integer id, TDicType dicType) {
        // 更新前先获取旧类型，用于缓存管理
        TDicType oldDicType = getDicTypeById(id);
        if (oldDicType == null) {
            return false;
        }
        ensureTypeCodeUnchanged(oldDicType, dicType);
        applyTypeDisableMetadata(oldDicType, dicType);

        boolean result = dicMapper.updateDicType(id, dicType) > 0;
        if (result) {
            evictDictionaryCaches();
            auditRecorder.record(AuditActionEnum.DICT_TYPE_SAVE, String.valueOf(id));
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateDicValue(Integer id, TDicValue dicValue) {
        // 更新前先获取旧值，用于缓存管理
        TDicValue oldDicValue = getDicValueById(id);
        if (oldDicValue == null) {
            return false;
        }
        ensureValueCodeUnchanged(oldDicValue, dicValue);
        applyValueDisableMetadata(oldDicValue, dicValue);

        // 设置 ID 用于更新
        dicValue.setId(id);

        boolean result = dicMapper.updateDicValue(dicValue) > 0;
        if (result) {
            evictDictionaryCaches();
            auditRecorder.record(AuditActionEnum.DICT_VALUE_SAVE, String.valueOf(id));
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteDicType(Integer id) {
        try {
            TDicType dicType = dicMapper.selectDicTypeById(id);
            if (dicType == null) {
                return false;
            }
            ensureNotBuiltIn(dicType.getBuiltIn(), "系统内置字典类型不能删除");

            // 1. 获取字典类型代码
            String typeCode = dicType.getTypeCode();

            // 2. 获取关联的字典值ID列表
            List<Integer> dicValueIds = dicMapper.selectDicValueIdsByTypeCode(typeCode);

            // 3. 检查是否有业务数据引用
            ensureValuesNotReferenced(dicValueIds, "该字典类型下有业务数据引用，无法删除");

            // 4. 删除未被引用的字典值，不删除任何业务历史或备注
            if (dicValueIds != null && !dicValueIds.isEmpty()) {
                dicMapper.deleteDicValuesByIds(dicValueIds);
            }

            // 5. 最后删除字典类型
            boolean result = dicMapper.deleteDicType(id) > 0;
            if (result) {
                evictDictionaryCaches();
                auditRecorder.record(AuditActionEnum.DICT_TYPE_DELETE, String.valueOf(id));
            }
            return result;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(CodeEnum.OPERATION_FAILED, "删除字典类型失败", e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteDicValue(Integer id) {
        try {
            // 获取字典值，用于后续清除缓存
            TDicValue dicValue = dicMapper.selectDicValueById(id);
            if (dicValue == null) {
                return false;
            }
            ensureNotBuiltIn(dicValue.getBuiltIn(), "系统内置字典值不能删除");
            ensureValuesNotReferenced(List.of(id), "该字典值已被业务引用，无法删除");

            // 只删除字典值本身，业务备注和历史事实必须保留
            boolean result = dicMapper.deleteDicValue(id) > 0;
            if (result) {
                evictDictionaryCaches();
                auditRecorder.record(AuditActionEnum.DICT_VALUE_DELETE, String.valueOf(id));
            }
            return result;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(CodeEnum.OPERATION_FAILED, "删除字典值失败", e);
        }
    }

    @Override
    public List<TDicValue> getDicValuesByTypeId(Integer typeId) {
        String cacheKey = RedisKeys.dictValuesByType(typeId);
        return CacheUtils.getCacheData(
            () -> redisManager.get(cacheKey),
            () -> dicMapper.selectDicValuesByTypeId(typeId),
            data -> redisManager.set(cacheKey, data, CACHE_EXPIRE_SECONDS)
        );
    }

    @Override
    public TDicType getDicTypeByCode(String typeCode) {
        String cacheKey = RedisKeys.dictTypeByCode(typeCode);
        return CacheUtils.getCacheData(
            () -> redisManager.get(cacheKey),
            () -> dicMapper.selectDicTypeByCode(typeCode),
            data -> redisManager.set(cacheKey, data, CACHE_EXPIRE_SECONDS)
        );
    }

    @Override
    public void evictDictionaryCaches() {
        boolean typeDeleted = redisManager.deletePattern(RedisKeys.dictTypePattern());
        boolean valueDeleted = redisManager.deletePattern(RedisKeys.dictValuePattern());
        boolean valuesByTypeDeleted = redisManager.deletePattern(RedisKeys.dictValuesByTypePattern());
        if (!typeDeleted || !valueDeleted || !valuesByTypeDeleted) {
            throw new BusinessException(CodeEnum.OPERATION_FAILED, "字典缓存失效失败");
        }
        log.info("Dictionary cache evicted");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteDicTypesByIds(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }

        try {
            // 1. 获取所有要删除的类型代码
            List<String> typeCodes = dicMapper.selectTypeCodesByIds(ids);
            if (typeCodes == null || typeCodes.isEmpty()) {
                return false;
            }
            List<TDicType> dicTypes = dicMapper.selectDicTypesByIds(ids);
            if (dicTypes != null) {
                for (TDicType dicType : dicTypes) {
                    ensureNotBuiltIn(dicType.getBuiltIn(), "系统内置字典类型不能删除");
                }
            }

            // 2. 批量获取关联的字典值ID
            List<Integer> dicValueIds = dicMapper.selectDicValueIdsByTypeCodes(typeCodes);

            // 3. 检查业务引用，禁止为了删除字典而删除业务历史
            ensureValuesNotReferenced(dicValueIds, "所选字典类型下有业务数据引用，无法删除");

            // 4. 再删除未被引用的字典值
            if (dicValueIds != null && !dicValueIds.isEmpty()) {
                dicMapper.deleteDicValuesByIds(dicValueIds);
            }

            // 5. 最后删除字典类型
            boolean result = dicMapper.deleteDicTypesByIds(ids) > 0;
            if (result) {
                evictDictionaryCaches();
                auditRecorder.record(AuditActionEnum.DICT_TYPE_DELETE, ids.toString());
            }
            return result;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(CodeEnum.OPERATION_FAILED, "批量删除字典类型失败", e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteDicValuesByIds(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }

        try {
            ensureValuesNotReferenced(ids, "所选字典值已被业务引用，无法删除");
            // 只删除未被引用的字典值，不删除任何业务备注或历史记录
            boolean result = dicMapper.deleteDicValuesByIds(ids) > 0;
            if (result) {
                evictDictionaryCaches();
                auditRecorder.record(AuditActionEnum.DICT_VALUE_DELETE, ids.toString());
            }
            return result;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(CodeEnum.OPERATION_FAILED, "批量删除字典值失败", e);
        }
    }

    @Override
    public void refreshTypeCache() {
        // 清除字典类型相关缓存
        ensureCacheDeleted(redisManager.deletePattern(RedisKeys.dictTypePattern()));
        // 重新加载字典类型数据到缓存
        DicQuery query = new DicQuery();
        List<TDicType> types = dicMapper.selectDicTypes(query);
        for (TDicType type : types) {
            String cacheKey = RedisKeys.dictTypeByCode(type.getTypeCode());
            redisManager.set(cacheKey, type, CACHE_EXPIRE_SECONDS);
        }
        log.info("Dictionary type cache refreshed, {} types loaded", types.size());
    }

    @Override
    public void refreshValueCache() {
        // 清除字典值相关缓存
        ensureCacheDeleted(redisManager.deletePattern(RedisKeys.dictValuePattern())
            && redisManager.deletePattern(RedisKeys.dictValuesByTypePattern()));
        // 重新加载字典值数据到缓存
        DicQuery query = new DicQuery();
        List<TDicValue> values = dicMapper.selectDicValues(query);
        for (TDicValue value : values) {
            String cacheKey = RedisKeys.dictValueDetail(value.getId());
            redisManager.set(cacheKey, value, CACHE_EXPIRE_SECONDS);
        }
        log.info("Dictionary value cache refreshed, {} values loaded", values.size());
    }

    private Boolean defaultTrue(Boolean value) {
        return value == null ? Boolean.TRUE : value;
    }

    private void ensureTypeCodeUnchanged(TDicType oldDicType, TDicType newDicType) {
        if (StringUtils.hasText(newDicType.getTypeCode())
            && !Objects.equals(oldDicType.getTypeCode(), newDicType.getTypeCode())) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "字典类型编码不能修改");
        }
    }

    private void ensureValueCodeUnchanged(TDicValue oldDicValue, TDicValue newDicValue) {
        if (StringUtils.hasText(newDicValue.getTypeCode())
            && !Objects.equals(oldDicValue.getTypeCode(), newDicValue.getTypeCode())) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "字典值所属类型不能修改");
        }
        if (StringUtils.hasText(newDicValue.getValueCode())
            && !Objects.equals(oldDicValue.getValueCode(), newDicValue.getValueCode())) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "字典值编码不能修改");
        }
    }

    private void applyTypeDisableMetadata(TDicType oldDicType, TDicType newDicType) {
        if (newDicType.getEnabled() == null) {
            return;
        }
        if (Boolean.TRUE.equals(oldDicType.getBuiltIn()) && Boolean.FALSE.equals(newDicType.getEnabled())) {
            throw new BusinessException(CodeEnum.RESOURCE_IN_USE, "系统内置字典类型不能停用");
        }
        applyDisableMetadata(oldDicType.getEnabled(), newDicType.getEnabled(), newDicType.getDisableReason(),
            newDicType::setDisableReason, newDicType::setDisabledBy, newDicType::setDisabledTime);
    }

    private void applyValueDisableMetadata(TDicValue oldDicValue, TDicValue newDicValue) {
        if (newDicValue.getEnabled() == null) {
            return;
        }
        if (Boolean.TRUE.equals(oldDicValue.getBuiltIn()) && Boolean.FALSE.equals(newDicValue.getEnabled())) {
            throw new BusinessException(CodeEnum.RESOURCE_IN_USE, "系统内置字典值不能停用");
        }
        applyDisableMetadata(oldDicValue.getEnabled(), newDicValue.getEnabled(), newDicValue.getDisableReason(),
            newDicValue::setDisableReason, newDicValue::setDisabledBy, newDicValue::setDisabledTime);
    }

    private void applyDisableMetadata(Boolean oldEnabled, Boolean newEnabled, String reason,
                                      java.util.function.Consumer<String> reasonSetter,
                                      java.util.function.Consumer<Integer> disabledBySetter,
                                      java.util.function.Consumer<LocalDateTime> disabledTimeSetter) {
        if (Boolean.FALSE.equals(newEnabled)) {
            if (Boolean.TRUE.equals(defaultTrue(oldEnabled)) && !StringUtils.hasText(reason)) {
                throw new BusinessException(CodeEnum.PARAM_ERROR, "停用字典必须填写原因");
            }
            reasonSetter.accept(reason);
            disabledBySetter.accept(currentUserIdOrNull());
            disabledTimeSetter.accept(LocalDateTime.now());
            return;
        }
        reasonSetter.accept(null);
        disabledBySetter.accept(null);
        disabledTimeSetter.accept(null);
    }

    private Integer currentUserIdOrNull() {
        try {
            return currentUserProvider == null ? null : currentUserProvider.getCurrentUserId();
        } catch (RuntimeException ex) {
            log.warn("Dictionary disable audit user unavailable: {}", ex.getMessage());
            return null;
        }
    }

    private void ensureNotBuiltIn(Boolean builtIn, String message) {
        if (Boolean.TRUE.equals(builtIn)) {
            throw new BusinessException(CodeEnum.RESOURCE_IN_USE, message);
        }
    }

    private void ensureValuesNotReferenced(List<Integer> valueIds, String message) {
        if (valueIds == null || valueIds.isEmpty()) {
            return;
        }
        int referenceCount = dicMapper.selectRemarkCountByDicValueIds(valueIds);
        if (referenceCount > 0) {
            throw new BusinessException(CodeEnum.RESOURCE_IN_USE, message);
        }
    }

    private void ensureCacheDeleted(boolean success) {
        if (!success) {
            throw new BusinessException(CodeEnum.OPERATION_FAILED, "字典缓存失效失败");
        }
    }
}
