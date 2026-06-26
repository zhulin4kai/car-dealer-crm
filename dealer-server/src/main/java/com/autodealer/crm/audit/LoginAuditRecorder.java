package com.autodealer.crm.audit;

import com.autodealer.crm.mapper.TLoginLogMapper;
import com.autodealer.crm.mapper.TUserMapper;
import com.autodealer.crm.model.TLoginLog;
import com.autodealer.crm.model.TUser;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.Locale;
import java.util.UUID;

@Component
public class LoginAuditRecorder {

    public static final String RESULT_SUCCESS = "SUCCESS";
    public static final String RESULT_FAILURE = "FAILURE";
    private static final Logger log = LoggerFactory.getLogger(LoginAuditRecorder.class);

    private final TLoginLogMapper loginLogMapper;
    private final TUserMapper userMapper;

    public LoginAuditRecorder(TLoginLogMapper loginLogMapper, TUserMapper userMapper) {
        this.loginLogMapper = loginLogMapper;
        this.userMapper = userMapper;
    }

    public void recordSuccess(TUser user, HttpServletRequest request) {
        if (user == null) {
            throw new IllegalArgumentException("登录成功审计缺少用户");
        }
        TLoginLog logEntry = buildBaseLog(request);
        logEntry.setLoginAct(limit(user.getLoginAct(), 64));
        logEntry.setUserId(user.getId());
        logEntry.setUserName(limit(user.getName(), 64));
        logEntry.setResult(RESULT_SUCCESS);
        logEntry.setReasonCode("SUCCESS");
        logEntry.setReasonMessage("登录成功");
        insertOrThrow(logEntry);
    }

    public void recordFailure(String loginAct, AuthenticationException exception, HttpServletRequest request) {
        TLoginLog logEntry = buildBaseLog(request);
        String actualLoginAct = StringUtils.hasText(loginAct) ? loginAct : "";
        logEntry.setLoginAct(limit(actualLoginAct, 64));
        bindKnownUser(logEntry, actualLoginAct);
        logEntry.setResult(RESULT_FAILURE);
        logEntry.setReasonCode(resolveReasonCode(exception));
        logEntry.setReasonMessage(resolveReasonMessage(logEntry.getReasonCode()));
        insertOrThrow(logEntry);
    }

    private void bindKnownUser(TLoginLog logEntry, String loginAct) {
        if (!StringUtils.hasText(loginAct)) {
            return;
        }
        try {
            TUser user = userMapper.selectByLoginAct(loginAct);
            if (user != null) {
                logEntry.setUserId(user.getId());
                logEntry.setUserName(limit(user.getName(), 64));
            }
        } catch (RuntimeException e) {
            log.warn("登录失败审计关联用户失败 loginAct={}", sanitizeForAppLog(loginAct), e);
        }
    }

    private TLoginLog buildBaseLog(HttpServletRequest request) {
        TLoginLog logEntry = new TLoginLog();
        logEntry.setIp(limit(extractClientIp(request), 64));
        logEntry.setBrowser(limit(resolveBrowser(request), 128));
        logEntry.setOs(limit(resolveOs(request), 128));
        logEntry.setRequestId(limit(resolveRequestId(request), 64));
        logEntry.setCreateTime(new Date());
        return logEntry;
    }

    private void insertOrThrow(TLoginLog logEntry) {
        int rows = loginLogMapper.insert(logEntry);
        if (rows != 1) {
            throw new IllegalStateException("登录审计记录写入失败，影响行数: " + rows
                    + "，loginAct=" + sanitizeForAppLog(logEntry.getLoginAct())
                    + " result=" + logEntry.getResult());
        }
    }

    private String resolveReasonCode(AuthenticationException exception) {
        if (exception instanceof BadCredentialsException) {
            return "BAD_CREDENTIALS";
        }
        if (exception instanceof DisabledException) {
            return "ACCOUNT_DISABLED";
        }
        if (exception instanceof LockedException) {
            return "ACCOUNT_LOCKED";
        }
        if (exception instanceof AccountExpiredException) {
            return "ACCOUNT_EXPIRED";
        }
        if (exception instanceof CredentialsExpiredException) {
            return "CREDENTIALS_EXPIRED";
        }
        return "AUTHENTICATION_FAILED";
    }

    private String resolveReasonMessage(String reasonCode) {
        return switch (reasonCode) {
            case "BAD_CREDENTIALS" -> "账号或密码错误";
            case "ACCOUNT_DISABLED" -> "账号已停用";
            case "ACCOUNT_LOCKED" -> "账号已锁定";
            case "ACCOUNT_EXPIRED" -> "账号已过期";
            case "CREDENTIALS_EXPIRED" -> "凭证已过期";
            default -> "认证失败";
        };
    }

    private String extractClientIp(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwardedFor)) {
            return forwardedFor.split(",")[0].trim();
        }
        String remoteAddr = request.getRemoteAddr();
        return StringUtils.hasText(remoteAddr) ? remoteAddr : "unknown";
    }

    private String resolveRequestId(HttpServletRequest request) {
        if (request != null) {
            String header = request.getHeader("X-Request-Id");
            if (StringUtils.hasText(header)) {
                return header;
            }
        }
        return UUID.randomUUID().toString();
    }

    private String resolveBrowser(HttpServletRequest request) {
        String userAgent = request == null ? "" : request.getHeader("User-Agent");
        if (!StringUtils.hasText(userAgent)) {
            return "unknown";
        }
        String lower = userAgent.toLowerCase(Locale.ROOT);
        if (lower.contains("edg/")) {
            return "Edge";
        }
        if (lower.contains("chrome/")) {
            return "Chrome";
        }
        if (lower.contains("safari/")) {
            return "Safari";
        }
        if (lower.contains("firefox/")) {
            return "Firefox";
        }
        return "Other";
    }

    private String resolveOs(HttpServletRequest request) {
        String userAgent = request == null ? "" : request.getHeader("User-Agent");
        if (!StringUtils.hasText(userAgent)) {
            return "unknown";
        }
        String lower = userAgent.toLowerCase(Locale.ROOT);
        if (lower.contains("windows")) {
            return "Windows";
        }
        if (lower.contains("mac os") || lower.contains("macintosh")) {
            return "macOS";
        }
        if (lower.contains("android")) {
            return "Android";
        }
        if (lower.contains("iphone") || lower.contains("ipad")) {
            return "iOS";
        }
        if (lower.contains("linux")) {
            return "Linux";
        }
        return "Other";
    }

    private String limit(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        String sanitized = value.replace('\r', ' ').replace('\n', ' ').replace('\t', ' ').trim();
        return sanitized.length() <= maxLength ? sanitized : sanitized.substring(0, maxLength);
    }

    private String sanitizeForAppLog(String value) {
        return value == null ? "" : value.replaceAll("[\\r\\n\\t]", "_");
    }
}
