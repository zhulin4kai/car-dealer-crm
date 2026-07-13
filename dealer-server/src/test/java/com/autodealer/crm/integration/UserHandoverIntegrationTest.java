package com.autodealer.crm.integration;

import com.autodealer.crm.dto.user.UserLifecycleDtos.ResponsibilityRow;
import com.autodealer.crm.dto.user.UserLifecycleDtos.*;
import com.autodealer.crm.exception.BusinessException;
import com.autodealer.crm.audit.AuditActionEnum;
import com.autodealer.crm.audit.OperationAuditRecorder;
import com.autodealer.crm.config.security.CurrentUserProvider;
import com.autodealer.crm.constant.PermissionCodes;
import com.autodealer.crm.mapper.TUserLifecycleMapper;
import com.autodealer.crm.model.TUser;
import com.autodealer.crm.service.AuthorizationDataScope;
import com.autodealer.crm.service.DataScopeResolver;
import com.autodealer.crm.service.UserLifecycleService;
import com.autodealer.crm.service.impl.UserAuthorizationPolicy;
import com.autodealer.crm.result.CodeEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
class UserHandoverIntegrationTest {
    @Autowired JdbcTemplate jdbc; @Autowired TUserLifecycleMapper lifecycle; @Autowired TransactionTemplate transactions;
    @Autowired UserLifecycleService service;
    @MockBean CurrentUserProvider current;
    @MockBean UserAuthorizationPolicy policy;
    @MockBean DataScopeResolver dataScopes;
    @MockBean OperationAuditRecorder operationAudit;

    @BeforeEach void seedSixDomains(){
        when(current.getCurrentUserId()).thenReturn(1);when(current.hasAuthority(anyString())).thenReturn(true);
        when(policy.isGlobalOperator()).thenReturn(true);
        when(dataScopes.resolve(anyInt(),anyString())).thenReturn(AuthorizationDataScope.global(java.util.Set.of()));
        jdbc.update("DELETE FROM t_user_lifecycle_event WHERE user_id=2");
        jdbc.update("DELETE FROM t_user_lifecycle_snapshot WHERE user_id=2");
        jdbc.update("DELETE FROM t_communication_record WHERE id=9909");
        jdbc.update("DELETE FROM t_quote WHERE id=9907");
        jdbc.update("DELETE FROM t_tran WHERE id=9908");
        jdbc.update("DELETE FROM t_test_drive WHERE id=9906");jdbc.update("DELETE FROM t_follow_task WHERE id=9905");
        jdbc.update("DELETE FROM t_opportunity WHERE id=9904");
        jdbc.update("DELETE FROM t_customer_owner_history WHERE customer_id=9903");jdbc.update("DELETE FROM t_customer WHERE id=9903");
        jdbc.update("DELETE FROM t_clue_owner_history WHERE clue_id=9902");jdbc.update("DELETE FROM t_clue WHERE id=9902");jdbc.update("DELETE FROM t_activity WHERE id IN (9901,9911)");
        jdbc.update("MERGE INTO t_product(id,sku,name,price,stock,status) KEY(id) VALUES(9900,'SKU-9900','交接测试车辆',1,1,'ON_SALE')");
        jdbc.update("MERGE INTO t_product_vehicle(id,product_id,vin,color,location,status) KEY(id) VALUES(9900,9900,'VIN-9900','黑','测试库位','AVAILABLE')");
        jdbc.update("INSERT INTO t_activity(id,owner_id,name,status,channel,reviewed_by,create_by,edit_by) VALUES(9901,2,'交接活动','ENDED','OFFLINE_EVENT',2,2,2)");
        jdbc.update("INSERT INTO t_clue(id,owner_id,full_name,phone,state,create_by,edit_by) VALUES(9902,2,'交接线索','13999909902',-999,2,2)");
        jdbc.update("INSERT INTO t_customer(id,clue_id,owner_id,customer_name,phone,customer_status,create_by,edit_by) VALUES(9903,9902,2,'交接客户','13999909903','INTENTION',2,2)");
        jdbc.update("INSERT INTO t_opportunity(id,opportunity_no,customer_id,owner_id,stage,requirement,version,create_by,update_by) VALUES(9904,'OPP-9904',9903,2,'SHELVED','交接商机',7,2,2)");
        jdbc.update("INSERT INTO t_follow_task(id,title,task_type,related_object_type,related_object_id,owner_id,priority,due_time,status,completed_by,version,create_by,update_by) VALUES(9905,'交接任务','PHONE_FOLLOW_UP','CUSTOMER',9903,2,'NORMAL',CURRENT_TIMESTAMP,'OVERDUE',2,8,2,2)");
        Long vehicleId=9900L;
        jdbc.update("INSERT INTO t_test_drive(id,test_drive_no,customer_id,vehicle_id,owner_id,planned_start_time,planned_end_time,status,contact_name,contact_phone,reschedule_count,safety_confirmed_by,check_in_by,version,create_by,update_by) VALUES(9906,'TD-9906',9903,?,2,?,?, 'CHECKED_IN','客户','13999909906',0,2,2,9,2,2)",vehicleId,LocalDateTime.now().plusDays(2),LocalDateTime.now().plusDays(2).plusHours(1));
    }

