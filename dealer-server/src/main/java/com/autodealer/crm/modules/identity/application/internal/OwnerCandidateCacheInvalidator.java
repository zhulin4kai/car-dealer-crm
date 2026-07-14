package com.autodealer.crm.modules.identity.application.internal;

import com.autodealer.crm.shared.infrastructure.cache.RedisKeys;
import com.autodealer.crm.shared.infrastructure.cache.RedisManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** 负责人候选三元缓存的事务提交后统一失效入口。 */
@Component
public class OwnerCandidateCacheInvalidator {
    private static final Logger log = LoggerFactory.getLogger(OwnerCandidateCacheInvalidator.class);
    private static final String TRANSACTION_RESOURCE = OwnerCandidateCacheInvalidator.class.getName() + ".scheduled";
    private static final int DELETE_ATTEMPTS = 2;

    private final RedisManager redisManager;

    public OwnerCandidateCacheInvalidator(RedisManager redisManager) {
        this.redisManager = redisManager;
    }

    /**
     * 同一事务无论有多少资格事实发生变化，只注册一次提交后失效。
     * 调用方必须处于真实事务中，避免误把回滚前状态暴露给缓存消费者。
     */
    public void invalidateAfterCommit() {
        if (!TransactionSynchronizationManager.isActualTransactionActive()
                || !TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalStateException("负责人候选缓存失效必须在事务中注册");
        }
        if (TransactionSynchronizationManager.hasResource(TRANSACTION_RESOURCE)) return;

        CacheInvalidationSynchronization synchronization = new CacheInvalidationSynchronization();
        TransactionSynchronizationManager.bindResource(TRANSACTION_RESOURCE, synchronization);
        try {
            TransactionSynchronizationManager.registerSynchronization(synchronization);
        } catch (RuntimeException exception) {
            TransactionSynchronizationManager.unbindResourceIfPossible(TRANSACTION_RESOURCE);
            throw exception;
        }
    }

    private final class CacheInvalidationSynchronization implements TransactionSynchronization {
        @Override
        public void suspend() {
            unbindSelf();
        }

        @Override
        public void resume() {
            if (!TransactionSynchronizationManager.hasResource(TRANSACTION_RESOURCE)) {
                TransactionSynchronizationManager.bindResource(TRANSACTION_RESOURCE, this);
            }
        }

        @Override
        public void afterCommit() {
            for (int attempt = 1; attempt <= DELETE_ATTEMPTS; attempt++) {
                try {
                    if (redisManager.deletePattern(RedisKeys.ownerListPattern())) return;
                } catch (RuntimeException exception) {
                    log.warn("负责人候选缓存失效失败 attempt={}", attempt, exception);
                }
            }
            log.error("负责人候选缓存失效重试耗尽 pattern={} attempts={}",
                    RedisKeys.ownerListPattern(), DELETE_ATTEMPTS);
        }

        @Override
        public void afterCompletion(int status) {
            unbindSelf();
        }

        private void unbindSelf() {
            Object bound = TransactionSynchronizationManager.getResource(TRANSACTION_RESOURCE);
            if (bound == this) TransactionSynchronizationManager.unbindResource(TRANSACTION_RESOURCE);
        }
    }
}
