package com.autodealer.crm.modules.ai.application.api.tool;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ToolSchemas {
    private ToolSchemas() {
    }

    public static Map<String, Object> forTool(String toolName) {
        return switch (toolName) {
            case "list_my_followups" -> object(Map.of(
                    "page", integer(1, null),
                    "size", integer(1, 20),
                    "status", string(null, 64),
                    "overdueOnly", bool(),
                    "keyword", string(null, 64)));
            case "search_customers" -> object(Map.of(
                    "page", integer(1, null),
                    "size", integer(1, 20),
                    "keyword", string(null, 64)));
            case "get_customer_profile" -> object(Map.of(
                    "customerId", integer(1, null)), "customerId");
            case "resolve_vehicle_product" -> object(Map.of(
                    "productId", integer(1, null),
                    "sku", string(null, 255)));
            case "get_inventory_alerts" -> object(Map.of(
                    "page", integer(1, null),
                    "size", integer(1, 20),
                    "sku", string(null, 255),
                    "name", string(null, 255),
                    "categoryId", integer(1, null)));
            case "get_transaction_detail" -> object(Map.of(
                    "tranId", integer(1, null)), "tranId");
            case "get_opportunity_detail" -> object(Map.of(
                    "opportunityId", integer(1, null)), "opportunityId");
            case "get_quote_detail" -> object(Map.of(
                    "quoteId", integer(1, null)), "quoteId");
            case "get_test_drive_detail" -> object(Map.of(
                    "testDriveId", integer(1, null)), "testDriveId");
            case "get_delivery_detail" -> object(Map.of(
                    "deliveryId", integer(1, null)), "deliveryId");
            case "get_business_overview" -> object(Map.of());
            case "list_pending_transaction_approvals" -> object(Map.of(
                    "page", integer(1, null),
                    "size", integer(1, 20)));
            case "create_communication_record_proposal" -> object(Map.ofEntries(
                    Map.entry("followTaskId", integer(1, null)),
                    Map.entry("relatedObjectType", stringEnum("CLUE", "CUSTOMER", "OPPORTUNITY", "TEST_DRIVE", "ORDER")),
                    Map.entry("relatedObjectId", integer(1, null)),
                    Map.entry("communicationMethod", stringEnum("PHONE", "STORE_VISIT", "WECHAT", "SMS", "EMAIL", "OTHER")),
                    Map.entry("communicationTime", string(null, 32)),
                    Map.entry("summary", string(1, 500)),
                    Map.entry("customerFeedback", string(null, 500)),
                    Map.entry("nextAction", string(null, 500)),
                    Map.entry("nextFollowTime", string(null, 32)),
                    Map.entry("createNextTask", bool()),
                    Map.entry("nextTaskType", followTaskType()),
                    Map.entry("nextTaskTitle", string(null, 128)),
                    Map.entry("nextTaskPriority", stringEnum("LOW", "NORMAL", "HIGH", "URGENT")),
                    Map.entry("nextTaskDueTime", string(null, 32)),
                    Map.entry("nextTaskRemindTime", string(null, 32))),
                    "relatedObjectType", "relatedObjectId", "communicationMethod", "summary");
            case "create_follow_task_proposal" -> object(Map.of(
                    "title", string(1, 128),
                    "taskType", followTaskType(),
                    "relatedObjectType", stringEnum("CLUE", "CUSTOMER", "OPPORTUNITY", "TEST_DRIVE", "ORDER"),
                    "relatedObjectId", integer(1, null),
                    "ownerId", integer(1, null),
                    "priority", stringEnum("LOW", "NORMAL", "HIGH", "URGENT"),
                    "dueTime", string(1, 32),
                    "remindTime", string(null, 32)),
                    "title", "taskType", "relatedObjectType", "relatedObjectId", "dueTime");
            default -> object(Map.of());
        };
    }

    private static Map<String, Object> object(Map<String, Object> properties, String... required) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", new LinkedHashMap<>(properties));
        schema.put("required", Arrays.asList(required));
        schema.put("additionalProperties", false);
        return schema;
    }

    private static Map<String, Object> string(Integer minLength, Integer maxLength) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "string");
        if (minLength != null) {
            schema.put("minLength", minLength);
        }
        if (maxLength != null) {
            schema.put("maxLength", maxLength);
        }
        return schema;
    }

    private static Map<String, Object> stringEnum(String... values) {
        Map<String, Object> schema = string(null, null);
        schema.put("enum", List.of(values));
        return schema;
    }

    private static Map<String, Object> integer(Integer minimum, Integer maximum) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "integer");
        if (minimum != null) {
            schema.put("minimum", minimum);
        }
        if (maximum != null) {
            schema.put("maximum", maximum);
        }
        return schema;
    }

    private static Map<String, Object> bool() {
        return Map.of("type", "boolean");
    }

    private static Map<String, Object> followTaskType() {
        return stringEnum(
                "FIRST_CONTACT",
                "PHONE_FOLLOW_UP",
                "STORE_INVITATION",
                "TEST_DRIVE_CONFIRM",
                "QUOTE_COMMUNICATION",
                "PRICE_NEGOTIATION",
                "CONTRACT_SIGN_REMINDER",
                "PAYMENT_REMINDER",
                "DELIVERY_CONFIRM",
                "POST_DELIVERY_FOLLOW_UP",
                "LONG_TERM_MAINTENANCE");
    }
}
