package com.autodealer.crm.integration;

import com.autodealer.crm.bootstrap.DealerCRMApplication;
import com.autodealer.crm.modules.identity.application.api.enums.AccountType;
import com.autodealer.crm.modules.identity.persistence.mapper.TUserMapper;
import com.autodealer.crm.modules.identity.application.api.model.TUser;
import org.apache.ibatis.session.SqlSession;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(classes = DealerCRMApplication.class)
@ActiveProfiles("test")
@Transactional
class AuthenticationVersionMapperIntegrationTest {

    @Autowired
    private TUserMapper userMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SqlSession sqlSession;

    @Test
    void updatePassword_shouldAtomicallyIncrementAuthVersion() {
        TUser before = userMapper.selectByPrimaryKey(2);

        assertEquals(1, userMapper.updatePassword(2, before.getLoginPwd()));

        TUser after = userMapper.selectByPrimaryKey(2);
        assertEquals(before.getAuthVersion() + 1, after.getAuthVersion());
    }

    @Test
    void incrementAuthVersion_missingUser_shouldAffectNoRows() {
        assertEquals(0, userMapper.incrementAuthVersion(999999));
    }

    @Test
    void invitationActivation_shouldReplaceExpiredCredentialState() {
        TUser before = userMapper.selectByPrimaryKey(2);
        jdbcTemplate.update("UPDATE t_user SET credentials_no_expired=0,password_expires_at=? WHERE id=2", LocalDateTime.now().minusDays(1));

        assertEquals(1, userMapper.updatePasswordCredentialState(2, before.getVersion(), before.getLoginPwd(), true, LocalDateTime.now().plusDays(90)));

        TUser after = userMapper.selectByPrimaryKey(2);
        assertEquals(1, after.getCredentialsNoExpired());
        assertEquals("ACTIVE", after.getAccountStatus().name());
    }

    @Test
    void availableAdminCount_shouldExcludeEveryUnavailableSecurityState() {
        assertEquals(0, userMapper.countAdminUsers(), "受保护恢复账号不能充当普通管理入口");
        insertAvailableOrdinaryAdmin();
        sqlSession.clearCache();
        assertEquals(1, userMapper.countAdminUsers());

        jdbcTemplate.update("UPDATE t_user SET account_no_locked=0 WHERE id=91");
        sqlSession.clearCache();
        assertEquals(0, userMapper.countAdminUsers());
        jdbcTemplate.update("UPDATE t_user SET account_no_locked=1,account_no_expired=0 WHERE id=91");
        sqlSession.clearCache();
        assertEquals(0, userMapper.countAdminUsers());
        jdbcTemplate.update("UPDATE t_user SET account_no_expired=1,account_expires_at=? WHERE id=91", LocalDateTime.now().minusSeconds(1));
        sqlSession.clearCache();
        assertEquals(0, userMapper.countAdminUsers());
        jdbcTemplate.update("UPDATE t_user SET account_expires_at=NULL,credentials_no_expired=0 WHERE id=91");
        sqlSession.clearCache();
        assertEquals(0, userMapper.countAdminUsers());
        jdbcTemplate.update("UPDATE t_user SET credentials_no_expired=1,password_expires_at=? WHERE id=91", LocalDateTime.now().minusSeconds(1));
        sqlSession.clearCache();
        assertEquals(0, userMapper.countAdminUsers());
        jdbcTemplate.update("UPDATE t_user SET password_expires_at=NULL WHERE id=91");
        sqlSession.clearCache();
        assertEquals(1, userMapper.countAdminUsers());

        jdbcTemplate.update("UPDATE t_employee SET phone_verified=0 WHERE id=91");
        sqlSession.clearCache();
        assertEquals(0,userMapper.countAdminUsers());
        assertEquals(1,userMapper.countPendingAdminUsers());
        jdbcTemplate.update("UPDATE t_employee SET phone=NULL WHERE id=91");
        sqlSession.clearCache();
        assertEquals(0,userMapper.countPendingAdminUsers());
        assertEquals(0,userMapper.selectRecoverableAdminCandidatesForUpdate().size());
        jdbcTemplate.update("UPDATE t_employee SET phone='13900000091',phone_verified=1 WHERE id=91");
        sqlSession.clearCache();
        assertEquals(1,userMapper.countAdminUsers());

        jdbcTemplate.update("UPDATE t_employee SET employment_status='LEFT' WHERE id=91");
        sqlSession.clearCache();
        assertEquals(0, userMapper.countAdminUsers());
        jdbcTemplate.update("UPDATE t_employee SET employment_status='ACTIVE' WHERE id=91");
        jdbcTemplate.update("UPDATE t_organization_unit SET enabled=0 WHERE id=991");
        sqlSession.clearCache();
        assertEquals(0, userMapper.countAdminUsers());
        jdbcTemplate.update("UPDATE t_organization_unit SET enabled=1 WHERE id=991");
        jdbcTemplate.update("UPDATE t_position SET enabled=0 WHERE id=991");
        sqlSession.clearCache();
        assertEquals(0, userMapper.countAdminUsers());
        jdbcTemplate.update("UPDATE t_position SET enabled=1 WHERE id=991");
        assertThrows(DataIntegrityViolationException.class,()->jdbcTemplate.update("UPDATE t_user SET account_type='SYSTEM',protected_account=1 WHERE id=91"));
        sqlSession.clearCache();
        assertEquals(1, userMapper.countAdminUsers(), "普通账号不能升级为第二个受保护恢复账号");
    }

