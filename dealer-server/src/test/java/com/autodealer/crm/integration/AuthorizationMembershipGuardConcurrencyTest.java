package com.autodealer.crm.integration;

import com.autodealer.crm.bootstrap.DealerCRMApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = DealerCRMApplication.class)
@ActiveProfiles("test")
class AuthorizationMembershipGuardConcurrencyTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired TransactionTemplate transactions;

    @Test
    void matrixAndPersonalPermissionWritersSerializeOnTheSameGuardWithoutDeadlock() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch firstLocked = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        CountDownLatch secondStarted = new CountDownLatch(1);
        CountDownLatch secondLocked = new CountDownLatch(1);
        try {
            Future<?> matrixWriter = executor.submit(() -> transactions.executeWithoutResult(status -> {
                assertEquals("AUTHORIZATION_MEMBERSHIP_GUARD", jdbc.queryForObject(
                        "SELECT lock_name FROM t_authorization_graph_lock WHERE lock_name='AUTHORIZATION_MEMBERSHIP_GUARD' FOR UPDATE", String.class));
                firstLocked.countDown();
                try { assertTrue(releaseFirst.await(3, TimeUnit.SECONDS)); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new RuntimeException(e); }
            }));
            Future<?> personalPermissionWriter = executor.submit(() -> {
                try { assertTrue(firstLocked.await(3, TimeUnit.SECONDS)); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new RuntimeException(e); }
                secondStarted.countDown();
                transactions.executeWithoutResult(status -> {
                    assertEquals("AUTHORIZATION_MEMBERSHIP_GUARD", jdbc.queryForObject(
                            "SELECT lock_name FROM t_authorization_graph_lock WHERE lock_name='AUTHORIZATION_MEMBERSHIP_GUARD' FOR UPDATE", String.class));
                    secondLocked.countDown();
                });
            });
            assertTrue(firstLocked.await(3, TimeUnit.SECONDS));
            assertTrue(secondStarted.await(3, TimeUnit.SECONDS));
            assertFalse(secondLocked.await(200, TimeUnit.MILLISECONDS), "第二个成员图写事务不应越过首个 FOR UPDATE");
            releaseFirst.countDown();
            assertTrue(secondLocked.await(3, TimeUnit.SECONDS));
            matrixWriter.get(3, TimeUnit.SECONDS);
            personalPermissionWriter.get(3, TimeUnit.SECONDS);
        } finally {
            releaseFirst.countDown();
            executor.shutdownNow();
        }
    }
}
