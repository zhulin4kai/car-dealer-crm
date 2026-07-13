package com.autodealer.crm.config.security;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OwnerCandidateCacheInvalidationWiringTest {
    private static final Path IMPLEMENTATION_ROOT = Path.of("src/main/java/com/autodealer/crm/service/impl");
    private static final String INVALIDATE = "ownerCandidateCacheInvalidator.invalidateAfterCommit()";

    @Test
    void everyOwnerQualificationFactMutationReachesTheCentralInvalidator() throws Exception {
        String accounts = source("ManagedUserAccountServiceImpl");
        assertMethodContains(accounts, "public Detail changeStatus(", INVALIDATE);
        assertMethodDoesNotContain(accounts, "public Detail changeSecurityExpiration(", INVALIDATE);

        String authorization = source("AuthorizationServiceImpl");
        assertMethodContains(authorization, "public Detail replaceRoles(", "scheduleCleanup(userId)");
        assertMethodContains(authorization, "public Detail updatePermissions(", "scheduleCleanup(userId)");
        assertMethodContains(authorization, "public BatchResult batchUpdateRoles(", "finishBatch(");
        assertMethodContains(authorization, "public BatchResult batchUpdatePermissions(", "finishBatch(");
        assertMethodContains(authorization, "private BatchResult finishBatch(", "changedUserIds.forEach(this::scheduleCleanup)");
        assertMethodContains(authorization, "private void scheduleCleanup(", INVALIDATE);

        String roles = source("RoleAccessServiceImpl");
        assertMethodContains(roles, "public RoleResponse update(", "invalidateRoleMembers(id)");
        assertMethodContains(roles, "public RoleResponse status(", "invalidateRoleMembers(id)");
        assertMethodContains(roles, "public UpdateMatrixResponse updateMatrix(", "scheduleCleanup(users)");
        assertMethodContains(roles, "private void scheduleCleanup(", INVALIDATE);

        String organization = source("OrganizationServiceImpl");
        assertMethodContains(organization, "public OrganizationUnitResponse updateOrganizationUnit(", INVALIDATE);
        assertMethodContains(organization, "public OrganizationUnitResponse changeOrganizationUnitStatus(", INVALIDATE);
        assertMethodContains(organization, "public PositionResponse changePositionStatus(", INVALIDATE);
        assertMethodContains(organization, "public EmployeeOrganizationMembershipResponse updateEmployeeOrganizationMembership(",
                "scheduleAssignmentSecurityCleanup(employee.getUserId())");
        assertMethodContains(organization, "public ActingReportingCollectionResponse replaceActingReportings(",
                "scheduleAssignmentSecurityCleanup(employee.getUserId())");
        assertMethodContains(organization, "private void scheduleAssignmentSecurityCleanup(", INVALIDATE);

        String lifecycle = source("UserLifecycleServiceImpl");
        assertMethodContains(lifecycle, "public Context transfer(", INVALIDATE);
        assertMethodContains(lifecycle, "public Context startDeparture(", INVALIDATE);
        assertMethodContains(lifecycle, "public Context completeDeparture(", INVALIDATE);
        assertMethodContains(lifecycle, "public RehireResult rehire(", INVALIDATE);

        String credentials = source("CredentialServiceImpl");
        assertMethodContains(credentials, "public CommandResult activate(", INVALIDATE);
    }

    @Test
    void ownerPatternDeletionIsCentralizedAndLoginNoiseDoesNotTriggerIt() throws Exception {
        String directDeletion = "deletePattern(RedisKeys.ownerListPattern())";
        try (var files = Files.walk(Path.of("src/main/java"))) {
            files.filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> !path.endsWith("OwnerCandidateCacheInvalidator.java"))
                    .forEach(path -> assertFileDoesNotContain(path, directDeletion));
        }
        assertFalse(source("LoginSecurityServiceImpl").contains("OwnerCandidateCacheInvalidator"),
                "登录成功或自动锁定不是当前负责人候选 SQL 的资格事实，禁止高频全量失效");
    }

    private String source(String simpleName) throws Exception {
        return Files.readString(IMPLEMENTATION_ROOT.resolve(simpleName + ".java"));
    }

    private void assertMethodContains(String source, String anchor, String expected) {
        assertTrue(methodBody(source, anchor).contains(expected), anchor + " 缺少 " + expected);
    }

    private void assertMethodDoesNotContain(String source, String anchor, String forbidden) {
        assertFalse(methodBody(source, anchor).contains(forbidden), anchor + " 不应包含 " + forbidden);
    }

    private String methodBody(String source, String anchor) {
        int signature = source.indexOf(anchor);
        assertTrue(signature >= 0, "找不到方法: " + anchor);
        int openingBrace = source.indexOf('{', signature);
        assertTrue(openingBrace >= 0, "找不到方法体: " + anchor);
        int depth = 0;
        for (int index = openingBrace; index < source.length(); index++) {
            char current = source.charAt(index);
            if (current == '{') depth++;
            if (current == '}' && --depth == 0) return source.substring(openingBrace, index + 1);
        }
        throw new AssertionError("方法体未闭合: " + anchor);
    }

    private void assertFileDoesNotContain(Path path, String forbidden) {
        try {
            assertFalse(Files.readString(path).contains(forbidden), path + " 绕过了统一失效入口");
        } catch (java.io.IOException exception) {
            throw new AssertionError("无法读取 " + path, exception);
        }
    }
}
