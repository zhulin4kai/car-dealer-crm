package com.autodealer.crm.config.handler;

import com.autodealer.crm.result.CodeEnum;
import com.autodealer.crm.result.R;
import org.springframework.dao.DataAccessException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import lombok.extern.slf4j.Slf4j;

/**
 * 统一异常处理类，controller发生了异常，统一用该类进行处理
 *
 */
@Slf4j
@RestControllerAdvice //aop。拦截标注了@RestController的controller中的所有方法
//@ControllerAdvice //aop。拦截标注了@Controller的controller中的所有方法
public class GlobalExceptionHandler {

    /**
     * 异常处理的方法（controller方法发生了异常，那么就使用该方法来处理）
     *
     * @return
     */
    @ExceptionHandler(value = Exception.class)
    public R handException(Exception e) {
        // 详细信息只记录到日志
        log.error("系统异常: {}", e.getMessage(), e);
        // 返回通用错误信息
        return R.FAIL("系统繁忙，请稍后重试");
    }

    /**
     * 业务异常处理（RuntimeException）
     */
    @ExceptionHandler(value = RuntimeException.class)
    public R handRuntimeException(RuntimeException e) {
        // 业务异常返回具体信息
        log.warn("业务异常: {}", e.getMessage());
        return R.FAIL(e.getMessage());
    }

    /**
     * 异常的精确匹配，先精确匹配，匹配不到了，就找父类的异常处理
     *
     * @param e
     * @return
     */
    @ExceptionHandler(value = DataAccessException.class)
    public R handException3(DataAccessException e) {
        // 详细信息只记录到日志
        log.error("数据访问异常: {}", e.getMessage(), e);
        return R.FAIL(CodeEnum.DATA_ACCESS_EXCEPTION);
    }

    /**
     * 权限不足的异常处理
     *
     * @param e
     * @return
     */
    @ExceptionHandler(value = AccessDeniedException.class)
    public R handException(AccessDeniedException e) {
        // 详细信息只记录到日志
        log.error("权限不足: {}", e.getMessage(), e);
        return R.FAIL(CodeEnum.ACCESS_DENIED);
    }

    @ExceptionHandler(value = HttpRequestMethodNotSupportedException.class)
    public R handException(HttpRequestMethodNotSupportedException e) {
        log.warn("不支持的请求方法: {}", e.getMethod());
        return R.FAIL("不支持的请求方法: " + e.getMethod());
    }

    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    public R handException(MethodArgumentNotValidException e) {
        log.warn("参数校验失败: {}", e.getMessage());
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .findFirst()
                .orElse("参数校验失败");
        return R.FAIL(message);
    }

    @ExceptionHandler(value = HttpMessageNotReadableException.class)
    public R handException(HttpMessageNotReadableException e) {
        log.warn("请求体格式错误: {}", e.getMessage());
        return R.FAIL("请求体格式错误或无法读取");
    }
}
