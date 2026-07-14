package com.autodealer.crm.config.aop;

import com.autodealer.crm.audit.AuditRequestIdProvider;
import com.autodealer.crm.config.security.CurrentUserProvider;
import com.autodealer.crm.service.command.UserManagementCommand;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 100)
public class UserManagementCommandAspect {
    private static final Logger log = LoggerFactory.getLogger(UserManagementCommandAspect.class);

    private final ThreadLocal<Boolean> activeRoot = new ThreadLocal<>();
    private final AuditRequestIdProvider requestIds;
    private final CurrentUserProvider currentUser;
    private final Clock clock;

    public UserManagementCommandAspect(AuditRequestIdProvider requestIds,
                                       CurrentUserProvider currentUser,
                                       Clock clock) {
        this.requestIds = requestIds;
        this.currentUser = currentUser;
        this.clock = clock;
    }

    @Around("@annotation(command)")
    public Object invoke(ProceedingJoinPoint joinPoint, UserManagementCommand command) throws Throwable {
        if (activeRoot.get() != null) return joinPoint.proceed();
        String requestId = requestIds.currentRequestId();
        Integer operatorId = currentUser.getCurrentUserId();
        Instant startedAt = Instant.now(clock);
        String rootInvocationId = UUID.randomUUID().toString();
        activeRoot.set(Boolean.TRUE);
        try {
            Object result = joinPoint.proceed();
            observe(command.value(), requestId, operatorId, rootInvocationId, startedAt, null);
            return result;
        } catch (Throwable failure) {
            observe(command.value(), requestId, operatorId, rootInvocationId, startedAt, failure);
            throw failure;
        } finally {
            activeRoot.remove();
        }
    }

    private void observe(String commandCode, String requestId, Integer operatorId,
                         String rootInvocationId, Instant startedAt, Throwable failure) {
        try {
            long durationMs = Math.max(0, Instant.now(clock).toEpochMilli() - startedAt.toEpochMilli());
            if (failure == null) {
                log.info("用户管理命令完成 commandCode={} requestId={} operatorId={} rootInvocationId={} outcome=SUCCESS durationMs={}",
                        commandCode, requestId, operatorId, rootInvocationId, durationMs);
            } else {
                log.warn("用户管理命令完成 commandCode={} requestId={} operatorId={} rootInvocationId={} outcome=FAILURE durationMs={} failureType={}",
                        commandCode, requestId, operatorId, rootInvocationId, durationMs,
                        failure.getClass().getSimpleName());
            }
        } catch (RuntimeException observationFailure) {
            log.warn("用户管理命令观测失败 commandCode={} rootInvocationId={} failureType={}",
                    commandCode, rootInvocationId, observationFailure.getClass().getSimpleName());
        }
    }
}
