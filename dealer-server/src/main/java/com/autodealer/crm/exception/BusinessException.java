package com.autodealer.crm.exception;

import com.autodealer.crm.result.CodeEnum;

/**
 * 业务异常，携带稳定错误码和可选安全展示文案。
 *
 * <p>Service 层遇到无法正常处理的业务规则违反时抛出此异常，
 * 由 {@link com.autodealer.crm.config.handler.GlobalExceptionHandler}
 * 统一转换为 {@code R.FAIL(CodeEnum)} 响应。
 *
 * <p>禁止在 message 中携带用户输入、SQL、堆栈或内部路径信息。
 */
public class BusinessException extends RuntimeException {

    private final CodeEnum codeEnum;
    private final Object data;

    public BusinessException(CodeEnum codeEnum) {
        super(codeEnum.getMsg());
        this.codeEnum = codeEnum;
        this.data = null;
    }

    public BusinessException(CodeEnum codeEnum, String message) {
        super(message);
        this.codeEnum = codeEnum;
        this.data = null;
    }

    public BusinessException(CodeEnum codeEnum, String message, Object data) {
        super(message);
        this.codeEnum = codeEnum;
        this.data = data;
    }

    public BusinessException(CodeEnum codeEnum, String message, Throwable cause) {
        super(message, cause);
        this.codeEnum = codeEnum;
        this.data = null;
    }

    public CodeEnum getCodeEnum() {
        return codeEnum;
    }

    public Object getData() {
        return data;
    }
}