    private void insertAvailableOrdinaryAdmin() {
        jdbcTemplate.update("INSERT INTO t_organization_unit(id,code,name,type,parent_id,order_no,placeholder,enabled,version,create_time,create_by) VALUES(991,'COUNT_ADMIN_ORG','有效管理员组织','DEPARTMENT',1,1,0,1,0,CURRENT_TIMESTAMP,1)");
        jdbcTemplate.update("INSERT INTO t_position(id,code,name,position_level,built_in,enabled,version,create_time,create_by) VALUES(991,'COUNT_ADMIN_POSITION','有效管理员岗位',100,0,1,0,CURRENT_TIMESTAMP,1)");
        jdbcTemplate.update("INSERT INTO t_user(id,login_act,login_pwd,name,account_no_expired,credentials_no_expired,account_no_locked,account_enabled,account_status,account_type,protected_account,manual_locked,version,authorization_version,auth_version,session_revision,create_time,create_by) VALUES(91,'ordinary_admin_91','x','普通管理员',1,1,1,1,'ACTIVE','HUMAN',0,0,0,0,0,0,CURRENT_TIMESTAMP,1)");
        jdbcTemplate.update("INSERT INTO t_employee(id,user_id,employee_no,name,phone,employment_status,profile_completed,version,profile_version,phone_verified,email_verified,create_time,create_by) VALUES(91,91,'EMP-000091','普通管理员','13900000091','ACTIVE',1,0,0,1,0,CURRENT_TIMESTAMP,1)");
        jdbcTemplate.update("INSERT INTO t_employee_assignment(id,employee_id,organization_unit_id,position_id,assignment_type,status,active_primary_marker,effective_from,reason,version,create_time,create_by) VALUES(991,91,991,991,'PRIMARY','ACTIVE',1,CURRENT_TIMESTAMP,'管理员计数测试',0,CURRENT_TIMESTAMP,1)");
        jdbcTemplate.update("INSERT INTO t_user_role(user_id,role_id,granted_by,reason,effective_from,active_marker,version) SELECT 91,id,1,'管理员计数测试',CURRENT_TIMESTAMP,1,0 FROM t_role WHERE role='admin'");
    }

    @Test
    void genericUpdate_shouldNotRewriteProtectedOrVersionFields() {
        TUser before = userMapper.selectByPrimaryKey(2);
        TUser update = new TUser();
        update.setId(2);
        update.setName(before.getName() + "-资料更新");
        update.setAccountType(AccountType.SYSTEM);
        update.setProtectedAccount(true);
        update.setVersion(before.getVersion() + 10);
        update.setAuthVersion(before.getAuthVersion() + 10);

        userMapper.updateByPrimaryKeySelective(update);

        TUser after = userMapper.selectByPrimaryKey(2);
        assertEquals(before.getName() + "-资料更新", after.getName());
        assertEquals(AccountType.HUMAN, after.getAccountType());
        assertFalse(after.getProtectedAccount());
        assertEquals(before.getVersion(), after.getVersion());
        assertEquals(before.getAuthVersion(), after.getAuthVersion());
    }
}
