package com.autodealer.crm.integration;

import com.autodealer.crm.modules.identity.application.api.enums.AssignmentStatus;
import com.autodealer.crm.modules.identity.application.api.enums.AssignmentType;
import com.autodealer.crm.modules.identity.application.api.enums.EmployeeStatus;
import com.autodealer.crm.modules.identity.application.api.enums.OrganizationUnitType;
import com.autodealer.crm.modules.identity.application.api.enums.ReportingStatus;
import com.autodealer.crm.modules.identity.application.api.enums.ReportingType;
import com.autodealer.crm.modules.identity.persistence.mapper.TEmployeeAssignmentMapper;
import com.autodealer.crm.modules.identity.persistence.mapper.TEmployeeMapper;
import com.autodealer.crm.modules.identity.persistence.mapper.TEmployeeReportingMapper;
import com.autodealer.crm.modules.identity.persistence.mapper.TOrganizationUnitMapper;
import com.autodealer.crm.modules.identity.persistence.mapper.TPositionMapper;
import com.autodealer.crm.modules.identity.persistence.model.TEmployee;
import com.autodealer.crm.modules.identity.persistence.model.TEmployeeAssignment;
import com.autodealer.crm.modules.identity.persistence.model.TEmployeeReporting;
import com.autodealer.crm.modules.identity.persistence.model.TOrganizationUnit;
import com.autodealer.crm.modules.identity.persistence.model.TPosition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Transactional
class OrganizationFoundationMapperIntegrationTest extends BackendIntegrationTestBase {

    @Autowired
    private TOrganizationUnitMapper organizationUnitMapper;

    @Autowired
    private TPositionMapper positionMapper;

    @Autowired
    private TEmployeeMapper employeeMapper;

    @Autowired
    private TEmployeeAssignmentMapper employeeAssignmentMapper;

    @Autowired
    private TEmployeeReportingMapper employeeReportingMapper;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("组织和岗位映射完整且列表排序稳定")
    void organizationAndPositionSeed_shouldMapEnumsAndStableOrdering() {
        List<TOrganizationUnit> organizationUnits = organizationUnitMapper.selectAll();
        assertEquals(List.of("DEFAULT_COMPANY"),
                organizationUnits.stream().map(TOrganizationUnit::getCode).toList());
        assertEquals(OrganizationUnitType.COMPANY, organizationUnits.get(0).getType());
        assertEquals(List.of("DEFAULT_COMPANY", "UNASSIGNED_ORG"),
                organizationUnitMapper.selectAllIncludingPlaceholders().stream()
                        .map(TOrganizationUnit::getCode).toList());
        assertEquals(List.of("DEFAULT_COMPANY"),
                organizationUnitMapper.selectRoots().stream().map(TOrganizationUnit::getCode).toList());
        TOrganizationUnit placeholder = organizationUnitMapper.selectByCode("UNASSIGNED_ORG");
        assertTrue(placeholder.getMigrationPlaceholder());
        assertEquals(organizationUnits.get(0).getId(), placeholder.getParentId());

        List<TPosition> positions = positionMapper.selectAll();
        assertFalse(positions.isEmpty());
        TPosition placeholderPosition = positionMapper.selectByCode("UNASSIGNED_POSITION");
        assertNotNull(placeholderPosition);
        assertTrue(placeholderPosition.getBuiltIn());
    }

    @Test
    @DisplayName("禁用历史根公司不参与唯一启用根和首次引导判断")
    void disabledHistoricalRoot_shouldNotAppearInActiveRoots() {
        jdbcTemplate.update("""
                INSERT INTO t_organization_unit
                  (code,name,type,parent_id,order_no,migration_placeholder,enabled,version,create_time)
                VALUES ('HISTORICAL_ROOT','历史根公司','COMPANY',NULL,99,0,0,0,CURRENT_TIMESTAMP)
                """);

        assertEquals(List.of("DEFAULT_COMPANY"),
                organizationUnitMapper.selectRoots().stream().map(TOrganizationUnit::getCode).toList());
    }

    @Test
    @DisplayName("测试种子为普通账号提供完整员工和主任职，受保护系统账号不绑定员工")
    void compatibilitySeed_shouldBackfillHumanUsersWithoutInventingManager() {
        assertNull(employeeMapper.selectByUserId(1));

        TEmployee employee = employeeMapper.selectByUserId(2);
        assertEquals("EMP-000002", employee.getEmployeeNo());
        assertEquals(EmployeeStatus.ACTIVE, employee.getEmploymentStatus());
        assertFalse(employee.getProfileCompleted());

        TEmployeeAssignment primary = employeeAssignmentMapper.selectCurrentPrimaryByEmployeeId(
                employee.getId(), LocalDateTime.now());
        assertEquals(AssignmentType.PRIMARY, primary.getAssignmentType());
        assertEquals(AssignmentStatus.ACTIVE, primary.getStatus());
        assertEquals("DEFAULT_COMPANY",
                organizationUnitMapper.selectByPrimaryKey(primary.getOrganizationUnitId()).getCode());
        assertEquals("TEST_STAFF", positionMapper.selectByPrimaryKey(primary.getPositionId()).getCode());
        assertEquals(1, employeeAssignmentMapper.selectEffectiveByEmployeeId(
                employee.getId(), LocalDateTime.now()).size());
        assertNull(employeeReportingMapper.selectCurrentDirectBySubordinateId(
                employee.getId(), LocalDateTime.now()));
    }

