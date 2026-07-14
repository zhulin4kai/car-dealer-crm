package com.autodealer.crm.service.impl;

import com.autodealer.crm.audit.AuditActionEnum;
import com.autodealer.crm.audit.AuditSensitiveDataSanitizer;
import com.autodealer.crm.dto.user.UserHistoryDtos;
import com.autodealer.crm.dto.user.UserHistoryDtos.*;
import com.autodealer.crm.dto.user.UserHistoryRows.ActionFacet;
import com.autodealer.crm.dto.user.UserHistoryRows.ProjectionQuery;
import com.autodealer.crm.dto.user.UserHistoryRows.ProjectionRow;
import com.autodealer.crm.enums.AuthorizationChangeType;
import com.autodealer.crm.enums.AuthorizationSubjectType;
import com.autodealer.crm.exception.BusinessException;
import com.autodealer.crm.mapper.TUserMapper;
import com.autodealer.crm.mapper.UserHistoryProjectionMapper;
import com.autodealer.crm.model.TUser;
import com.autodealer.crm.result.CodeEnum;
import com.autodealer.crm.service.UserHistoryService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.*;
import java.util.*;
import java.util.regex.Pattern;

/** 用户视角的只读历史投影；不会返回原始审计实体或 detail。 */
@Service
public class UserHistoryServiceImpl implements UserHistoryService {
    private static final int MAX_SIZE = 100;
    private static final String AUTHORIZATION_SOURCE = "AUTHORIZATION_HISTORY";
    private static final String OPERATION_SOURCE = "OPERATION_LOG";
    private static final String LIFECYCLE_SOURCE = "USER_LIFECYCLE_EVENT";
    private static final Pattern FORBIDDEN_FIELD = Pattern.compile(
            "(?i)password|passwd|pwd|hash|digest|token|secret|credential|cookie|session.?id|phone|mobile|email|raw|detail|payload|context|headers?|request|response|body|ip(?:address)?|contact|address|key|signature|salt|nonce");
    private static final Set<String> OPERATION_ACTIONS = Set.of(
            "USER_CREATE", "USER_UPDATE", "USER_STATUS_CHANGE", "USER_MANUAL_LOCK_CHANGE",
            "USER_LOGIN_ACCOUNT_CHANGE", "USER_SECURITY_EXPIRATION_CHANGE",
            "USER_PROFILE_CHANGE", "USER_PROFILE_UPDATE", "USER_PASSWORD_CHANGE",
            "USER_PASSWORD_RESET_ISSUE", "USER_ACTIVATION", "USER_SESSION_REVOKE",
            "USER_SESSION_SECURITY_REVOKE", "USER_SESSION_CREATE", "USER_INVITATION_ISSUE",
            "USER_CREDENTIAL_ISSUE", "USER_CREDENTIAL_CONSUME",
            "USER_CREDENTIAL_DELIVERY_SUCCESS", "USER_CREDENTIAL_DELIVERY_FAILURE",
            "USER_DEGRADED_ADMIN_RECOVERY", "USER_LOGIN_AUTO_LOCK",
            "USER_LOGIN_AUTO_LOCK_BYPASSED", "USER_CONTACT_VERIFICATION_ISSUE",
            "USER_CONTACT_VERIFICATION_COMPLETE", "USER_HANDOVER");
    private static final Set<String> LIFECYCLE_ACTIONS = Set.of(
            "USER_TRANSFER","USER_DEPARTURE_START","USER_HANDOVER_CONFIRM","USER_DEPARTURE_COMPLETE","USER_REHIRE");
    private static final Set<String> SECURITY_OPERATION_ACTIONS = Set.of(
            "USER_PASSWORD_CHANGE", "USER_PASSWORD_RESET_ISSUE", "USER_ACTIVATION",
            "USER_SESSION_REVOKE", "USER_SESSION_SECURITY_REVOKE", "USER_SESSION_CREATE",
            "USER_INVITATION_ISSUE", "USER_CREDENTIAL_ISSUE", "USER_CREDENTIAL_CONSUME",
            "USER_CREDENTIAL_DELIVERY_SUCCESS", "USER_CREDENTIAL_DELIVERY_FAILURE",
            "USER_LOGIN_AUTO_LOCK", "USER_LOGIN_AUTO_LOCK_BYPASSED",
            "USER_CONTACT_VERIFICATION_ISSUE", "USER_CONTACT_VERIFICATION_COMPLETE",
            "USER_MANUAL_LOCK_CHANGE", "USER_LOGIN_ACCOUNT_CHANGE", "USER_SECURITY_EXPIRATION_CHANGE",
            "USER_DEGRADED_ADMIN_RECOVERY");
    private static final Map<AuthorizationSubjectType, Set<AuthorizationChangeType>> VALID_AUTHORIZATION_ACTIONS = Map.ofEntries(
            Map.entry(AuthorizationSubjectType.ROLE, EnumSet.of(AuthorizationChangeType.CREATE,
                    AuthorizationChangeType.UPDATE, AuthorizationChangeType.ENABLE, AuthorizationChangeType.DISABLE)),
            Map.entry(AuthorizationSubjectType.ROLE_PERMISSION, EnumSet.of(AuthorizationChangeType.GRANT,
                    AuthorizationChangeType.REVOKE, AuthorizationChangeType.UPDATE)),
            Map.entry(AuthorizationSubjectType.USER_ROLE, EnumSet.of(AuthorizationChangeType.ASSIGN,
                    AuthorizationChangeType.UNASSIGN, AuthorizationChangeType.REVOKE)),
            Map.entry(AuthorizationSubjectType.USER_PERMISSION, EnumSet.of(AuthorizationChangeType.GRANT,
                    AuthorizationChangeType.DENY, AuthorizationChangeType.REVOKE)),
            Map.entry(AuthorizationSubjectType.ORGANIZATION_UNIT, EnumSet.of(AuthorizationChangeType.CREATE,
                    AuthorizationChangeType.UPDATE, AuthorizationChangeType.ENABLE, AuthorizationChangeType.DISABLE)),
            Map.entry(AuthorizationSubjectType.POSITION, EnumSet.of(AuthorizationChangeType.CREATE,
                    AuthorizationChangeType.UPDATE, AuthorizationChangeType.ENABLE, AuthorizationChangeType.DISABLE)),
            Map.entry(AuthorizationSubjectType.ORGANIZATION_ASSIGNMENT, EnumSet.of(AuthorizationChangeType.CREATE,
                    AuthorizationChangeType.UPDATE, AuthorizationChangeType.EXPIRE)),
            Map.entry(AuthorizationSubjectType.REPORTING_RELATION, EnumSet.of(AuthorizationChangeType.CREATE,
                    AuthorizationChangeType.UPDATE, AuthorizationChangeType.EXPIRE)));

