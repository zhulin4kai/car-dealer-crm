package com.autodealer.crm.shared.infrastructure.cache;

import org.springframework.util.ObjectUtils;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class CacheUtils {

    /**
     * 带有缓存的查询工具方法
     *
     * @param cacheSelector
     * @param databaseSelector
     * @param cacheSave
     * @return
     * @param <T>
     */
    public static <T> T getCacheData(Supplier<T> cacheSelector, Supplier<T> databaseSelector, Consumer<T> cacheSave) {
        //从redis查询
        T data = cacheSelector.get();
        //如果redis没查到
        if (ObjectUtils.isEmpty(data)) {
            //从数据库查
            data = databaseSelector.get();
            //数据库查到了数据
            if (!ObjectUtils.isEmpty(data)) {
                //把数据放入redis
                cacheSave.accept(data);
            }
        }
        //返回数据
        return data;
    }

    /**
     * 生成缓存key
     */
    public static String generateKey(Object... params) {
        StringBuilder key = new StringBuilder();
        for (Object param : params) {
            if (param != null) {
                key.append(param.toString()).append(":");
            }
        }
        return key.length() > 0 ? key.substring(0, key.length() - 1) : "";
    }
}
