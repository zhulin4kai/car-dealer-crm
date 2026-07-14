package com.autodealer.crm.modules.identity.application.internal;

import com.autodealer.crm.modules.identity.application.api.dto.organization.ActingReportingCollectionResponse;
import com.autodealer.crm.modules.identity.application.api.dto.organization.EmployeeOrganizationMembershipResponse;
import com.autodealer.crm.modules.identity.application.api.dto.organization.OrganizationUnitResponse;
import com.autodealer.crm.modules.identity.application.api.dto.organization.PositionResponse;
import com.autodealer.crm.shared.infrastructure.cache.RedisKeys;
import com.autodealer.crm.modules.identity.application.api.*;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OwnerCandidateCacheInvalidationWiringTest {
    private static final Path IMPLEMENTATION_ROOT = Path.of(
            "src/main/java/com/autodealer/crm/modules/identity/application/internal");
    private static final String ACCESS_CHANGED = "securityMutations.accessChanged";
    private static final String AUTHENTICATION_CHANGED = "securityMutations.authenticationChanged";

    @Test
    void everyOwnerQualificationFactMutationReachesTheCentralInvalidator() throws Exception {
        String accounts = source("ManagedUserAccountServiceImpl");
        assertMethodContains(accounts, "public Detail changeStatus(", ACCESS_CHANGED);
        assertMethodContains(accounts, "public Detail updateProfile(", "securityMutations.ownerEligibilityChanged()");
        assertMethodContains(accounts, "public Detail changeLoginAccount(", AUTHENTICATION_CHANGED);
        assertMethodContains(accounts, "public Detail changeSecurityExpiration(", AUTHENTICATION_CHANGED);
        assertMethodDoesNotContain(accounts, "public Detail changeSecurityExpiration(", ACCESS_CHANGED);

        String authorization = source("AuthorizationServiceImpl");
        assertMethodContains(authorization, "public Detail replaceRoles(", "scheduleCleanup(userId)");
        assertMethodContains(authorization, "public Detail updatePermissions(", "scheduleCleanup(userId)");
        assertMethodContains(authorization, "public BatchResult batchUpdateRoles(", "finishBatch(");
        assertMethodContains(authorization, "public BatchResult batchUpdatePermissions(", "finishBatch(");
        assertMethodContains(authorization, "private BatchResult finishBatch(", "changedUserIds.forEach(this::scheduleCleanup)");
        assertMethodContains(authorization, "private void scheduleCleanup(", ACCESS_CHANGED);

        String roles = source("RoleAccessServiceImpl");
        assertMethodContains(roles, "public RoleResponse update(", "invalidateRoleMembers(id)");
        assertMethodContains(roles, "public RoleResponse status(", "invalidateRoleMembers(id)");
        assertMethodContains(roles, "public UpdateMatrixResponse updateMatrix(", "scheduleCleanup(users)");
        assertMethodContains(roles, "private void scheduleCleanup(", ACCESS_CHANGED);

        String organization = source("OrganizationServiceImpl");
        assertMethodContains(organization, "public OrganizationUnitResponse updateOrganizationUnit(", "securityMutations.ownerEligibilityChanged()");
        assertMethodContains(organization, "public OrganizationUnitResponse changeOrganizationUnitStatus(", "securityMutations.ownerEligibilityChanged()");
        assertMethodContains(organization, "public PositionResponse updatePosition(", "securityMutations.ownerEligibilityChanged()");
        assertMethodContains(organization, "public PositionResponse changePositionStatus(", "securityMutations.ownerEligibilityChanged()");
        assertMethodContains(organization, "public EmployeeOrganizationMembershipResponse updateEmployeeOrganizationMembership(",
                "scheduleAssignmentSecurityCleanup(employee.getUserId())");
        assertMethodContains(organization, "public ActingReportingCollectionResponse replaceActingReportings(",
                "scheduleAssignmentSecurityCleanup(employee.getUserId())");
        assertMethodContains(organization, "private void scheduleAssignmentSecurityCleanup(", ACCESS_CHANGED);

        String lifecycle = source("UserLifecycleServiceImpl");
        assertMethodContains(lifecycle, "public Context transfer(", ACCESS_CHANGED);
        assertMethodContains(lifecycle, "public Context startDeparture(", ACCESS_CHANGED);
        assertMethodContains(lifecycle, "public Context completeDeparture(", ACCESS_CHANGED);
        assertMethodContains(lifecycle, "public RehireResult rehire(", "securityMutations.ownerEligibilityChanged()");

        String credentials = source("CredentialServiceImpl");
        assertMethodContains(credentials, "private void changePassword(", ACCESS_CHANGED);
        assertMethodContains(credentials, "private void changePassword(", AUTHENTICATION_CHANGED);
        assertMethodContains(credentials, "private void recoverProtectedPassword(", AUTHENTICATION_CHANGED);

        String loginSecurity = source("LoginSecurityServiceImpl");
        assertMethodContains(loginSecurity, "public void recordFailure(", ACCESS_CHANGED);

        String profile = source("ProfileServiceImpl");
        assertMethodContains(profile, "public Profile updateOwn(", "securityMutations.ownerEligibilityChanged()");
    }

    @Test
    void ownerPatternDeletionAndSecurityRevocationAreCentralized() throws Exception {
        String directDeletion = "deletePattern(RedisKeys.ownerListPattern())";
        try (var files = Files.walk(Path.of("src/main/java"))) {
            files.filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> !path.endsWith("OwnerCandidateCacheInvalidator.java"))
                    .forEach(path -> assertFileDoesNotContain(path, directDeletion));
        }
        String directRevocation = "revokeAllForSecurityChange(";
        try (var files = Files.walk(IMPLEMENTATION_ROOT)) {
            files.filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> !path.endsWith("UserSecurityMutationCoordinator.java"))
                    .filter(path -> !path.endsWith("UserSessionServiceImpl.java"))
                    .forEach(path -> assertFileDoesNotContain(path, directRevocation));
        }
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
