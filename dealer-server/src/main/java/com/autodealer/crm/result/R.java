package com.autodealer.crm.result;

import lombok.Data;

/**
 * 统一封装web层向前端页面返回的结果
 *
 */
@Data
public class R<T> {

    //表示返回的结果码，比如200成功，500失败
    private Integer code;

    //表示返回的结果信息，比如 用户登录状态失效了，请求参数格式有误.......
    private String msg;

    //表示返回的结果数据，数据可能是一个对象，也可以是一个List集合.....
    private T data;

    public R() {}

    public R(Integer code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public R(Integer code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    public static <T> R<T> OK() {
        return new R<>(CodeEnum.OK.getCode(), CodeEnum.OK.getMsg());
    }

    public static <T> R<T> OK(T data) {
        return new R<>(CodeEnum.OK.getCode(), CodeEnum.OK.getMsg(), data);
    }

    public static <T> R<T> OK(String msg, T data) {
        return new R<>(CodeEnum.OK.getCode(), msg, data);
    }

    public static <T> R<T> FAIL() {
        return new R<>(CodeEnum.FAIL.getCode(), CodeEnum.FAIL.getMsg());
    }

    public static <T> R<T> FAIL(String msg) {
        return new R<>(CodeEnum.FAIL.getCode(), msg);
    }

    public static <T> R<T> FAIL(Integer code, String msg) {
        return new R<>(code, msg);
    }

    public static <T> R<T> FAIL(CodeEnum codeEnum) {
        return new R<>(codeEnum.getCode(), codeEnum.getMsg());
    }

    // 保留小写方法以保持向后兼容
    public static <T> R<T> ok() {
        return OK();
    }

    public static <T> R<T> ok(T data) {
        return OK(data);
    }

    public static <T> R<T> ok(String msg, T data) {
        return OK(msg, data);
    }

    public static <T> R<T> error() {
        return FAIL();
    }

    public static <T> R<T> error(String msg) {
        return FAIL(msg);
    }

    public static <T> R<T> error(Integer code, String msg) {
        return FAIL(code, msg);
    }

    public static <T> R<T> error(CodeEnum codeEnum) {
        return FAIL(codeEnum);
    }
}

