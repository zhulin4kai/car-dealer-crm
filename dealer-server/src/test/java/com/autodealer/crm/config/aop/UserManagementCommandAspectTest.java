package com.autodealer.crm.config.aop;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.autodealer.crm.audit.AuditRequestIdProvider;
import com.autodealer.crm.config.security.CurrentUserProvider;
import com.autodealer.crm.dto.profile.ProfileDtos.UpdateRequest;
import com.autodealer.crm.exception.BusinessException;
import com.autodealer.crm.result.CodeEnum;
import com.autodealer.crm.service.command.UserManagementCommand;
import com.autodealer.crm.service.impl.ProfileServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.sql.DataSource;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = UserManagementCommandAspectTest.TestConfig.class)
class UserManagementCommandAspectTest {
    @Autowired TestCommands commands;
    @Autowired JdbcTemplate jdbc;

    private final Logger logger = (Logger) LoggerFactory.getLogger(UserManagementCommandAspect.class);
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();

    @BeforeEach
    void setUp() {
        appender.start();
        logger.addAppender(appender);
        jdbc.update("DELETE FROM command_probe");
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(appender);
        appender.stop();
    }

    @Test
    void rootCommandIsObservedOnceAndNestedCommandIsNotDuplicated() {
        assertEquals("nested-result", commands.nested());

        assertEquals(1, appender.list.size());
        String message = appender.list.get(0).getFormattedMessage();
        assertTrue(message.contains("commandCode=TEST_OUTER"));
        assertTrue(message.contains("requestId=request-1"));
        assertTrue(message.contains("operatorId=7"));
        assertTrue(message.contains("outcome=SUCCESS"));
        assertFalse(message.contains("TEST_INNER"));
    }

    @Test
    void unannotatedMethodIsIgnoredAndBusinessTransactionStillExists() {
        assertEquals("plain", commands.unannotated());
        assertTrue(appender.list.isEmpty());

        assertTrue(commands.transactionActive());
        assertEquals(1, appender.list.size());
        assertTrue(appender.list.get(0).getFormattedMessage().contains("commandCode=TEST_TRANSACTION"));
    }

    @Test
    void returnAndFailureTypesRemainUnchangedWithoutLoggingSensitiveMessages() {
        BusinessException expectedBusiness = commands.expectedBusinessFailure();
        BusinessException business = assertThrows(BusinessException.class, commands::businessFailure);
        assertSame(expectedBusiness, business);
        assertEquals(CodeEnum.PROFILE_VERSION_CONFLICT, business.getCodeEnum());
        assertFailureLog("TEST_BUSINESS_FAILURE", "BusinessException", "业务敏感消息");

        appender.list.clear();
        IllegalStateException expectedRuntime = commands.expectedRuntimeFailure();
        IllegalStateException runtime = assertThrows(IllegalStateException.class, commands::runtimeFailure);
        assertSame(expectedRuntime, runtime);
        assertFailureLog("TEST_RUNTIME_FAILURE", "IllegalStateException", "运行敏感消息");
    }

    @Test
    void runtimeFailureStillRollsBackBusinessTransaction() {
        IllegalStateException failure = assertThrows(IllegalStateException.class, commands::transactionRollback);

        assertSame(commands.expectedRollbackFailure(), failure);
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM command_probe", Integer.class));
        assertFailureLog("TEST_TRANSACTION_ROLLBACK", "IllegalStateException", "回滚敏感消息");
    }

