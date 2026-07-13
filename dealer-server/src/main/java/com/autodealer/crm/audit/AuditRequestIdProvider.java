package com.autodealer.crm.audit;

import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.UUID;

/** 为同一服务端请求生成并复用可信审计关联 ID。 */
@Component
public class AuditRequestIdProvider {
    private static final String ATTRIBUTE = AuditRequestIdProvider.class.getName() + ".requestId";
    private static final Object TRANSACTION_RESOURCE_KEY = AuditRequestIdProvider.class.getName() + ".transactionRequestId";

    public String currentRequestId() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes == null) return transactionalRequestId();
        Object existing = attributes.getAttribute(ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
        if (existing instanceof String requestId && !requestId.isBlank()) return requestId;
        String external = null;
        if (attributes instanceof ServletRequestAttributes servlet) {
            external = normalizeExternal(servlet.getRequest().getHeader("X-Request-Id"));
        }
        String generated = UUID.randomUUID().toString();
        String trusted = external == null ? generated : external + "-" + generated.substring(0, 12);
        attributes.setAttribute(ATTRIBUTE, trusted, RequestAttributes.SCOPE_REQUEST);
        return trusted;
    }

    private String transactionalRequestId() {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) return UUID.randomUUID().toString();
        Object existing = TransactionSynchronizationManager.getResource(TRANSACTION_RESOURCE_KEY);
        if (existing instanceof String requestId && !requestId.isBlank()) return requestId;
        String generated = UUID.randomUUID().toString();
        TransactionSynchronizationManager.bindResource(TRANSACTION_RESOURCE_KEY, generated);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCompletion(int status) {
                if (TransactionSynchronizationManager.hasResource(TRANSACTION_RESOURCE_KEY)) {
                    TransactionSynchronizationManager.unbindResource(TRANSACTION_RESOURCE_KEY);
                }
            }
        });
        return generated;
    }

    private String normalizeExternal(String value) {
        if (value == null || value.isBlank()) return null;
        String sanitized = AuditSensitiveDataSanitizer.sanitize(value)
                .replaceAll("[^A-Za-z0-9._:-]", "_");
        if (sanitized.isBlank()) return null;
        return sanitized.length() <= 48 ? sanitized : sanitized.substring(0, 48);
    }
}
