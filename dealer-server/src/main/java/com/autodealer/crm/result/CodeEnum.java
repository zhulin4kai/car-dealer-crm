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

    // AI 业务助手相关错误
    AI_RUN_NOT_FOUND(560, "AI 运行不存在或无权访问"),
    AI_RUN_FINISHED(561, "AI 运行已结束"),
    AI_SSE_FAILED(562, "AI 事件连接失败"),
    AI_PROVIDER_FAILED(563, "AI 服务调用失败"),
    AI_TOOL_NOT_FOUND(564, "AI 工具不存在"),
    AI_TOOL_FORBIDDEN(565, "AI 工具无权限"),
    AI_TOOL_ARGUMENT_INVALID(566, "AI 工具参数错误"),
    AI_PROPOSAL_EXPIRED(567, "AI 提议已过期"),
    AI_PROPOSAL_HASH_MISMATCH(568, "AI 提议参数校验失败"),
    AI_WORKFLOW_NOT_FOUND(569, "AI 工作流不存在或无权访问"),
    AI_WORKFLOW_STATE_CONFLICT(570, "AI 工作流状态冲突"),
    AI_PROACTIVE_SUBSCRIPTION_NOT_FOUND(571, "AI 主动提醒订阅不存在或无权访问"),
    AI_PROACTIVE_EVENT_NOT_FOUND(572, "AI 主动提醒事件不存在或无权访问"),
    AI_PROACTIVE_STATE_CONFLICT(573, "AI 主动提醒状态冲突"),
    AI_PROACTIVE_FORBIDDEN(574, "AI 主动提醒无权限"),
    AI_PROVIDER_CONFIG_NOT_FOUND(575, "AI 模型配置不存在"),
    AI_PROVIDER_CONFIG_DISABLED(576, "AI 模型配置未启用"),
    AI_PROVIDER_CONFIG_REQUIRED(577, "AI 模型配置缺失"),
    AI_PROVIDER_CONFIG_TEST_FAILED(578, "AI 模型配置测试失败"),
    AI_PROVIDER_KEY_ENCRYPTION_FAILED(579, "AI 模型密钥加密失败"),
    AI_PROVIDER_KEY_DECRYPTION_FAILED(580, "AI 模型密钥解密失败"),
    AI_RUN_CANCELLED(581, "AI 运行已取消"),
    AI_RUN_CANCEL_CONFLICT(582, "AI 运行取消状态冲突"),
    AI_DEALER_AI_UNAVAILABLE(583, "AI 编排服务不可用"),
    AI_PROVIDER_UNSUPPORTED_FORMAT(584, "AI 模型协议不支持"),
    AI_CONVERSATION_NOT_FOUND(585, "AI 会话不存在或无权访问"),
    AI_CONVERSATION_ARCHIVED(586, "AI 会话已归档"),

    // 用户相关
    USER_LOGOUT(200, "退出成功");

    private final int code;
    private final String msg;

    CodeEnum(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }
}