    private final TUserMapper users;
    private final UserHistoryProjectionMapper history;
    private final UserAuthorizationPolicy policy;
    private final ObjectMapper json;

    public UserHistoryServiceImpl(TUserMapper users, UserHistoryProjectionMapper history,
                                  UserAuthorizationPolicy policy,
                                  ObjectMapper json) {
        this.users = users;
        this.history = history;
        this.policy = policy;
        this.json = json;
    }

    @Override
    @Transactional(readOnly = true)
    public UserHistoryDtos.Collection getUserHistory(Integer userId, Query query) {
        Query safeQuery = query == null ? new Query() : query;
        validate(safeQuery);
        TUser targetUser = users.selectByPrimaryKey(userId);
        if (targetUser == null) throw new BusinessException(CodeEnum.NOT_FOUND, "目标用户不存在");
        // 历史比一般用户详情更敏感：本人也不能查询，必须同时满足审计权限和管理范围。
        policy.requireManage(targetUser);

        ProjectionQuery projection = projectionQuery(userId, safeQuery);
        long total = history.count(projection);
        List<Item> page = total == 0 ? List.of() : history.selectPage(projection).stream()
                .map(row -> map(row, targetUser))
                .toList();
        List<ActionOption> options = actionOptions(history.selectActionFacets(projection));
        UserHistoryDtos.Collection result = new UserHistoryDtos.Collection();
        result.setPageNum(safeQuery.getPage());
        result.setPageSize(safeQuery.getSize());
        result.setTotal(total);
        result.setPages(total == 0 ? 0 : (int) ((total + safeQuery.getSize() - 1) / safeQuery.getSize()));
        result.setList(new ArrayList<>(page));
        result.setSize(page.size());
        result.setActionOptions(new ArrayList<>(options));
        result.setAllowedActions(List.of("VIEW"));
        result.setUnavailableReasons(new LinkedHashMap<>());
        return result;
    }

