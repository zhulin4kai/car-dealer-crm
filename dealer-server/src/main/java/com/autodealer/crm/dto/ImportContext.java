package com.autodealer.crm.dto;

import com.autodealer.crm.model.TActivity;
import com.autodealer.crm.model.TDicValue;
import com.autodealer.crm.model.TUser;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Excel 导入字典、商品、负责人和活动名称映射上下文。
 *
 * <p>由 Service 在导入开始时一次性构造，不依赖全局静态 cacheMap。
 */
public class ImportContext {

    private final Map<String, List<TDicValue>> dicMap;
    private final Map<String, ProductSimpleDTO> productMap;
    private final Map<String, TUser> ownerMap;
    private final Map<String, TActivity> activityMap;

    public ImportContext(Map<String, List<TDicValue>> dicMap,
                         Map<String, ProductSimpleDTO> productMap,
                         Map<String, TUser> ownerMap,
                         Map<String, TActivity> activityMap) {
        this.dicMap = dicMap != null ? Collections.unmodifiableMap(dicMap) : Collections.emptyMap();
        this.productMap = productMap != null ? Collections.unmodifiableMap(productMap) : Collections.emptyMap();
        this.ownerMap = ownerMap != null ? Collections.unmodifiableMap(ownerMap) : Collections.emptyMap();
        this.activityMap = activityMap != null ? Collections.unmodifiableMap(activityMap) : Collections.emptyMap();
    }

    /**
     * 根据字典类型 code 获取字典值列表。
     */
    public List<TDicValue> getDicValues(String dicTypeCode) {
        List<TDicValue> values = dicMap.get(dicTypeCode);
        return values != null ? values : Collections.emptyList();
    }

    /**
     * 根据字典值文本查找其 ID，未找到返回 null。
     */
    public Integer findDicValueId(String dicTypeCode, String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        String trimmed = text.trim();
        for (TDicValue v : getDicValues(dicTypeCode)) {
            if (trimmed.equals(v.getTypeValue())) {
                return v.getId();
            }
        }
        return null;
    }

    /**
     * 根据商品名称查找其 ID，未找到返回 null。
     */
    public Integer findProductId(String productName) {
        if (productName == null || productName.trim().isEmpty()) {
            return null;
        }
        String trimmed = productName.trim();
        ProductSimpleDTO dto = productMap.get(trimmed);
        return dto != null ? dto.getId() : null;
    }

    /**
     * 根据负责人名称查找其 ID，未找到返回 null。
     */
    public Integer findOwnerId(String ownerName) {
        if (ownerName == null || ownerName.trim().isEmpty()) {
            return null;
        }
        String trimmed = ownerName.trim();
        TUser user = ownerMap.get(trimmed);
        return user != null ? user.getId() : null;
    }

    /**
     * 根据活动名称查找其 ID，未找到返回 null。
     */
    public Integer findActivityId(String activityName) {
        if (activityName == null || activityName.trim().isEmpty()) {
            return null;
        }
        String trimmed = activityName.trim();
        TActivity activity = activityMap.get(trimmed);
        return activity != null ? activity.getId() : null;
    }

    /**
     * 根据活动名称查找可写入业务记录的活动名称快照。
     */
    public String findActivityNameSnapshot(String activityName) {
        if (activityName == null || activityName.trim().isEmpty()) {
            return null;
        }
        String trimmed = activityName.trim();
        TActivity activity = activityMap.get(trimmed);
        return activity != null ? activity.getName() : null;
    }
}
