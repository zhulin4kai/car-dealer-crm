package com.autodealer.crm.service.impl;

import com.autodealer.crm.audit.AuditActionEnum;
import com.autodealer.crm.audit.SecurityFailureAuditService;
import com.autodealer.crm.config.security.CurrentUserProvider;
import com.autodealer.crm.config.security.UserManagementAccessGate;
import com.autodealer.crm.dto.credential.CredentialDtos.AdminAccessRecoveryRequest;
import com.autodealer.crm.dto.credential.CredentialDtos.AdminAccessRecoveryResult;
import com.autodealer.crm.dto.credential.CredentialDtos.ManagedDeliveryResult;
import com.autodealer.crm.enums.AccountStatus;
import com.autodealer.crm.enums.AccountType;
import com.autodealer.crm.exception.BusinessException;
import com.autodealer.crm.mapper.TAuthorizationGraphLockMapper;
import com.autodealer.crm.mapper.TUserMapper;
import com.autodealer.crm.mapper.TEmployeeMapper;
import com.autodealer.crm.model.TUser;
import com.autodealer.crm.model.TEmployee;
import com.autodealer.crm.manager.RedisManager;
import com.autodealer.crm.constant.RedisKeys;
import com.autodealer.crm.result.CodeEnum;
import com.autodealer.crm.service.CredentialService;
import com.autodealer.crm.service.UserSessionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** 零可用普通管理员时的窄恢复命令；不能选择目标、增加角色或修改组织。 */
@Service
public class AdminAccessRecoveryService {
    private final TUserMapper users;private final TAuthorizationGraphLockMapper graphLocks;
    private final CurrentUserProvider current;private final UserManagementAccessGate gate;
    private final CredentialService credentials;private final UserSessionService sessions;private final String recoveryKey;
    private final SecurityFailureAuditService failureAudit;
    private final TEmployeeMapper employees;private final RedisManager redis;private final CredentialDerivationCodec digester;
    private final boolean rateLimitEnabled;
    public AdminAccessRecoveryService(TUserMapper users,TAuthorizationGraphLockMapper graphLocks,
                                      CurrentUserProvider current,UserManagementAccessGate gate,
                                      CredentialService credentials,UserSessionService sessions,SecurityFailureAuditService failureAudit,
                                      TEmployeeMapper employees,RedisManager redis,CredentialDerivationCodec digester,
                                      @Value("${security.recovery.break-glass.key:${RECOVERY_BREAK_GLASS_KEY:}}") String recoveryKey,
                                      @Value("${security.credential-rate-limit.enabled:true}") boolean rateLimitEnabled){
        this.users=users;this.graphLocks=graphLocks;this.current=current;this.gate=gate;
        this.credentials=credentials;this.sessions=sessions;this.failureAudit=failureAudit;this.recoveryKey=recoveryKey==null?"":recoveryKey;
        this.employees=employees;this.redis=redis;this.digester=digester;this.rateLimitEnabled=rateLimitEnabled;
    }