    private ProjectionQuery projectionQuery(Integer userId, Query query) {
        ProjectionQuery projection = new ProjectionQuery();
        projection.setUserId(userId);
        projection.setResourceId(String.valueOf(userId));
        projection.setOperationActionCodes(OPERATION_ACTIONS.stream().sorted().toList());
        projection.setLifecycleActions(LIFECYCLE_ACTIONS.stream()
                .map(action -> action.substring("USER_".length()))
                .sorted().toList());
        projection.setStartTime(local(query.getStartTime()));
        projection.setEndTime(local(query.getEndTime()));
        projection.setOffset(Math.multiplyExact((long) query.getPage() - 1L, query.getSize()));
        projection.setLimit(query.getSize());
        applyActionFilter(projection, query.getActionCode());
        return projection;
    }

    private void applyActionFilter(ProjectionQuery projection, String actionCode) {
        if (!StringUtils.hasText(actionCode)) return;
        if (OPERATION_ACTIONS.contains(actionCode)) {
            projection.setFilterSource(OPERATION_SOURCE);
            projection.setFilterActionCode(actionCode);
            return;
        }
        if (LIFECYCLE_ACTIONS.contains(actionCode)) {
            projection.setFilterSource(LIFECYCLE_SOURCE);
            projection.setFilterActionCode(actionCode.substring("USER_".length()));
            return;
        }
        for (Map.Entry<AuthorizationSubjectType, Set<AuthorizationChangeType>> entry
                : VALID_AUTHORIZATION_ACTIONS.entrySet()) {
            for (AuthorizationChangeType change : entry.getValue()) {
                if (actionCode.equals(authorizationActionCode(entry.getKey(), change))) {
                    projection.setFilterSource(AUTHORIZATION_SOURCE);
                    projection.setFilterSubjectType(entry.getKey());
                    projection.setFilterChangeType(change);
                    return;
                }
            }
        }
        throw new BusinessException(CodeEnum.PARAM_ERROR, "历史动作编码不合法");
    }

    private List<ActionOption> actionOptions(List<ActionFacet> facets) {
        Map<String, String> options = new TreeMap<>();
        for (ActionFacet facet : facets) {
            String code;
            String name;
            if (AUTHORIZATION_SOURCE.equals(facet.getSourceKey())) {
                code = authorizationActionCode(facet.getSubjectType(), facet.getChangeType());
                name = authorizationActionName(facet.getSubjectType(), facet.getChangeType());
            } else {
                code = facet.getActionCode();
                name = auditActionName(code);
            }
            if (StringUtils.hasText(code)) options.putIfAbsent(code, name);
        }
        return options.entrySet().stream()
                .map(entry -> new ActionOption(entry.getKey(), entry.getValue()))
                .toList();
    }

    private Item map(ProjectionRow row, TUser targetUser) {
        return switch (row.getSourceKey()) {
            case AUTHORIZATION_SOURCE -> mapAuthorization(row);
            case OPERATION_SOURCE -> mapOperation(row, targetUser);
            case LIFECYCLE_SOURCE -> mapLifecycle(row, targetUser);
            default -> throw new IllegalStateException("未知用户历史来源: " + row.getSourceKey());
        };
    }