    @Test void successfulConfirmTransfersOnlySixOwnersAndLeavesDerivedAndHistoricalFactsUntouched(){
        prepareRealRecipientAndSource();
        jdbc.update("INSERT INTO t_quote(id,quote_no,customer_id,status,remark,create_by,update_by) VALUES(9907,'Q-9907',9903,'DRAFT','派生报价',2,2)");
        jdbc.update("INSERT INTO t_tran(id,tran_no,customer_id,money,stage,description,create_by,edit_by,version) VALUES(9908,'T-9908',9903,100,'PENDING','动态待审批队列',2,2,4)");
        jdbc.update("INSERT INTO t_communication_record(id,related_object_type,related_object_id,owner_id,communication_method,communication_time,summary,status,version,create_by,update_by) VALUES(9909,'CUSTOMER',9903,2,'PHONE',CURRENT_TIMESTAMP,'历史责任快照','ACTIVE',3,2,2)");
        String reason="六域成功交接";
        DeparturePrecheckRequest precheckRequest=new DeparturePrecheckRequest();precheckRequest.setEmployeeVersion(0);precheckRequest.setReason(reason);
        DeparturePrecheck precheck=service.precheckDeparture(2,precheckRequest);
        int quoteImpact=precheck.getResponsibilities().stream().filter(value->"QUOTE".equals(value.getResourceType())).findFirst().orElseThrow().getCount();
        int tranImpact=precheck.getResponsibilities().stream().filter(value->"TRAN".equals(value.getResourceType())).findFirst().orElseThrow().getCount();
        assertTrue(quoteImpact>=1);assertTrue(tranImpact>=1);
        ConfirmHandoverRequest command=new ConfirmHandoverRequest();command.setEmployeeVersion(0);command.setReason(reason);command.setSnapshotToken(precheck.getSnapshotToken());
        command.setTransfers(java.util.Arrays.stream(DirectResourceType.values()).map(type->{TransferSelection value=new TransferSelection();value.setResourceType(type);value.setTargetEmployeeId(2);return value;}).toList());

        HandoverResult result=service.confirmHandover(2,command);

        assertTrue(result.isSuccess());assertEquals(6,result.getDomainResults().size());
        result.getDomainResults().forEach(domain->{assertEquals("SUCCESS",domain.getResultCode());assertEquals(domain.getExpectedCount(),domain.getTransferredCount());});
        for(String table:List.of("t_activity","t_clue","t_customer","t_opportunity","t_follow_task","t_test_drive"))
            assertEquals(3,jdbc.queryForObject("SELECT owner_id FROM "+table+" WHERE id=?",Integer.class,id(table)));
        assertEquals("DRAFT",jdbc.queryForObject("SELECT status FROM t_quote WHERE id=9907",String.class));
        assertEquals(2,jdbc.queryForObject("SELECT create_by FROM t_quote WHERE id=9907",Integer.class));
        assertEquals(2,jdbc.queryForObject("SELECT update_by FROM t_quote WHERE id=9907",Integer.class));
        assertEquals("PENDING",jdbc.queryForObject("SELECT stage FROM t_tran WHERE id=9908",String.class));
        assertEquals(2,jdbc.queryForObject("SELECT create_by FROM t_tran WHERE id=9908",Integer.class));
        assertEquals(2,jdbc.queryForObject("SELECT edit_by FROM t_tran WHERE id=9908",Integer.class));
        assertEquals(4,jdbc.queryForObject("SELECT version FROM t_tran WHERE id=9908",Integer.class));
        assertEquals(2,jdbc.queryForObject("SELECT owner_id FROM t_communication_record WHERE id=9909",Integer.class));
        assertEquals(2,jdbc.queryForObject("SELECT create_by FROM t_communication_record WHERE id=9909",Integer.class));
        assertEquals(2,jdbc.queryForObject("SELECT update_by FROM t_communication_record WHERE id=9909",Integer.class));
        assertEquals(0,lifecycle.countActiveQuotesByOwner(2));assertEquals(0,lifecycle.countActiveTransactionsByOwner(2));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM t_user_lifecycle_event WHERE user_id=2 AND action='HANDOVER_CONFIRM'",Integer.class));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM t_user_lifecycle_snapshot WHERE user_id=2 AND consumed_at IS NOT NULL",Integer.class),"成功确认后预检快照必须被一次性消费");
        verify(operationAudit).record(eq(AuditActionEnum.USER_HANDOVER_CONFIRM),eq("2"),eq("SUCCESS"),anyString());
    }

    @Test void realConfirmHandoverRollsBackSnapshotSixDomainsHistoriesEmployeeAndLifecycleEventWhenFinalAuditFails(){
        prepareRealRecipientAndSource();
        String reason="服务级交接回滚";
        DeparturePrecheckRequest precheckRequest=new DeparturePrecheckRequest();precheckRequest.setEmployeeVersion(0);precheckRequest.setReason(reason);
        DeparturePrecheck precheck=service.precheckDeparture(2,precheckRequest);
        long snapshotId=jdbc.queryForObject("SELECT id FROM t_user_lifecycle_snapshot WHERE token_digest IS NOT NULL ORDER BY id DESC LIMIT 1",Long.class);
        int clueHistoryBefore=count("t_clue_owner_history");int customerHistoryBefore=count("t_customer_owner_history");
        int eventBefore=count("t_user_lifecycle_event");int operationBefore=count("t_operation_log");
        doThrow(new IllegalStateException("故障注入：最终操作审计失败")).when(operationAudit)
                .record(eq(AuditActionEnum.USER_HANDOVER_CONFIRM),eq("2"),eq("SUCCESS"),anyString());
        ConfirmHandoverRequest command=new ConfirmHandoverRequest();command.setEmployeeVersion(0);command.setReason(reason);
        command.setSnapshotToken(precheck.getSnapshotToken());
        command.setTransfers(java.util.Arrays.stream(DirectResourceType.values()).map(type->{TransferSelection value=new TransferSelection();value.setResourceType(type);value.setTargetEmployeeId(2);return value;}).toList());

        IllegalStateException failure=assertThrows(IllegalStateException.class,()->service.confirmHandover(2,command));

        assertTrue(failure.getMessage().contains("最终操作审计失败"));
        for(String table:List.of("t_activity","t_clue","t_customer","t_opportunity","t_follow_task","t_test_drive"))
            assertEquals(2,jdbc.queryForObject("SELECT owner_id FROM "+table+" WHERE id=?",Integer.class,id(table)));
        assertEquals(7,jdbc.queryForObject("SELECT version FROM t_opportunity WHERE id=9904",Integer.class));
        assertEquals(8,jdbc.queryForObject("SELECT version FROM t_follow_task WHERE id=9905",Integer.class));
        assertEquals(9,jdbc.queryForObject("SELECT version FROM t_test_drive WHERE id=9906",Integer.class));
        assertEquals(2,jdbc.queryForObject("SELECT reviewed_by FROM t_activity WHERE id=9901",Integer.class));
        assertEquals(2,jdbc.queryForObject("SELECT completed_by FROM t_follow_task WHERE id=9905",Integer.class));
        assertEquals(2,jdbc.queryForObject("SELECT safety_confirmed_by FROM t_test_drive WHERE id=9906",Integer.class));
        assertEquals(2,jdbc.queryForObject("SELECT check_in_by FROM t_test_drive WHERE id=9906",Integer.class));
        assertEquals(0,jdbc.queryForObject("SELECT version FROM t_employee WHERE user_id=2",Integer.class));
        assertNull(jdbc.queryForObject("SELECT consumed_at FROM t_user_lifecycle_snapshot WHERE id=?",LocalDateTime.class,snapshotId));
        assertEquals(clueHistoryBefore,count("t_clue_owner_history"));assertEquals(customerHistoryBefore,count("t_customer_owner_history"));
        assertEquals(eventBefore,count("t_user_lifecycle_event"));assertEquals(operationBefore,count("t_operation_log"));
    }

    @Test void confirmRejectsSelectionForDomainWithoutCurrentResponsibility(){
        prepareRealRecipientAndSource();
        jdbc.update("DELETE FROM t_activity WHERE id=9901");
        String reason="拒绝空责任域";
        DeparturePrecheckRequest precheckRequest=new DeparturePrecheckRequest();precheckRequest.setEmployeeVersion(0);precheckRequest.setReason(reason);
        DeparturePrecheck precheck=service.precheckDeparture(2,precheckRequest);
        ConfirmHandoverRequest command=new ConfirmHandoverRequest();command.setEmployeeVersion(0);command.setReason(reason);command.setSnapshotToken(precheck.getSnapshotToken());
        command.setTransfers(java.util.Arrays.stream(DirectResourceType.values()).map(type->{TransferSelection value=new TransferSelection();value.setResourceType(type);value.setTargetEmployeeId(2);return value;}).toList());

        BusinessException exception=assertThrows(BusinessException.class,()->service.confirmHandover(2,command));

        assertEquals(CodeEnum.PARAM_ERROR,exception.getCodeEnum());
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM t_user_lifecycle_event WHERE user_id=2 AND action='HANDOVER_CONFIRM'",Integer.class));
    }

    @Test void confirmRejectsExpiredSnapshotBeforeAnyOwnerChanges(){
        prepareRealRecipientAndSource();String reason="过期预检";
        DeparturePrecheckRequest precheckRequest=new DeparturePrecheckRequest();precheckRequest.setEmployeeVersion(0);precheckRequest.setReason(reason);
        DeparturePrecheck precheck=service.precheckDeparture(2,precheckRequest);
        jdbc.update("UPDATE t_user_lifecycle_snapshot SET expires_at=DATEADD('SECOND',-1,CURRENT_TIMESTAMP) WHERE user_id=2 AND consumed_at IS NULL");
        ConfirmHandoverRequest command=handoverCommand(precheck,reason);

        BusinessException exception=assertThrows(BusinessException.class,()->service.confirmHandover(2,command));

        assertEquals(CodeEnum.USER_LIFECYCLE_SNAPSHOT_EXPIRED,exception.getCodeEnum());
        assertEquals(2,jdbc.queryForObject("SELECT owner_id FROM t_customer WHERE id=9903",Integer.class));
    }

    @Test void sameCountResponsibilitySwapInvalidatesSnapshot(){
        prepareRealRecipientAndSource();String reason="同数量事实变化";
        DeparturePrecheckRequest precheckRequest=new DeparturePrecheckRequest();precheckRequest.setEmployeeVersion(0);precheckRequest.setReason(reason);
        DeparturePrecheck precheck=service.precheckDeparture(2,precheckRequest);
        jdbc.update("DELETE FROM t_activity WHERE id=9901");
        jdbc.update("INSERT INTO t_activity(id,owner_id,name,status,channel,reviewed_by,create_by,edit_by) VALUES(9911,2,'替换后的交接活动','ENDED','OFFLINE_EVENT',2,2,2)");

        BusinessException exception=assertThrows(BusinessException.class,()->service.confirmHandover(2,handoverCommand(precheck,reason)));

        assertEquals(CodeEnum.USER_LIFECYCLE_CONFLICT,exception.getCodeEnum());
        assertEquals(2,jdbc.queryForObject("SELECT owner_id FROM t_activity WHERE id=9911",Integer.class));
    }

    @Test void failureAfterSixExactUpdatesRollsBackEveryOwnerVersionAndHistoricalActor(){
        RuntimeException failure=assertThrows(RuntimeException.class,()->transactions.executeWithoutResult(status->{
            ResponsibilityRow activity=only(lifecycle.selectActivities(2,true));ResponsibilityRow clue=only(lifecycle.selectClues(2,true));
            ResponsibilityRow customer=only(lifecycle.selectCustomers(2,true));ResponsibilityRow opportunity=only(lifecycle.selectOpportunities(2,true));
            ResponsibilityRow follow=only(lifecycle.selectFollowTasks(2,true));ResponsibilityRow drive=only(lifecycle.selectTestDrives(2,true));
            assertEquals(1,lifecycle.transferActivity(activity.getId(),2,3,activity.getStatus()));
            assertEquals(1,lifecycle.transferClue(clue.getId(),2,3,clue.getState()));
            assertEquals(1,lifecycle.transferCustomer(customer.getId(),2,3,customer.getStatus()));
            assertEquals(1,lifecycle.transferOpportunity(opportunity.getId(),2,3,opportunity.getStatus(),opportunity.getVersion()));
            assertEquals(1,lifecycle.transferFollowTask(follow.getId(),2,3,follow.getStatus(),follow.getVersion()));
            assertEquals(1,lifecycle.transferTestDrive(drive.getId(),2,3,drive.getStatus(),drive.getVersion()));
            throw new RuntimeException("故障注入：审计写入失败");
        }));
        assertTrue(failure.getMessage().contains("故障注入"));
        for(String table:List.of("t_activity","t_clue","t_customer","t_opportunity","t_follow_task","t_test_drive"))
            assertEquals(2,jdbc.queryForObject("SELECT owner_id FROM "+table+" WHERE id=?",Integer.class,id(table)));
        assertEquals(7,jdbc.queryForObject("SELECT version FROM t_opportunity WHERE id=9904",Integer.class));
        assertEquals(8,jdbc.queryForObject("SELECT version FROM t_follow_task WHERE id=9905",Integer.class));
        assertEquals(9,jdbc.queryForObject("SELECT version FROM t_test_drive WHERE id=9906",Integer.class));
        assertEquals(2,jdbc.queryForObject("SELECT reviewed_by FROM t_activity WHERE id=9901",Integer.class));
        assertEquals(2,jdbc.queryForObject("SELECT completed_by FROM t_follow_task WHERE id=9905",Integer.class));
        assertEquals(2,jdbc.queryForObject("SELECT safety_confirmed_by FROM t_test_drive WHERE id=9906",Integer.class));
        assertEquals(2,jdbc.queryForObject("SELECT check_in_by FROM t_test_drive WHERE id=9906",Integer.class));
    }

    private ResponsibilityRow only(List<ResponsibilityRow> rows){return rows.stream().filter(value->value.getId()>=9900).findFirst().orElseThrow();}
    private ConfirmHandoverRequest handoverCommand(DeparturePrecheck precheck,String reason){ConfirmHandoverRequest command=new ConfirmHandoverRequest();command.setEmployeeVersion(precheck.getEmployeeVersion());command.setReason(reason);command.setSnapshotToken(precheck.getSnapshotToken());
        command.setTransfers(java.util.Arrays.stream(DirectResourceType.values()).map(type->{TransferSelection value=new TransferSelection();value.setResourceType(type);value.setTargetEmployeeId(2);return value;}).toList());return command;}
    private void prepareRealRecipientAndSource(){
        jdbc.update("UPDATE t_employee SET employment_status='HANDOVER',version=0 WHERE user_id=2");
        jdbc.update("MERGE INTO t_organization_unit(id,code,name,type,parent_id,order_no,migration_placeholder,enabled,version,create_time,create_by) KEY(id) VALUES(990,'HANDOVER_ORG','交接组织','DEPARTMENT',1,0,0,1,0,CURRENT_TIMESTAMP,1)");
        jdbc.update("MERGE INTO t_position(id,code,name,position_level,built_in,enabled,version,create_time,create_by) KEY(id) VALUES(990,'HANDOVER_POSITION','交接岗位',1,0,1,0,CURRENT_TIMESTAMP,1)");
        jdbc.update("UPDATE t_employee_assignment SET organization_unit_id=990,position_id=990,assignment_type='PRIMARY',status='ACTIVE',active_primary_marker=1,effective_from=DATEADD('DAY',-1,CURRENT_TIMESTAMP),effective_to=NULL WHERE employee_id=2");
        jdbc.update("UPDATE t_user SET account_status='ACTIVE',account_enabled=1,account_no_locked=1,account_no_expired=1,manual_locked=0,auto_locked_until=NULL,account_type='HUMAN',protected_account=0 WHERE id=3");
        for(String code:List.of(PermissionCodes.ACTIVITY_EDIT,PermissionCodes.ACTIVITY_REVIEW,PermissionCodes.CLUE_EDIT,
                PermissionCodes.CUSTOMER_VIEW,PermissionCodes.QUOTE_EDIT,PermissionCodes.TRAN_EDIT,
                PermissionCodes.OPPORTUNITY_EDIT,PermissionCodes.FOLLOW_TASK_UPDATE,PermissionCodes.TEST_DRIVE_COMPLETE)){
            Integer permissionId=jdbc.queryForObject("SELECT id FROM t_permission WHERE code=?",Integer.class,code);
            jdbc.update("MERGE INTO t_user_permission(user_id,permission_id,effect,data_scope_code,effective_from,effective_to,active_marker,reason,granted_by,version,create_time,update_time) KEY(user_id,permission_id) VALUES(3,?,'GRANT','GLOBAL',DATEADD('DAY',-1,CURRENT_TIMESTAMP),NULL,1,'交接测试资格',1,0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)",permissionId);
        }
    }
    private int count(String table){return jdbc.queryForObject("SELECT COUNT(*) FROM "+table,Integer.class);}
    private long id(String table){return switch(table){case "t_activity"->9901;case "t_clue"->9902;case "t_customer"->9903;case "t_opportunity"->9904;case "t_follow_task"->9905;default->9906;};}
}
