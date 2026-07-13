package com.autodealer.crm.config.security;

import com.autodealer.crm.constant.RedisKeys;
import com.autodealer.crm.manager.RedisManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OwnerCandidateCacheInvalidatorTest {
    private final RedisManager redis = mock(RedisManager.class);
    private final OwnerCandidateCacheInvalidator invalidator = new OwnerCandidateCacheInvalidator(redis);

    @BeforeEach
    void beginTransactionSynchronization() {
        TransactionSynchronizationManager.setActualTransactionActive(true);
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void clearTransactionSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
        TransactionSynchronizationManager.setActualTransactionActive(false);
        TransactionSynchronizationManager.clear();
    }

    @Test
    void duplicateEventsDeleteAllLegacyAndTripleOwnerKeysOnlyOnceAfterCommit() {
        when(redis.deletePattern(RedisKeys.ownerListPattern())).thenReturn(true);

        invalidator.invalidateAfterCommit();
        invalidator.invalidateAfterCommit();

        verify(redis, never()).deletePattern(RedisKeys.ownerListPattern());
        List<TransactionSynchronization> synchronizations = TransactionSynchronizationManager.getSynchronizations();
        assertEquals(1, synchronizations.size());
        synchronizations.forEach(TransactionSynchronization::afterCommit);
        synchronizations.forEach(sync -> sync.afterCompletion(TransactionSynchronization.STATUS_COMMITTED));

        verify(redis).deletePattern("cdrm:user:owner*");
        assertEquals("cdrm:user:owner:7:user:status:ACTIVITY_OWNER",
                RedisKeys.ownerList(7, "user:status", "ACTIVITY_OWNER"));
    }

    @Test
    void rollbackNeverDeletesAndFailureRetriesAfterCommit() {
        invalidator.invalidateAfterCommit();
        List<TransactionSynchronization> rollback = TransactionSynchronizationManager.getSynchronizations();
        rollback.forEach(sync -> sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
        verify(redis, never()).deletePattern(RedisKeys.ownerListPattern());

        TransactionSynchronizationManager.clearSynchronization();
        TransactionSynchronizationManager.initSynchronization();
        when(redis.deletePattern(RedisKeys.ownerListPattern())).thenReturn(false, true);
        invalidator.invalidateAfterCommit();
        List<TransactionSynchronization> committed = TransactionSynchronizationManager.getSynchronizations();
        committed.forEach(TransactionSynchronization::afterCommit);
        committed.forEach(sync -> sync.afterCompletion(TransactionSynchronization.STATUS_COMMITTED));
        verify(redis, times(2)).deletePattern(RedisKeys.ownerListPattern());
    }

    @Test
    void registrationOutsideTransactionFailsClosed() {
        TransactionSynchronizationManager.clearSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(false);
        assertThrows(IllegalStateException.class, invalidator::invalidateAfterCommit);
    }
}