    private void validate(Query query) {
        if (query.getPage() == null || query.getPage() < 1 || query.getSize() == null
                || query.getSize() < 1 || query.getSize() > MAX_SIZE) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "分页参数不合法");
        }
        if (query.getStartTime() != null && query.getEndTime() != null
                && query.getStartTime().isAfter(query.getEndTime())) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "开始时间不能晚于结束时间");
        }
        if (StringUtils.hasText(query.getActionCode()) && !knownActionCodes().contains(query.getActionCode())) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "历史动作编码不合法");
        }
    }

    private Set<String> knownActionCodes() {
        Set<String> codes = new HashSet<>(OPERATION_ACTIONS);
        codes.addAll(LIFECYCLE_ACTIONS);
        for (Map.Entry<AuthorizationSubjectType, Set<AuthorizationChangeType>> entry
                : VALID_AUTHORIZATION_ACTIONS.entrySet()) {
            for (AuthorizationChangeType change : entry.getValue()) {
                codes.add(authorizationActionCode(entry.getKey(), change));
            }
        }
        return codes;
    }

    private Item mapAuthorization(ProjectionRow row) {
        String actionCode = authorizationActionCode(row.getSubjectType(), row.getChangeType());
        String categoryCode = authorizationCategory(row.getSubjectType());
        JsonNode before = parse(row.getBeforeValue());
        JsonNode after = parse(row.getAfterValue());
        TargetSummary target = authorizationTarget(row, before, after);
        return new Item("authorization:" + row.getId(), "AUTHORIZATION_HISTORY",
                actionCode, authorizationActionName(row.getSubjectType(), row.getChangeType()),
                categoryCode, categoryName(categoryCode), target,
                new OperatorSummary(row.getOperatorId(), safe(row.getOperatorName(), "系统"), safe(row.getOperatorEmployeeNo(), null)),
                safeFields(before), safeFields(after), safe(row.getReason(), null),
                offset(row.getEffectiveFrom()), offset(row.getEffectiveTo()), "SUCCESS", "成功", null,
                offset(row.getOccurredTime()));
    }

    private Item mapOperation(ProjectionRow row, TUser targetUser) {
        AuditActionEnum action;
        try { action = AuditActionEnum.fromActionCode(row.getActionCode()); }
        catch (IllegalArgumentException ignored) { action = null; }
        JsonNode wrapper = parse(row.getDetail());
        JsonNode summary = wrapper.has("summary") ? wrapper.get("summary") : wrapper;
        String category = operationCategory(row.getActionCode());
        String actionName = action == null ? row.getActionCode() : action.getActionName();
        TargetSummary target = new TargetSummary("USER", "用户", targetUser.getId(),
                safe(targetUser.getLoginAct(), null), safe(targetUser.getName(), null));
        String reason = text(summary, "reason");
        String resultCode = "SUCCESS".equalsIgnoreCase(row.getResult()) ? "SUCCESS" : "FAILURE";
        return new Item("operation:" + row.getId(), "OPERATION_LOG", row.getActionCode(), safe(actionName, row.getActionCode()),
                category, categoryName(category), target,
                new OperatorSummary(row.getOperatorId(), safe(row.getOperatorName(), "系统"), safe(row.getOperatorEmployeeNo(), null)),
                safeFields(summary == null ? null : summary.get("before")),
                safeFields(summary == null ? null : summary.get("after")), reason,
                null, null, resultCode, "SUCCESS".equals(resultCode) ? "成功" : "失败",
                batch(summary, resultCode), offset(row.getOccurredTime()));
    }

    private Item mapLifecycle(ProjectionRow row,TUser targetUser){String actionCode=row.getActionCode();AuditActionEnum action=AuditActionEnum.fromActionCode(actionCode);
        TargetSummary target=new TargetSummary("USER_LIFECYCLE","人员生命周期",targetUser.getId(),safe(targetUser.getLoginAct(),null),safe(targetUser.getName(),null));
        return new Item("lifecycle:"+row.getId(),"USER_LIFECYCLE_EVENT",actionCode,action.getActionName(),"ORGANIZATION","组织任职",target,
                new OperatorSummary(row.getOperatorId(),safe(row.getOperatorName(),"系统"),safe(row.getOperatorEmployeeNo(),null)),
                safeFields(parse(row.getBeforeValue())),safeFields(parse(row.getAfterValue())),safe(row.getReason(),null),null,null,
                "SUCCESS","成功",null,offset(row.getOccurredTime()));}

    private TargetSummary authorizationTarget(ProjectionRow row, JsonNode before, JsonNode after) {
        JsonNode source = after != null && !after.isNull() ? after : before;
        String type = row.getSubjectType() == null ? "AUTHORIZATION" : row.getSubjectType().name();
        JsonNode nested = source;
        if (source != null && type.equals("ORGANIZATION_ASSIGNMENT") && source.has("organizationUnit")) nested = source.get("organizationUnit");
        if (source != null && type.equals("REPORTING_RELATION") && source.has("manager")) nested = source.get("manager");
        Integer id = switch (type) {
            case "ROLE", "USER_ROLE" -> firstInt(nested, "roleId", row.getRoleId());
            case "USER_PERMISSION", "ROLE_PERMISSION" -> firstInt(nested, "permissionId", row.getPermissionId());
            case "ORGANIZATION_UNIT", "ORGANIZATION_ASSIGNMENT" -> firstInt(nested, "id", firstInt(nested, "organizationUnitId", integer(row.getSubjectId())));
            case "POSITION" -> firstInt(nested, "positionId", integer(row.getSubjectId()));
            case "REPORTING_RELATION" -> firstInt(nested, "id", firstInt(nested, "managerEmployeeId", integer(row.getSubjectId())));
            default -> integer(row.getSubjectId());
        };
        String code = firstText(nested, type.contains("PERMISSION") ? "permissionCode" : type.contains("ROLE") ? "roleCode"
                : type.contains("POSITION") ? "positionCode" : type.contains("REPORTING") ? "managerEmployeeNo" : "organizationUnitCode", "code");
        String name = firstText(nested, type.contains("PERMISSION") ? "permissionName" : type.contains("ROLE") ? "roleName"
                : type.contains("POSITION") ? "positionName" : type.contains("REPORTING") ? "managerName" : "organizationUnitName", "name");
        return new TargetSummary(type, subjectName(type), id, safe(code, null), safe(name, null));
    }

    private List<ValueField> safeFields(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) return List.of();
        List<ValueField> fields = new ArrayList<>();
        if (node.isObject()) {
            node.fields().forEachRemaining(entry -> addSafeField(fields, entry.getKey(), entry.getValue()));
        } else if (node.isArray()) {
            int index = 0;
            for (JsonNode item : node) addSafeField(fields, "item" + (++index), item);
        }
        return fields;
    }

    private void addSafeField(List<ValueField> fields, String code, JsonNode value) {
        if (!StringUtils.hasText(code) || FORBIDDEN_FIELD.matcher(code).find() || value == null || value.isNull()) return;
        if (value.isValueNode()) {
            fields.add(new ValueField(code, fieldLabel(code), code.endsWith("Code") ? safe(value.asText(), null) : null,
                    null, code.endsWith("Code") ? null : safe(value.asText(), null)));
            return;
        }
        if (value.isObject()) {
            String stableCode = firstText(value, "code", "roleCode", "permissionCode", "organizationUnitCode", "positionCode", "employeeNo");
            String stableName = firstText(value, "name", "roleName", "permissionName", "organizationUnitName", "positionName", "managerName");
            if (stableCode != null || stableName != null) {
                fields.add(new ValueField(code, fieldLabel(code), safe(stableCode, null), safe(stableName, null), null));
                return;
            }
            value.fields().forEachRemaining(child -> addSafeField(fields, code + "." + child.getKey(), child.getValue()));
        } else if (value.isArray()) {
            List<String> names = new ArrayList<>();
            for (JsonNode item : value) {
                String name = item.isValueNode() ? item.asText() : firstText(item, "name", "roleName", "permissionName", "organizationUnitName", "positionName");
                if (name != null) names.add(safe(name, null));
            }
            if (!names.isEmpty()) fields.add(new ValueField(code, fieldLabel(code), null, null, String.join("、", names)));
        }
    }

    private BatchSummary batch(JsonNode node, String resultCode) {
        if (node == null || !node.isObject() || !node.hasNonNull("batchId")) return null;
        String id = safe(node.get("batchId").asText(), null);
        if (id == null) return null;
        return new BatchSummary(id, integer(node, "totalCount"), integer(node, "successCount"), integer(node, "failureCount"),
                textOr(node, "targetResultCode", resultCode), textOr(node, "targetResultName", "SUCCESS".equals(resultCode) ? "成功" : "失败"));
    }

    private JsonNode parse(String value) {
        if (!StringUtils.hasText(value)) return null;
        try { return json.readTree(value); }
        catch (Exception ignored) { return null; }
    }

    private static String authorizationActionCode(AuthorizationSubjectType subject, AuthorizationChangeType change) {
        String subjectCode = subject == null ? "AUTHORIZATION" : subject.name();
        String suffix = change == null ? "UPDATED" : switch (change) {
            case CREATE -> "CREATED"; case UPDATE -> "UPDATED"; case ENABLE -> "ENABLED"; case DISABLE -> "DISABLED";
            case ASSIGN -> "ASSIGNED"; case UNASSIGN -> "UNASSIGNED"; case GRANT -> "GRANTED";
            case DENY -> "DENIED"; case REVOKE -> "REVOKED"; case EXPIRE -> "EXPIRED";
        };
        return subjectCode + "_" + suffix;
    }
    private static String authorizationActionName(AuthorizationSubjectType subject, AuthorizationChangeType change) {
        return subjectName(subject == null ? "AUTHORIZATION" : subject.name()) + switch (change == null ? AuthorizationChangeType.UPDATE : change) {
            case CREATE -> "创建"; case UPDATE -> "调整"; case ENABLE -> "启用"; case DISABLE -> "停用";
            case ASSIGN -> "分配"; case UNASSIGN -> "移除"; case GRANT -> "授予"; case DENY -> "拒绝";
            case REVOKE -> "撤销"; case EXPIRE -> "到期";
        };
    }
    private static String authorizationCategory(AuthorizationSubjectType subject) {
        if (subject == AuthorizationSubjectType.ORGANIZATION_UNIT || subject == AuthorizationSubjectType.POSITION
                || subject == AuthorizationSubjectType.ORGANIZATION_ASSIGNMENT || subject == AuthorizationSubjectType.REPORTING_RELATION) return "ORGANIZATION";
        return "AUTHORIZATION";
    }
    private static String operationCategory(String action) {
        if (SECURITY_OPERATION_ACTIONS.contains(action)) return "SECURITY";
        if ("USER_HANDOVER".equals(action)) return "ORGANIZATION";
        return "ACCOUNT";
    }
    private static String auditActionName(String actionCode) {
        try { return AuditActionEnum.fromActionCode(actionCode).getActionName(); }
        catch (IllegalArgumentException ignored) { return actionCode; }
    }
    private static String categoryName(String category) { return switch (category) { case "AUTHORIZATION" -> "授权"; case "ORGANIZATION" -> "组织任职"; case "SECURITY" -> "账号安全"; default -> "账号资料"; }; }
    private static String subjectName(String subject) { return switch (subject) {
        case "ROLE", "USER_ROLE" -> "角色"; case "ROLE_PERMISSION", "USER_PERMISSION" -> "权限";
        case "ORGANIZATION_UNIT" -> "组织"; case "POSITION" -> "岗位";
        case "ORGANIZATION_ASSIGNMENT" -> "任职"; case "REPORTING_RELATION" -> "直属管理者"; default -> "授权事实";
    }; }
    private static String fieldLabel(String code) { return switch (code) {
        case "role", "roleName" -> "角色"; case "permission", "permissionName" -> "权限";
        case "loginAct" -> "登录账号"; case "accountStatus" -> "账号状态"; case "lockStatus", "manualLocked" -> "锁定状态";
        case "organizationUnit", "organizationUnitName" -> "组织"; case "position", "positionName" -> "岗位";
        case "manager", "managerName" -> "直属管理者"; case "name" -> "姓名"; case "changedFieldCodes" -> "变更字段";
        default -> code;
    }; }
    private static String safe(String value, String fallback) {
        if (!StringUtils.hasText(value)) return fallback;
        String sanitized = AuditSensitiveDataSanitizer.sanitize(value);
        return FORBIDDEN_FIELD.matcher(sanitized).find() && sanitized.equals(value) ? "[已隐藏敏感内容]" : sanitized;
    }
    private static String text(JsonNode node, String key) { return node != null && node.hasNonNull(key) ? safe(node.get(key).asText(), null) : null; }
    private static String textOr(JsonNode node, String key, String fallback) { String value = text(node, key); return value == null ? fallback : value; }
    private static int integer(JsonNode node, String key) { return node != null && node.hasNonNull(key) ? node.get(key).asInt() : 0; }
    private static Integer integer(String value) { try { return value == null ? null : Integer.valueOf(value); } catch (NumberFormatException ignored) { return null; } }
    private static Integer firstInt(JsonNode node, String key, Integer fallback) { return node != null && node.hasNonNull(key) && node.get(key).canConvertToInt() ? node.get(key).asInt() : fallback; }
    private static String firstText(JsonNode node, String... keys) { if (node == null) return null; for (String key : keys) if (node.hasNonNull(key)) return node.get(key).asText(); return null; }
    private static LocalDateTime local(OffsetDateTime value) { return value == null ? null : value.atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime(); }
    private static OffsetDateTime offset(LocalDateTime value) { return value == null ? null : value.atZone(ZoneId.systemDefault()).toOffsetDateTime(); }
}