    @Test
    @DisplayName("CAS更新版本不匹配时影响零行")
    void organizationUpdate_staleVersion_shouldAffectNoRows() {
        TOrganizationUnit organizationUnit = organizationUnitMapper.selectByCode("DEFAULT_COMPANY");
        organizationUnit.setName("并发测试公司");
        organizationUnit.setEditTime(LocalDateTime.now());
        organizationUnit.setEditBy(1);

        assertEquals(0, organizationUnitMapper.updateByIdAndVersion(organizationUnit, 99));
        assertEquals(1, organizationUnitMapper.updateByIdAndVersion(
                organizationUnit, organizationUnit.getVersion()));
    }

    @Test
    @DisplayName("汇报关系Mapper映射稳定枚举和当前直属关系")
    void reportingMapper_insertDirectRelation_shouldMapCurrentRelation() {
        TEmployeeReporting reporting = new TEmployeeReporting();
        reporting.setSubordinateEmployeeId(1);
        reporting.setManagerEmployeeId(2);
        reporting.setRelationType(ReportingType.DIRECT);
        reporting.setStatus(ReportingStatus.ACTIVE);
        reporting.setActiveDirectMarker(true);
        reporting.setEffectiveFrom(LocalDateTime.now().minusMinutes(1));
        reporting.setReason("Mapper映射测试");
        reporting.setVersion(0);
        reporting.setCreateTime(LocalDateTime.now());
        reporting.setCreateBy(1);

        assertEquals(1, employeeReportingMapper.insert(reporting));
        TEmployeeReporting current = employeeReportingMapper.selectCurrentDirectBySubordinateId(
                1, LocalDateTime.now());
        assertEquals(ReportingType.DIRECT, current.getRelationType());
        assertEquals(ReportingStatus.ACTIVE, current.getStatus());
        assertEquals(2, current.getManagerEmployeeId());
        assertEquals(1, employeeReportingMapper.selectEffectiveManagers(1, LocalDateTime.now()).size());
    }

    @Test
    @DisplayName("新任职写入失败时关闭旧任职必须整体回滚")
    void assignmentHistoryInsertFailure_shouldRollbackCurrentAssignmentUpdate() {
        TEmployeeAssignment original = employeeAssignmentMapper.selectCurrentPrimaryByEmployeeId(
                1, LocalDateTime.now());
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        assertThrows(RuntimeException.class, () -> transactionTemplate.executeWithoutResult(status -> {
            TEmployeeAssignment closing = employeeAssignmentMapper.selectCurrentPrimaryByEmployeeId(
                    1, LocalDateTime.now());
            LocalDateTime closeTime = LocalDateTime.now();
            assertEquals(1, employeeAssignmentMapper.endByIdAndVersion(
                    closing.getId(), closing.getVersion(), closeTime, closeTime, 1));

            TEmployeeAssignment invalidNewAssignment = new TEmployeeAssignment();
            invalidNewAssignment.setEmployeeId(1);
            invalidNewAssignment.setOrganizationUnitId(999999);
            invalidNewAssignment.setPositionId(1);
            invalidNewAssignment.setAssignmentType(AssignmentType.PRIMARY);
            invalidNewAssignment.setStatus(AssignmentStatus.ACTIVE);
            invalidNewAssignment.setActivePrimaryMarker(true);
            invalidNewAssignment.setEffectiveFrom(LocalDateTime.now());
            invalidNewAssignment.setReason("故意触发外键失败");
            invalidNewAssignment.setVersion(0);
            invalidNewAssignment.setCreateTime(LocalDateTime.now());
            invalidNewAssignment.setCreateBy(1);
            employeeAssignmentMapper.insert(invalidNewAssignment);
        }));

        TEmployeeAssignment current = employeeAssignmentMapper.selectCurrentPrimaryByEmployeeId(
                1, LocalDateTime.now());
        assertEquals(original.getId(), current.getId());
        assertEquals(AssignmentStatus.ACTIVE, current.getStatus());
        assertEquals(original.getVersion(), current.getVersion());
        assertNull(current.getEffectiveTo());
    }
}
