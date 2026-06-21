package com.autodealer.crm.result;

import lombok.Getter;

@Getter
public enum CodeEnum {
    OK(200, "操作成功"),
    FAIL(500, "操作失败"),
    PARAM_ERROR(501, "请求参数格式有误"),
    AUTH_LOGIN_FAILED(502, "账号或密码错误"),
    UNAUTHORIZED_ERROR(503, "没有访问权限"),
    TOKEN_ERROR(504, "token无效"),
    TOKEN_EXPIRED(505, "token已过期"),
    SYSTEM_ERROR(506, "系统异常"),

    // Token相关错误
    TOKEN_IS_EMPTY(510, "token为空"),
    TOKEN_IS_ERROR(511, "token无效"),
    TOKEN_IS_EXPIRED(512, "token已过期"),
    TOKEN_IS_NONE_MATCH(513, "token不匹配"),

    // 权限相关错误
    ACCESS_DENIED(520, "没有访问权限"),
    DATA_ACCESS_EXCEPTION(521, "数据访问异常"),

    // 通用业务错误
    DUPLICATE(409, "数据已存在"),
    NOT_FOUND(404, "资源不存在"),
    RESOURCE_IN_USE(422, "资源被引用，无法操作"),
    OPERATION_FAILED(550, "业务操作失败"),

    // 交易相关错误
    TRAN_STATE_CONFLICT(409, "交易状态冲突"),
    TRAN_NO_PRODUCTS(507, "交易没有产品信息"),

    // 用户相关
    USER_LOGOUT(200, "退出成功");

    private final int code;
    private final String msg;

    CodeEnum(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }
}