    @Test
    void aspectHasNoDomainDependenciesAndProfilePilotKeepsTransactionBoundary() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/autodealer/crm/config/aop/UserManagementCommandAspect.java"));
        for (String forbidden : List.of("com.autodealer.crm.mapper", "PermissionCodes",
                "OperationAuditRecorder", "TAuthorizationHistoryMapper", "@Transactional",
                "lockByName(", "BusinessException", "AccountStatus", "EmployeeStatus")) {
            assertFalse(source.contains(forbidden), forbidden);
        }
        assertEquals(1, UserManagementCommand.class.getDeclaredMethods().length);
        Method method = ProfileServiceImpl.class.getMethod("updateOwn", UpdateRequest.class);
        assertEquals("PROFILE_UPDATE_OWN", method.getAnnotation(UserManagementCommand.class).value());
        assertNotNull(method.getAnnotation(Transactional.class));
    }

    private void assertFailureLog(String commandCode, String failureType, String sensitiveMessage) {
        assertEquals(1, appender.list.size());
        String message = appender.list.get(0).getFormattedMessage();
        assertTrue(message.contains("commandCode=" + commandCode));
        assertTrue(message.contains("outcome=FAILURE"));
        assertTrue(message.contains("failureType=" + failureType));
        assertFalse(message.contains(sensitiveMessage));
        assertFalse(message.contains("Exception:"));
    }

    @Configuration
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    @EnableTransactionManagement
    @Import(UserManagementCommandAspect.class)
    static class TestConfig {
        @Bean Clock businessClock() {
            return Clock.fixed(Instant.parse("2026-07-13T10:00:00Z"), ZoneOffset.UTC);
        }

        @Bean AuditRequestIdProvider requestIds() {
            AuditRequestIdProvider provider = mock(AuditRequestIdProvider.class);
            when(provider.currentRequestId()).thenReturn("request-1");
            return provider;
        }

        @Bean CurrentUserProvider currentUser() {
            CurrentUserProvider provider = mock(CurrentUserProvider.class);
            when(provider.getCurrentUserId()).thenReturn(7);
            return provider;
        }

        @Bean InnerCommand innerCommand() { return new InnerCommand(); }
        @Bean TestCommands testCommands(InnerCommand inner, JdbcTemplate jdbc) {
            return new TestCommands(inner, jdbc);
        }

        @Bean DataSource dataSource() {
            return new DriverManagerDataSource("jdbc:h2:mem:user-command-aspect;MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        }

        @Bean PlatformTransactionManager transactionManager(DataSource dataSource) {
            return new DataSourceTransactionManager(dataSource);
        }

        @Bean JdbcTemplate jdbcTemplate(DataSource dataSource) {
            JdbcTemplate jdbc = new JdbcTemplate(dataSource);
            jdbc.execute("CREATE TABLE command_probe(id INTEGER PRIMARY KEY)");
            return jdbc;
        }
    }

    static class InnerCommand {
        @UserManagementCommand("TEST_INNER") public String execute() { return "nested-result"; }
    }

    static class TestCommands {
        private final InnerCommand inner;
        private final JdbcTemplate jdbc;
        private final BusinessException businessFailure = new BusinessException(
                CodeEnum.PROFILE_VERSION_CONFLICT, "业务敏感消息");
        private final IllegalStateException runtimeFailure = new IllegalStateException("运行敏感消息");
        private final IllegalStateException rollbackFailure = new IllegalStateException("回滚敏感消息");

        TestCommands(InnerCommand inner, JdbcTemplate jdbc) { this.inner = inner; this.jdbc = jdbc; }

        @UserManagementCommand("TEST_OUTER") public String nested() { return inner.execute(); }
        public String unannotated() { return "plain"; }
        @Transactional @UserManagementCommand("TEST_TRANSACTION")
        public boolean transactionActive() { return TransactionSynchronizationManager.isActualTransactionActive(); }
        @UserManagementCommand("TEST_BUSINESS_FAILURE") public void businessFailure() { throw businessFailure; }
        @UserManagementCommand("TEST_RUNTIME_FAILURE") public void runtimeFailure() { throw runtimeFailure; }
        @Transactional @UserManagementCommand("TEST_TRANSACTION_ROLLBACK")
        public void transactionRollback() { jdbc.update("INSERT INTO command_probe(id) VALUES(1)"); throw rollbackFailure; }
        public BusinessException expectedBusinessFailure() { return businessFailure; }
        public IllegalStateException expectedRuntimeFailure() { return runtimeFailure; }
        public IllegalStateException expectedRollbackFailure() { return rollbackFailure; }
    }
}
