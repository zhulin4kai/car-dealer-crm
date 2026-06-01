package com.autodealer.crm.result;

import lombok.Getter;

@Getter
public enum CodeEnum {
    OK(200, "操作成功"),
    FAIL(500, "操作失败"),
    PARAM_ERROR(501, "请求参数格式有误"),
    LOGIN_ERROR(502, "登录失败"),
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
    
    // 用户相关
    USER_LOGOUT(200, "退出成功");

    private final int code;
    private final String msg;

    CodeEnum(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }
}
