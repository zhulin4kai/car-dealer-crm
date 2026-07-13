package com.autodealer.crm.config.handler;

import com.autodealer.crm.exception.BusinessException;
import com.autodealer.crm.result.CodeEnum;
import com.autodealer.crm.result.R;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;

/**
 * 统一异常处理类，controller发生了异常，统一用该类进行处理。
 *
 * <p>所有异常均通过 {@link ResponseEntity} 返回真实的 HTTP 状态码，
 * 响应体统一使用 {@code R.FAIL(code, msg)} 格式。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ==================== 业务异常 ====================

    @ExceptionHandler(value = BusinessException.class)
    public ResponseEntity<R> handleBusinessException(BusinessException e) {
        log.warn("业务异常: {}", e.getMessage());
        CodeEnum codeEnum = e.getCodeEnum();
        HttpStatus httpStatus = mapHttpStatusCode(codeEnum);
        R body = e.getData() == null
                ? R.FAIL(codeEnum.getCode(), e.getMessage())
                : R.FAIL(codeEnum.getCode(), e.getMessage(), e.getData());
        return ResponseEntity.status(httpStatus).body(body);
    }

    // ==================== 参数校验异常 ====================

    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    public ResponseEntity<R> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        log.warn("参数校验失败 fields={}", e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField()).distinct().sorted().toList());
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .findFirst()
                .orElse("参数校验失败");
        R body = R.FAIL(CodeEnum.PARAM_ERROR.getCode(), message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(value = BindException.class)
    public ResponseEntity<R> handleBindException(BindException e) {
        log.warn("参数绑定失败 fields={}", e.getFieldErrors().stream()
                .map(error -> error.getField()).distinct().sorted().toList());
        String message = e.getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .findFirst()
                .orElse("参数绑定失败");
        R body = R.FAIL(CodeEnum.PARAM_ERROR.getCode(), message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(value = ConstraintViolationException.class)
    public ResponseEntity<R> handleConstraintViolationException(ConstraintViolationException e) {
        log.warn("约束校验失败 fields={}", e.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath().toString()).distinct().sorted().toList());
        String message = e.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .findFirst()
                .map(Object::toString)
                .orElse("约束校验失败");
        R body = R.FAIL(CodeEnum.PARAM_ERROR.getCode(), message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(value = HttpMessageNotReadableException.class)
    public ResponseEntity<R> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.warn("请求体格式错误 type={}", e.getClass().getSimpleName());
        R body = R.FAIL(CodeEnum.PARAM_ERROR.getCode(), "请求体格式错误");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(value = MissingServletRequestPartException.class)
    public ResponseEntity<R> handleMissingServletRequestPartException(MissingServletRequestPartException e) {
        log.warn("缺少请求部分: {}", e.getMessage());
        R body = R.FAIL(CodeEnum.PARAM_ERROR.getCode(), "缺少必要的请求部分: " + e.getRequestPartName());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // ==================== 权限异常 ====================

    @ExceptionHandler(value = AccessDeniedException.class)
    public ResponseEntity<R> handleAccessDeniedException(AccessDeniedException e) {
        log.warn("权限不足: {}", e.getMessage());
        R body = R.FAIL(CodeEnum.ACCESS_DENIED);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    // ==================== 数据访问异常 ====================

    @ExceptionHandler(value = DuplicateKeyException.class)
    public ResponseEntity<R> handleDuplicateKeyException(DuplicateKeyException e) {
        log.error("数据唯一键冲突: {}", e.getMessage(), e);
        R body = R.FAIL(CodeEnum.DUPLICATE);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(value = DataAccessException.class)
    public ResponseEntity<R> handleDataAccessException(DataAccessException e) {
        log.error("数据访问异常: {}", e.getMessage(), e);
        R body = R.FAIL(CodeEnum.DATA_ACCESS_EXCEPTION);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    // ==================== 请求方法不支持 ====================

    @ExceptionHandler(value = HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<R> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        log.warn("不支持的请求方法: {}", e.getMethod());
        R body = R.FAIL(CodeEnum.FAIL.getCode(), "不支持的请求方法: " + e.getMethod());
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(body);
    }

    // ==================== 未知运行时异常 ====================

    @ExceptionHandler(value = RuntimeException.class)
    public ResponseEntity<R> handleRuntimeException(RuntimeException e) {
        log.error("未预期异常: {}", e.getMessage(), e);
        R body = R.FAIL(CodeEnum.SYSTEM_ERROR.getCode(), "系统繁忙，请稍后重试");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    // ==================== 未知异常 ====================

    @ExceptionHandler(value = Exception.class)
    public ResponseEntity<R> handleException(Exception e) {
        log.error("系统异常: {}", e.getMessage(), e);
        R body = R.FAIL(CodeEnum.SYSTEM_ERROR.getCode(), "系统繁忙，请稍后重试");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    // ==================== 辅助方法 ====================

    /**
     * 根据 CodeEnum 映射对应的 HTTP 状态码。
     */
    private HttpStatus mapHttpStatusCode(CodeEnum codeEnum) {
        if (codeEnum == null) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return switch (codeEnum) {
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case PARAM_ERROR -> HttpStatus.BAD_REQUEST;
            case OPERATION_FAILED, TRAN_NO_PRODUCTS, RESOURCE_IN_USE -> HttpStatus.UNPROCESSABLE_ENTITY;
            case DUPLICATE, TRAN_STATE_CONFLICT,
                 ORGANIZATION_VERSION_CONFLICT, ORGANIZATION_PARENT_CYCLE,
                 REPORTING_CYCLE, INVALID_MANAGER,
                 ORGANIZATION_HAS_ACTIVE_CHILDREN, ORGANIZATION_HAS_ACTIVE_EMPLOYEES,
                 POSITION_IN_USE, ASSIGNMENT_CONFLICT, ORGANIZATION_HIERARCHY_INVALID
                 , ROLE_VERSION_CONFLICT, PROTECTED_ROLE_FORBIDDEN, ROLE_PERMISSION_INVALID,
                 ROLE_PERMISSION_LIMIT, ROLE_IN_USE, LAST_AVAILABLE_ADMIN_REQUIRED
                 , CREDENTIAL_ALREADY_USED, PASSWORD_HISTORY_REUSED
                 , SESSION_VERSION_CONFLICT, SESSION_REVOKED,
                 PROFILE_VERSION_CONFLICT, ACCOUNT_VERSION_CONFLICT
                 , USER_LIFECYCLE_CONFLICT, USER_HANDOVER_QUALIFICATION_CHANGED,
                 USER_HANDOVER_COUNT_MISMATCH, USER_HANDOVER_SCHEDULE_CONFLICT
                    -> HttpStatus.CONFLICT;
            case CREDENTIAL_INVALID -> HttpStatus.BAD_REQUEST;
            case CREDENTIAL_EXPIRED -> HttpStatus.GONE;
            case PASSWORD_POLICY_VIOLATION -> HttpStatus.UNPROCESSABLE_ENTITY;
            case CREDENTIAL_DELIVERY_FAILED -> HttpStatus.SERVICE_UNAVAILABLE;
            case CREDENTIAL_RATE_LIMITED -> HttpStatus.TOO_MANY_REQUESTS;
            case SESSION_CACHE_FAILED -> HttpStatus.SERVICE_UNAVAILABLE;
            case SESSION_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case SESSION_EXPIRED -> HttpStatus.GONE;
            case USER_LIFECYCLE_SNAPSHOT_EXPIRED -> HttpStatus.GONE;
            case ACCESS_DENIED, SELF_MANAGEMENT_FORBIDDEN,
                 ADMIN_BOOTSTRAP_REQUIRED, RECOVERY_ACCOUNT_BUSINESS_FORBIDDEN -> HttpStatus.FORBIDDEN;
            case AUTH_LOGIN_FAILED, UNAUTHORIZED_ERROR, TOKEN_ERROR, TOKEN_EXPIRED,
                 TOKEN_IS_EMPTY, TOKEN_IS_ERROR, TOKEN_IS_EXPIRED,
                 TOKEN_IS_NONE_MATCH -> HttpStatus.UNAUTHORIZED;
            case FAIL -> HttpStatus.INTERNAL_SERVER_ERROR;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}
