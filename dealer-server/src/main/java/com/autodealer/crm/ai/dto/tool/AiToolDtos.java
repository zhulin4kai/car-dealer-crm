package com.autodealer.crm.ai.dto.tool;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class AiToolDtos {
    private AiToolDtos() {
    }

    @Data
    public static class PageRequest {
        @Min(value = 1, message = "页码不能小于 1")
        private Integer page = 1;

        @Min(value = 1, message = "每页数量不能小于 1")
        @Max(value = 20, message = "每页数量不能超过 20")
        private Integer size = 10;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class ListMyFollowupsRequest extends PageRequest {
        private String status;
        private Boolean overdueOnly;

        @Size(max = 64, message = "关键词不能超过 64 个字符")
        private String keyword;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class SearchCustomersRequest extends PageRequest {
        @Size(max = 64, message = "客户关键词不能超过 64 个字符")
        private String keyword;
    }

    @Data
    public static class GetCustomerProfileRequest {
        @NotNull(message = "客户 ID 不能为空")
        private Integer customerId;
    }

    @Data
    public static class ResolveVehicleProductRequest {
        private Long productId;

        @Size(max = 255, message = "SKU 不能超过 255 个字符")
        private String sku;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class GetInventoryAlertsRequest extends PageRequest {
        @Size(max = 255, message = "SKU 不能超过 255 个字符")
        private String sku;

        @Size(max = 255, message = "商品名称不能超过 255 个字符")
        private String name;

        private Long categoryId;
    }

    @Data
    public static class GetTransactionDetailRequest {
        @NotNull(message = "交易 ID 不能为空")
        private Integer tranId;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class ListPendingTransactionApprovalsRequest extends PageRequest {
    }

    @Data
    public static class CreateCommunicationRecordProposalRequest {
        private Long followTaskId;

        @NotBlank(message = "关联对象类型不能为空")
        private String relatedObjectType;

        @NotNull(message = "关联对象 ID 不能为空")
        private Long relatedObjectId;

        @NotBlank(message = "沟通方式不能为空")
        private String communicationMethod;

        private LocalDateTime communicationTime;

        @NotBlank(message = "沟通摘要不能为空")
        @Size(max = 500, message = "沟通摘要不能超过 500 个字符")
        private String summary;

        @Size(max = 500, message = "客户反馈不能超过 500 个字符")
        private String customerFeedback;

        @Size(max = 500, message = "下一步动作不能超过 500 个字符")
        private String nextAction;

        private LocalDateTime nextFollowTime;
        private Boolean createNextTask;
        private String nextTaskType;
        private String nextTaskTitle;
        private String nextTaskPriority;
        private LocalDateTime nextTaskDueTime;
        private LocalDateTime nextTaskRemindTime;
    }

    @Data
    public static class CreateFollowTaskProposalRequest {
        @NotBlank(message = "任务标题不能为空")
        @Size(max = 128, message = "任务标题不能超过 128 个字符")
        private String title;

        @NotBlank(message = "任务类型不能为空")
        private String taskType;

        @NotBlank(message = "关联对象类型不能为空")
        private String relatedObjectType;

        @NotNull(message = "关联对象 ID 不能为空")
        private Long relatedObjectId;

        private Integer ownerId;

        private String priority;

        @NotNull(message = "计划时间不能为空")
        private LocalDateTime dueTime;

        private LocalDateTime remindTime;
    }

    public record PageResult<T>(List<T> items, long total, int page, int size) {
    }

    public record FollowupItem(Long id, String title, String taskType, String relatedObjectType,
                               Long relatedObjectId, String relatedObjectName, String priority,
                               LocalDateTime dueTime, String status) {
    }

    public record CustomerSummary(Integer id, String customerName, String phoneMasked,
                                  String ownerName, String intentionProductName,
                                  String customerStatusName) {
    }

    public record CustomerProfile(Integer id, String customerName, String phoneMasked,
                                  String weixinMasked, String ownerName, String productName,
                                  String customerStatusName, String description,
                                  java.util.Date nextContactTime) {
    }

    public record ProductSummary(Long id, String sku, String name, String categoryName,
                                 String specification, BigDecimal price, Integer stock,
                                 Integer minStock, String status) {
    }

    public record InventoryAlert(Long id, String sku, String name, String categoryName,
                                 Integer stock, Integer minStock, String status) {
    }

    public record TransactionDetail(Integer id, String tranNo, Integer customerId, String customerName,
                                    BigDecimal money, String stage, String stageLabel,
                                    java.util.Date expectedDate, String description,
                                    List<TransactionProduct> products) {
    }

    public record TransactionProduct(Long productId, String productSku, String productName,
                                     String productSpecification, Integer quantity,
                                     BigDecimal price) {
    }

    public record PendingTransactionApproval(Integer id, String tranNo, String customerName,
                                             BigDecimal money, String stageLabel,
                                             java.util.Date createTime) {
    }

    public record ProposalCreated(Long proposalId, String proposalType, String riskLevel,
                                  String permissionCode, String relatedObjectType,
                                  String relatedObjectId, String paramsSummary,
                                  String impactSummary,
                                  LocalDateTime expiresTime) {
    }
}