    @Transactional(rollbackFor=Exception.class)
    public AdminAccessRecoveryResult recover(AdminAccessRecoveryRequest request){
        TUser operator=current.getCurrentUser();
        if(!isFixedRecovery(operator)){
            failureAudit.recordAuthenticated(AuditActionEnum.USER_RECOVERY_KEY_REJECTED,"ADMIN_ACCESS",
                    "{\"reason\":\"IDENTITY_MISMATCH\"}",operator);
            throw new BusinessException(CodeEnum.ACCESS_DENIED,"普通管理员入口恢复身份或恢复密钥无效");
        }
        requireRate("ADMIN_ACCESS_ACCOUNT",String.valueOf(operator.getId()),3,3600,operator);
        requireRate("ADMIN_ACCESS_SOURCE",requestSource(),10,3600,operator);
        if(recoveryKey.length()<32||!constantTimeEquals(recoveryKey,request.getRecoveryKey())){
            failureAudit.recordAuthenticated(AuditActionEnum.USER_RECOVERY_KEY_REJECTED,"ADMIN_ACCESS",
                    "{\"reason\":\"KEY_MISMATCH\"}",operator);
            throw new BusinessException(CodeEnum.ACCESS_DENIED,"普通管理员入口恢复身份或恢复密钥无效");
        }
        lock("AUTHORIZATION_MEMBERSHIP_GUARD");lock("ORGANIZATION_HIERARCHY");lock("REPORTING_GRAPH");lock("AVAILABLE_ADMIN_GUARD");
        UserManagementAccessGate.BootstrapState state=gate.state();
        if(state!=UserManagementAccessGate.BootstrapState.DEGRADED&&state!=UserManagementAccessGate.BootstrapState.PENDING_FIRST_CHANGE)
            throw new BusinessException(CodeEnum.ACCESS_DENIED,"当前系统状态不允许恢复普通管理员入口");
        List<TUser> candidates=users.selectRecoverableAdminCandidatesForUpdate();
        if(candidates.isEmpty()){failureAudit.recordAuthenticated(AuditActionEnum.USER_MANAGEMENT_GATE_REJECTED,"ADMIN_ACCESS",
                "{\"reason\":\"NO_DELIVERABLE_ADMIN_CANDIDATE\"}",operator);
            throw new BusinessException(CodeEnum.LAST_AVAILABLE_ADMIN_REQUIRED,"不存在同时保留有效管理员角色、任职和当前联系方式的可恢复普通账号");}
        TUser target=candidates.get(0);
        TEmployee employee=employees.selectByUserId(target.getId());
        requireRate("ADMIN_ACCESS_TARGET",String.valueOf(target.getId()),3,3600,operator);
        if(employee!=null){if(employee.getPhone()!=null)requireRate("ADMIN_ACCESS_CONTACT_PHONE",employee.getPhone(),3,3600,operator);
            if(employee.getEmail()!=null)requireRate("ADMIN_ACCESS_CONTACT_EMAIL",employee.getEmail(),3,3600,operator);}
        LocalDateTime now=LocalDateTime.now();
        if(target.getAccountStatus()!=AccountStatus.INVITED){
            if(users.recoverOrdinaryAdminSecurityByExpected(target.getId(),target.getVersion(),operator.getId(),now)!=1)
                throw new BusinessException(CodeEnum.ACCOUNT_VERSION_CONFLICT,"待恢复管理员状态已变化");
            sessions.revokeAllForSecurityChange(target.getId(),operator.getId(),"降级状态恢复普通管理员入口");
        }else{
            if(users.recoverInvitedAdminSecurityByExpected(target.getId(),target.getVersion(),operator.getId(),now)!=1)
                throw new BusinessException(CodeEnum.ACCOUNT_VERSION_CONFLICT,"待恢复邀请管理员状态已变化");
            sessions.revokeAllForSecurityChange(target.getId(),operator.getId(),"降级状态重发管理员邀请");
        }
        ManagedDeliveryResult delivery=credentials.issueDegradedAdminRecovery(target.getId(),request.getReason());
        return new AdminAccessRecoveryResult(target.getId(),target.getLoginAct(),target.getAccountStatus().name(),
                delivery.accepted(),delivery.deliveryStatus());
    }
    private void lock(String name){if(!name.equals(graphLocks.lockByName(name)))throw new IllegalStateException("管理员恢复图锁缺失: "+name);}
    private boolean isFixedRecovery(TUser user){return user!=null&&Objects.equals(user.getId(),1)&&"admin".equals(user.getLoginAct())&&user.getAccountType()==AccountType.SYSTEM&&Boolean.TRUE.equals(user.getProtectedAccount());}
    private boolean constantTimeEquals(String expected,String actual){return actual!=null&&MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),actual.getBytes(StandardCharsets.UTF_8));}
    private void requireRate(String scope,String subject,int limit,long seconds,TUser operator){if(!rateLimitEnabled)return;String digest=digester.contactDigest("RATE:"+scope,subject==null?"<NULL>":subject);Long count=redis.incrementSlidingWindow(RedisKeys.credentialRateLimit(scope,digest.substring(0,32)),seconds);if(count!=null&&count<=limit)return;failureAudit.recordAuthenticated(AuditActionEnum.USER_CREDENTIAL_RATE_LIMIT,"ADMIN_ACCESS","{\"scope\":\""+scope+"\"}",operator);throw new BusinessException(CodeEnum.CREDENTIAL_RATE_LIMITED);}
    private String requestSource(){if(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes){String remote=attributes.getRequest().getRemoteAddr();if(remote!=null&&!remote.isBlank())return remote;}return "INTERNAL";}
}
