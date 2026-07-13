package com.autodealer.crm.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class TestDriveScheduleGuardConcurrencyTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired TransactionTemplate transactions;

    @Test
    void testDriveWriterAndLifecycleHandoverSerializeOnTheSharedScheduleGuard() throws Exception {
        ExecutorService executor=Executors.newFixedThreadPool(2);
        CountDownLatch writerLocked=new CountDownLatch(1),releaseWriter=new CountDownLatch(1),handoverStarted=new CountDownLatch(1),handoverLocked=new CountDownLatch(1);
        try {
            Future<?> testDriveWriter=executor.submit(()->transactions.executeWithoutResult(status->{
                assertEquals("TEST_DRIVE_SCHEDULE_GUARD",lock());writerLocked.countDown();
                try{assertTrue(releaseWriter.await(3,TimeUnit.SECONDS));}catch(InterruptedException exception){Thread.currentThread().interrupt();throw new RuntimeException(exception);}
            }));
            Future<?> lifecycleHandover=executor.submit(()->{
                try{assertTrue(writerLocked.await(3,TimeUnit.SECONDS));}catch(InterruptedException exception){Thread.currentThread().interrupt();throw new RuntimeException(exception);}
                handoverStarted.countDown();transactions.executeWithoutResult(status->{assertEquals("TEST_DRIVE_SCHEDULE_GUARD",lock());handoverLocked.countDown();});
            });
            assertTrue(writerLocked.await(3,TimeUnit.SECONDS));assertTrue(handoverStarted.await(3,TimeUnit.SECONDS));
            assertFalse(handoverLocked.await(200,TimeUnit.MILLISECONDS),"试驾写事务持锁时，离职交接不得越过共享排期锁");
            releaseWriter.countDown();assertTrue(handoverLocked.await(3,TimeUnit.SECONDS));
            testDriveWriter.get(3,TimeUnit.SECONDS);lifecycleHandover.get(3,TimeUnit.SECONDS);
        } finally {releaseWriter.countDown();executor.shutdownNow();}
    }

    private String lock(){return jdbc.queryForObject("SELECT lock_name FROM t_authorization_graph_lock WHERE lock_name='TEST_DRIVE_SCHEDULE_GUARD' FOR UPDATE",String.class);}
}
