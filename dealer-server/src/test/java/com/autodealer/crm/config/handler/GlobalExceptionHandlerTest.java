package com.autodealer.crm.config.handler;

import com.autodealer.crm.exception.BusinessException;
import com.autodealer.crm.result.CodeEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.slf4j.LoggerFactory;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * GlobalExceptionHandler MockMvc 集成测试。
 *
 * 通过真实 HTTP 请求触发异常，同时断言 HTTP 状态码、$.code、$.msg。
 * 使用 standaloneSetup 避免加载完整 Spring 上下文。
 */
class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @RestController
    static class TestController {

        @GetMapping("/test/not-found")
        String notFound() {
            throw new BusinessException(CodeEnum.NOT_FOUND, "客户不存在");
        }

        @GetMapping("/test/operation-failed")
        String operationFailed() {
            throw new BusinessException(CodeEnum.OPERATION_FAILED, "库存不足");
        }

        @GetMapping("/test/duplicate")
        String duplicate() {
            throw new BusinessException(CodeEnum.DUPLICATE, "数据已存在");
        }

        @GetMapping("/test/tran-state-conflict")
        String tranStateConflict() {
            throw new BusinessException(CodeEnum.TRAN_STATE_CONFLICT, "交易状态不允许此操作");
        }

        @GetMapping("/test/tran-no-products")
        String tranNoProducts() {
            throw new BusinessException(CodeEnum.TRAN_NO_PRODUCTS, "交易没有产品信息");
        }

        @GetMapping("/test/resource-in-use")
        String resourceInUse() {
            throw new BusinessException(CodeEnum.RESOURCE_IN_USE, "资源被引用");
        }

        @GetMapping("/test/fail")
        String fail() {
            throw new BusinessException(CodeEnum.FAIL, "通用业务失败");
        }

        @GetMapping("/test/credential-rate-limited")
        String credentialRateLimited() {
            throw new BusinessException(CodeEnum.CREDENTIAL_RATE_LIMITED);
        }

        @PostMapping("/test/validation")
        String validation(@Valid @RequestBody ValidDto dto) {
            return "ok";
        }

        @PostMapping("/test/sensitive-validation")
        String sensitiveValidation(@Valid @RequestBody SensitiveDto dto) { return "ok"; }

        @GetMapping("/test/runtime")
        String runtime() {
            throw new RuntimeException("内部实现细节不应泄露");
        }

        @GetMapping("/test/generic")
        String generic() throws Exception {
            throw new Exception("未知异常细节");
        }
    }

    static class ValidDto {
        @NotBlank(message = "名称不能为空")
        private String name;
        @NotNull(message = "ID不能为空")
        private Integer id;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Integer getId() { return id; }
        public void setId(Integer id) { this.id = id; }
    }

    static class SensitiveDto {
        @Size(min=32,max=256) private String recoveryKey;
        public String getRecoveryKey(){return recoveryKey;}
        public void setRecoveryKey(String recoveryKey){this.recoveryKey=recoveryKey;}
    }

    // ==================== 业务异常 — HTTP 状态码 + $.code + $.msg ====================

    @Test
    void businessException_notFound_http404() throws Exception {
        mockMvc.perform(get("/test/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(CodeEnum.NOT_FOUND.getCode()))
                .andExpect(jsonPath("$.msg").value("客户不存在"));
    }

    @Test
    void businessException_operationFailed_http422() throws Exception {
        mockMvc.perform(get("/test/operation-failed"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value(CodeEnum.OPERATION_FAILED.getCode()))
                .andExpect(jsonPath("$.msg").value("库存不足"));
    }

    @Test
    void businessException_duplicate_http409() throws Exception {
        mockMvc.perform(get("/test/duplicate"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(CodeEnum.DUPLICATE.getCode()))
                .andExpect(jsonPath("$.msg").value("数据已存在"));
    }

    @Test
    void businessException_tranStateConflict_http409() throws Exception {
        mockMvc.perform(get("/test/tran-state-conflict"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(CodeEnum.TRAN_STATE_CONFLICT.getCode()))
                .andExpect(jsonPath("$.msg").value("交易状态不允许此操作"));
    }

    @Test
    void businessException_tranNoProducts_http422() throws Exception {
        mockMvc.perform(get("/test/tran-no-products"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value(CodeEnum.TRAN_NO_PRODUCTS.getCode()))
                .andExpect(jsonPath("$.msg").value("交易没有产品信息"));
    }

    @Test
    void businessException_resourceInUse_http422() throws Exception {
        mockMvc.perform(get("/test/resource-in-use"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value(CodeEnum.RESOURCE_IN_USE.getCode()))
                .andExpect(jsonPath("$.msg").value("资源被引用"));
    }

    @Test
    void businessException_fail_http500() throws Exception {
        mockMvc.perform(get("/test/fail"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(CodeEnum.FAIL.getCode()))
                .andExpect(jsonPath("$.msg").value("通用业务失败"));
    }

    @Test
    void credentialRateLimited_http429() throws Exception {
        mockMvc.perform(get("/test/credential-rate-limited"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value(CodeEnum.CREDENTIAL_RATE_LIMITED.getCode()));
    }

    // ==================== 参数校验异常 ====================

    @Test
    void validation_blankField_http400() throws Exception {
        mockMvc.perform(post("/test/validation")
                        .contentType(APPLICATION_JSON)
                        .content("{\"name\":\"\",\"id\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(CodeEnum.PARAM_ERROR.getCode()))
                .andExpect(jsonPath("$.msg").value(containsString("名称不能为空")));
    }

    @Test
    void validation_nullField_http400() throws Exception {
        mockMvc.perform(post("/test/validation")
                        .contentType(APPLICATION_JSON)
                        .content("{\"name\":\"ok\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(CodeEnum.PARAM_ERROR.getCode()))
                .andExpect(jsonPath("$.msg").value(containsString("ID不能为空")));
    }

    @Test
    void validationLogDoesNotContainRejectedRecoveryKey() throws Exception {
        String secret="real-recovery-key-must-not-log";
        Logger logger=(Logger)LoggerFactory.getLogger(GlobalExceptionHandler.class);
        ListAppender<ILoggingEvent> appender=new ListAppender<>();appender.start();logger.addAppender(appender);
        try {
            mockMvc.perform(post("/test/sensitive-validation").contentType(APPLICATION_JSON)
                            .content("{\"recoveryKey\":\""+secret+"\"}"))
                    .andExpect(status().isBadRequest());
            assertFalse(appender.list.stream().map(ILoggingEvent::getFormattedMessage)
                    .anyMatch(message->message.contains(secret)));
        } finally {logger.detachAppender(appender);appender.stop();}
    }

    // ==================== 未知异常 — 不泄露内部信息 ====================

    @Test
    void runtimeException_http500_safeMessage() throws Exception {
        mockMvc.perform(get("/test/runtime"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(CodeEnum.SYSTEM_ERROR.getCode()))
                .andExpect(jsonPath("$.msg").value("系统繁忙，请稍后重试"));
    }

    @Test
    void genericException_http500_safeMessage() throws Exception {
        mockMvc.perform(get("/test/generic"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(CodeEnum.SYSTEM_ERROR.getCode()))
                .andExpect(jsonPath("$.msg").value("系统繁忙，请稍后重试"));
    }
}
