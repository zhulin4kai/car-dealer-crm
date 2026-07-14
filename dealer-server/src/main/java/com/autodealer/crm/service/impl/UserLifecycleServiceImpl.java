package com.autodealer.crm.service.impl;

import com.autodealer.crm.audit.*;
import com.autodealer.crm.config.security.CurrentUserProvider;
import com.autodealer.crm.constant.PermissionCodes;
import com.autodealer.crm.dto.user.UserLifecycleDtos.*;
import com.autodealer.crm.enums.*;
import com.autodealer.crm.exception.BusinessException;
import com.autodealer.crm.mapper.*;
import com.autodealer.crm.model.*;
import com.autodealer.crm.result.CodeEnum;
import com.autodealer.crm.service.UserLifecycleService;
import com.autodealer.crm.service.DataScopeResolver;
import com.autodealer.crm.service.AuthorizationDataScope;
import com.autodealer.crm.service.CredentialService;
import com.autodealer.crm.dto.credential.CredentialDtos.ManagedDeliveryResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.*;
import java.util.*;

@Service
public class UserLifecycleServiceImpl implements UserLifecycleService {
    private static final Duration SNAPSHOT_TTL=Duration.ofMinutes(10);
    private static final ZoneId BUSINESS_ZONE=ZoneId.systemDefault();
    private static final List<DirectResourceType> DOMAINS=List.of(DirectResourceType.values());
    private static final Map<DirectResourceType,String> DOMAIN_NAMES=Map.of(
            DirectResourceType.ACTIVITY,"市场活动",DirectResourceType.CLUE,"线索",DirectResourceType.CUSTOMER,"客户",
            DirectResourceType.OPPORTUNITY,"商机",DirectResourceType.FOLLOW_TASK,"跟进任务",DirectResourceType.TEST_DRIVE,"试驾");

    private final TUserLifecycleMapper lifecycle;
    private final TUserMapper users; private final TEmployeeMapper employees;
    private final TEmployeeAssignmentMapper assignments; private final TEmployeeReportingMapper reporting;
    private final TOrganizationUnitMapper organizations; private final TPositionMapper positions;
    private final TUserRoleMapper userRoles; private final TUserPermissionMapper userPermissions;
    private final TRoleMapper roles; private final TPermissionMapper permissions;
    private final TClueOwnerHistoryMapper clueOwnerHistory; private final TCustomerOwnerHistoryMapper customerOwnerHistory;
    private final TAuthorizationGraphLockMapper graphLocks;
    private final UserAuthorizationPolicy policy; private final CurrentUserProvider current;
    private final DataScopeResolver dataScopes;
    private final OperationAuditRecorder audit;
    private final CredentialService credentials;
    private final AuthorizationAuditRecorder authorizationAudit;
    private final AuditRequestIdProvider requestIds; private final ObjectMapper json; private final Clock clock;
    private final DirectManagerPolicy directManagerPolicy;
    private final UserSecurityMutationCoordinator securityMutations;

    public UserLifecycleServiceImpl(TUserLifecycleMapper lifecycle,TUserMapper users,TEmployeeMapper employees,
            TEmployeeAssignmentMapper assignments,TEmployeeReportingMapper reporting,TOrganizationUnitMapper organizations,
            TPositionMapper positions,TUserRoleMapper userRoles,TUserPermissionMapper userPermissions,
            TRoleMapper roles,TPermissionMapper permissions,
            TClueOwnerHistoryMapper clueOwnerHistory,TCustomerOwnerHistoryMapper customerOwnerHistory,
            TAuthorizationGraphLockMapper graphLocks,UserAuthorizationPolicy policy,CurrentUserProvider current,
            DataScopeResolver dataScopes,
            CredentialService credentials,OperationAuditRecorder audit,AuditRequestIdProvider requestIds,
            AuthorizationAuditRecorder authorizationAudit,ObjectMapper json,Clock clock,
            DirectManagerPolicy directManagerPolicy,
            UserSecurityMutationCoordinator securityMutations){
        this.lifecycle=lifecycle;this.users=users;this.employees=employees;this.assignments=assignments;this.reporting=reporting;
        this.organizations=organizations;this.positions=positions;this.userRoles=userRoles;this.userPermissions=userPermissions;
        this.roles=roles;this.permissions=permissions;
        this.clueOwnerHistory=clueOwnerHistory;this.customerOwnerHistory=customerOwnerHistory;
        this.graphLocks=graphLocks;this.policy=policy;this.current=current;this.dataScopes=dataScopes;this.credentials=credentials;this.audit=audit;
        this.requestIds=requestIds;this.authorizationAudit=authorizationAudit;this.json=json;this.clock=clock;
        this.directManagerPolicy=directManagerPolicy;
        this.securityMutations=securityMutations;
    }

    @Override public Context getContext(Integer userId,Integer targetOrganizationId){
        TUser target=requireManagedUser(userId); TEmployee employee=requireEmployee(userId);
        return context(target,employee,now(),targetOrganizationId);
    }

    @Override @Transactional(rollbackFor=Exception.class)
    public Context transfer(Integer userId,AssignmentCommand request){
        lockGraph("AUTHORIZATION_MEMBERSHIP_GUARD");lockGraph("ORGANIZATION_HIERARCHY");lockGraph("REPORTING_GRAPH");
        TUser target=lockManagedUser(userId);TEmployee employee=lockEmployee(userId);
        requireStatus(employee,EmployeeStatus.ACTIVE);requireVersion(employee,request.getEmployeeVersion());
        requireAuthority(PermissionCodes.USER_STATUS,"缺少用户状态管理权限");requireAuthority(PermissionCodes.EMPLOYEE_ASSIGNMENT,"缺少员工任职调整权限");
        requireAuthority(PermissionCodes.EMPLOYEE_REPORTING,"缺少汇报关系调整权限");
        LocalDateTime at=validateEffectiveAt(request.getEffectiveFrom());validateAssignmentChoice(employee,request,at);
        OrganizationHistoryState organizationBefore=organizationHistoryState(employee,at);
        String before=json(Map.of("status",employee.getEmploymentStatus().name(),"version",employee.getVersion(),"assignment",assignmentSnapshot(employee.getId(),at)));
        Integer operator=current.getCurrentUserId();
        if(employees.incrementVersionByExpected(employee.getId(),request.getEmployeeVersion(),at,operator)!=1)conflict();
        replaceAssignment(employee.getId(),request,at,operator);
        employee.setVersion(request.getEmployeeVersion()+1);
        recordOrganizationHistory(employee,organizationBefore,organizationHistoryState(employee,at),
                request.getReason(),"TRANSFER");
        String after=json(Map.of("status",employee.getEmploymentStatus().name(),"version",employee.getVersion(),"assignment",assignmentSnapshot(employee.getId(),at)));
        recordEvent(AuditActionEnum.USER_TRANSFER,"TRANSFER",target,employee,before,after,request.getReason(),at);
        if(users.incrementAuthVersion(userId)!=1)throw new BusinessException(CodeEnum.SYSTEM_ERROR,"调岗安全版本更新失败");
        securityMutations.accessChanged(userId,"员工调岗");
        return context(target,employee,at);
    }

    @Override @Transactional(rollbackFor=Exception.class)
    public DeparturePrecheck precheckDeparture(Integer userId,DeparturePrecheckRequest request){
        requireAuthority(PermissionCodes.USER_STATUS,"缺少用户状态管理权限");
        lockGraph("AUTHORIZATION_MEMBERSHIP_GUARD");lockGraph("ORGANIZATION_HIERARCHY");lockGraph("REPORTING_GRAPH");lockGraph("TEST_DRIVE_SCHEDULE_GUARD");
        TUser target=requireManagedUser(userId);TEmployee employee=requireEmployee(userId);
        if(employee.getEmploymentStatus()!=EmployeeStatus.ACTIVE&&employee.getEmploymentStatus()!=EmployeeStatus.HANDOVER)conflict();
        requireVersion(employee,request.getEmployeeVersion());
        return buildPrecheck(target,employee,request.getReason(),now(),false);
    }

    @Override @Transactional(rollbackFor=Exception.class)
    public Context startDeparture(Integer userId,StartDepartureRequest request){
        requireAuthority(PermissionCodes.USER_STATUS,"缺少用户状态管理权限");
        lockGraph("AUTHORIZATION_MEMBERSHIP_GUARD");lockGraph("ORGANIZATION_HIERARCHY");lockGraph("REPORTING_GRAPH");lockGraph("TEST_DRIVE_SCHEDULE_GUARD");lockGraph("AVAILABLE_ADMIN_GUARD");
        TUser target=lockManagedUser(userId);TEmployee employee=lockEmployee(userId);
        requireStatus(employee,EmployeeStatus.ACTIVE);requireVersion(employee,request.getEmployeeVersion());
        LocalDateTime at=now();DeparturePrecheck check=buildPrecheck(target,employee,request.getReason(),at,true);
        verifySnapshot(request.getSnapshotToken(),target,employee,request.getReason(),check,at);
        validateNotLastAdmin(target,at);
        String before=json(Map.of("employmentStatus","ACTIVE","employeeVersion",employee.getVersion()));
        if(lifecycle.transitionEmployee(employee.getId(),employee.getVersion(),"ACTIVE","HANDOVER",at,current.getCurrentUserId(),false,false)!=1)conflict();
        employee.setEmploymentStatus(EmployeeStatus.HANDOVER);employee.setVersion(employee.getVersion()+1);
        if(users.incrementAuthVersion(userId)!=1)throw new BusinessException(CodeEnum.SYSTEM_ERROR,"进入待交接后的安全版本更新失败");
        securityMutations.accessChanged(userId,"员工进入待交接");
        recordEvent(AuditActionEnum.USER_DEPARTURE_START,"DEPARTURE_START",target,employee,before,
                json(Map.of("employmentStatus","HANDOVER","employeeVersion",employee.getVersion())),request.getReason(),at);
        return context(target,employee,at);
    }

    @Override @Transactional(rollbackFor=Exception.class)
    public HandoverResult confirmHandover(Integer userId,ConfirmHandoverRequest request){
        requireAuthority(PermissionCodes.USER_STATUS,"缺少用户状态管理权限");
        lockGraph("AUTHORIZATION_MEMBERSHIP_GUARD");lockGraph("ORGANIZATION_HIERARCHY");lockGraph("REPORTING_GRAPH");lockGraph("TEST_DRIVE_SCHEDULE_GUARD");
        TUser source=lockManagedUser(userId);TEmployee employee=lockEmployee(userId);
        requireStatus(employee,EmployeeStatus.HANDOVER);requireVersion(employee,request.getEmployeeVersion());
        EnumMap<DirectResourceType,Integer> selections=validateSelections(request.getTransfers());
        List<ResponsibilityRow> activity=rows(DirectResourceType.ACTIVITY,userId,true),clue=rows(DirectResourceType.CLUE,userId,true),
                customer=rows(DirectResourceType.CUSTOMER,userId,true),opportunity=rows(DirectResourceType.OPPORTUNITY,userId,true),
                follow=rows(DirectResourceType.FOLLOW_TASK,userId,true),drives=rows(DirectResourceType.TEST_DRIVE,userId,true);
        EnumMap<DirectResourceType,List<ResponsibilityRow>> byDomain=new EnumMap<>(DirectResourceType.class);
        byDomain.put(DirectResourceType.ACTIVITY,activity);byDomain.put(DirectResourceType.CLUE,clue);byDomain.put(DirectResourceType.CUSTOMER,customer);
        byDomain.put(DirectResourceType.OPPORTUNITY,opportunity);byDomain.put(DirectResourceType.FOLLOW_TASK,follow);byDomain.put(DirectResourceType.TEST_DRIVE,drives);
        validateSelectionCoverage(selections,byDomain);
        LocalDateTime at=now();DeparturePrecheck check=buildPrecheckFromRows(source,employee,request.getReason(),at,byDomain,false);
        verifySnapshot(request.getSnapshotToken(),source,employee,request.getReason(),check,at);
        lockAndRevalidateTargets(source,selections,byDomain,at);
        String operationId=UUID.randomUUID().toString();List<DomainResult> results=new ArrayList<>();
        for(DirectResourceType domain:DOMAINS){
            List<ResponsibilityRow> domainRows=byDomain.get(domain);int expected=domainRows.size(),transferred=0;
            if(expected>0){Integer targetEmployeeId=selections.get(domain);if(targetEmployeeId==null)throw new BusinessException(CodeEnum.PARAM_ERROR,"有责任的交接域必须选择接收人");
                Integer targetUserId=requireTargetEmployee(targetEmployeeId).getUserId();requireOperatorAuthority(domain,userId,targetUserId);
                for(ResponsibilityRow row:domainRows){if(domain==DirectResourceType.TEST_DRIVE)assertNoScheduleConflict(targetUserId,row);
                    int changed=transferOne(domain,row,userId,targetUserId);if(changed==1)recordOwnerHistory(domain,row.getId(),userId,targetUserId,request.getReason(),at);transferred+=changed;}
            }
            if(transferred!=expected)throw new BusinessException(CodeEnum.USER_HANDOVER_COUNT_MISMATCH,DOMAIN_NAMES.get(domain)+"交接数量不一致");
            results.add(domainResult(domain,expected,transferred));
        }
        assertSourceResponsibilitiesEmpty(userId);
        if(employees.incrementVersionByExpected(employee.getId(),employee.getVersion(),at,current.getCurrentUserId())!=1)conflict();
        employee.setVersion(employee.getVersion()+1);
        String before=json(handoverEventSnapshot(byDomain,selections));
        recordEvent(operationId,AuditActionEnum.USER_HANDOVER_CONFIRM,"HANDOVER_CONFIRM",source,employee,before,
                json(Map.of("sourceCurrentResponsibilityCount",0,"domainResults",results)),request.getReason(),at);
        HandoverResult result=new HandoverResult();result.setOperationId(operationId);result.setSuccess(true);
        result.setResultCode("SUCCESS");result.setResultName("交接成功");result.setEmployeeVersion(employee.getVersion());result.setDomainResults(results);return result;
    }

    @Override @Transactional(rollbackFor=Exception.class)
    public Context completeDeparture(Integer userId,CompleteDepartureRequest request){
        requireAuthority(PermissionCodes.USER_STATUS,"缺少用户状态管理权限");
        lockGraph("AUTHORIZATION_MEMBERSHIP_GUARD");lockGraph("ORGANIZATION_HIERARCHY");lockGraph("REPORTING_GRAPH");lockGraph("AVAILABLE_ADMIN_GUARD");
        TUser target=lockManagedUser(userId);TEmployee employee=lockEmployee(userId);
        requireStatus(employee,EmployeeStatus.HANDOVER);requireVersion(employee,request.getEmployeeVersion());
        LocalDateTime at=now();DeparturePrecheck check=buildPrecheck(target,employee,request.getReason(),at,true);
        verifySnapshot(request.getSnapshotToken(),target,employee,request.getReason(),check,at);
        if(!check.isReadyToComplete())throw new BusinessException(CodeEnum.USER_LIFECYCLE_CONFLICT,"责任、下属或任职事实尚未满足完成离职条件");
        validateNotLastAdmin(target,at);
        OrganizationHistoryState organizationBefore=organizationHistoryState(employee,at);
        Map<String,Object> closureBefore=departureClosureSnapshot(employee.getId(),userId,at);closureBefore.put("employmentStatus","HANDOVER");closureBefore.put("employeeVersion",employee.getVersion());closureBefore.put("activeSessions",check.getActiveSessionCount());String before=json(closureBefore);
        closeAssignments(employee.getId(),at,current.getCurrentUserId());closeOwnReporting(employee.getId(),at,current.getCurrentUserId());
        OrganizationHistoryState organizationAfter=organizationHistoryState(employee,at);
        closeAuthorization(userId,at,request.getReason());
        if(users.incrementAuthorizationVersionsByExpected(userId,target.getAuthorizationVersion()==null?0:target.getAuthorizationVersion())!=1)conflict();
        if(users.updateAccountStatusByExpected(userId,target.getVersion(),"DISABLED",false,current.getCurrentUserId())!=1)conflict();
        if(lifecycle.transitionEmployee(employee.getId(),employee.getVersion(),"HANDOVER","LEFT",at,current.getCurrentUserId(),false,true)!=1)conflict();
        employee.setEmploymentStatus(EmployeeStatus.LEFT);employee.setVersion(employee.getVersion()+1);
        credentials.revokeAll(userId);
        securityMutations.accessChanged(userId,"完成员工离职");
        recordOrganizationHistory(employee,organizationBefore,organizationAfter,
                request.getReason(),"DEPARTURE_COMPLETE");
        recordEvent(AuditActionEnum.USER_DEPARTURE_COMPLETE,"DEPARTURE_COMPLETE",target,employee,before,
                json(Map.of("employmentStatus","LEFT","employeeVersion",employee.getVersion(),"activeRoles",0,"activePermissions",0,"sourceCurrentResponsibilityCount",0)),request.getReason(),at);
        return context(target,employee,at);
    }

    @Override @Transactional(rollbackFor=Exception.class)
    public RehireResult rehire(Integer userId,RehireRequest request){
        lockGraph("AUTHORIZATION_MEMBERSHIP_GUARD");lockGraph("ORGANIZATION_HIERARCHY");lockGraph("REPORTING_GRAPH");
        TUser target=lockManagedUser(userId);TEmployee employee=lockEmployee(userId);
        requireAuthority(PermissionCodes.USER_STATUS,"缺少用户状态管理权限");requireAuthority(PermissionCodes.EMPLOYEE_ASSIGNMENT,"缺少员工任职调整权限");requireAuthority(PermissionCodes.EMPLOYEE_REPORTING,"缺少汇报关系调整权限");
        requireStatus(employee,EmployeeStatus.LEFT);requireVersion(employee,request.getEmployeeVersion());
        validateRehireAccount(target,request.getAccountActivationMode());
        LocalDateTime at=validateEffectiveAt(request.getEffectiveFrom());validateAssignmentChoice(employee,request,at);
        if(lifecycle.countCurrentAndFutureRoles(userId,at)!=0||lifecycle.countCurrentAndFuturePermissions(userId,at)!=0)
            throw new BusinessException(CodeEnum.USER_LIFECYCLE_CONFLICT,"返聘前发现旧授权仍然有效");
        OrganizationHistoryState organizationBefore=organizationHistoryState(employee,at);
        String before=json(Map.of("employmentStatus","LEFT","employeeVersion",employee.getVersion(),"accountStatus",String.valueOf(target.getAccountStatus())));
        replaceAssignment(employee.getId(),request,at,current.getCurrentUserId());
        String accountStatus=request.getAccountActivationMode()==AccountActivationMode.RECOVER?"ACTIVE":"INVITED";
        boolean enabled=request.getAccountActivationMode()==AccountActivationMode.RECOVER;
        if(users.updateAccountStatusByExpected(userId,target.getVersion(),accountStatus,enabled,current.getCurrentUserId())!=1)conflict();
        if(lifecycle.transitionEmployee(employee.getId(),employee.getVersion(),"LEFT","ACTIVE",at,current.getCurrentUserId(),true,false)!=1)conflict();
        employee.setEmploymentStatus(EmployeeStatus.ACTIVE);employee.setVersion(employee.getVersion()+1);
        securityMutations.ownerEligibilityChanged();
        String deliveryStatus="NOT_REQUIRED";if(request.getAccountActivationMode()==AccountActivationMode.INVITE){ManagedDeliveryResult delivery=credentials.issueInvitation(userId,request.getReason());if(!delivery.accepted())throw new BusinessException(CodeEnum.CREDENTIAL_DELIVERY_FAILED,"返聘邀请凭证未成功投递");deliveryStatus=delivery.deliveryStatus();}
        recordOrganizationHistory(employee,organizationBefore,organizationHistoryState(employee,at),
                request.getReason(),"REHIRE");
        Map<String,Object> rehireAfter=new LinkedHashMap<>();rehireAfter.put("employmentStatus","ACTIVE");rehireAfter.put("employeeVersion",employee.getVersion());rehireAfter.put("accountStatus",accountStatus);rehireAfter.put("restoredLegacyAuthorizationCount",0);rehireAfter.put("assignment",assignmentSnapshot(employee.getId(),at));
        recordEvent(AuditActionEnum.USER_REHIRE,"REHIRE",target,employee,before,json(rehireAfter),request.getReason(),at);
        RehireResult result=new RehireResult();result.setContext(context(target,employee,at));result.setRestoredLegacyAuthorizationCount(0);
        result.setCredentialDeliveryStatus(deliveryStatus);return result;
    }

    private DeparturePrecheck buildPrecheck(TUser user,TEmployee employee,String reason,LocalDateTime at,boolean lock){
        EnumMap<DirectResourceType,List<ResponsibilityRow>> map=new EnumMap<>(DirectResourceType.class);
        for(DirectResourceType domain:DOMAINS)map.put(domain,rows(domain,user.getId(),lock));
        return buildPrecheckFromRows(user,employee,reason,at,map,!lock);
    }

    private DeparturePrecheck buildPrecheckFromRows(TUser user,TEmployee employee,String reason,LocalDateTime at,
                                                     EnumMap<DirectResourceType,List<ResponsibilityRow>> map,boolean issueToken){
        DeparturePrecheck out=new DeparturePrecheck();out.setGeneratedAt(offset(at));out.setExpiresAt(offset(at.plus(SNAPSHOT_TTL)));
        out.setUserId(user.getId());out.setEmploymentStatus(employee.getEmploymentStatus().name());out.setEmployeeVersion(employee.getVersion());
        int direct=0;for(DirectResourceType domain:DOMAINS){List<ResponsibilityRow> rows=map.get(domain);direct+=rows.size();out.getResponsibilities().add(summary(domain,user.getId(),rows,at));}
        int quotes=lifecycle.countActiveQuotesByOwner(user.getId()),trans=lifecycle.countActiveTransactionsByOwner(user.getId());
        out.getResponsibilities().add(derived("QUOTE","报价",quotes));out.getResponsibilities().add(derived("TRAN","交易",trans));
        out.setActiveRoleCount(lifecycle.countActiveRoles(user.getId(),at));out.setActivePersonalPermissionCount(lifecycle.countActivePersonalPermissions(user.getId(),at));
        out.setActiveSessionCount(lifecycle.countActiveSessions(user.getId(),at));out.setActiveAssignmentCount(lifecycle.countActiveAssignments(employee.getId(),at));
        out.setActiveReportingCount(lifecycle.countActiveReporting(employee.getId(),at));
        int subordinates=lifecycle.countCurrentAndFutureSubordinates(employee.getId(),at);if(subordinates>0)out.getBlockingReasons().add("仍有"+subordinates+"条当前或未来下属关系，必须先调整其直属管理者");
        int ledOrganizations=lifecycle.countEnabledLedOrganizations(employee.getId());if(ledOrganizations>0)out.getBlockingReasons().add("仍是"+ledOrganizations+"个启用组织的负责人，必须先调整组织负责人");
        boolean domainBlocked=out.getResponsibilities().stream().anyMatch(ResponsibilitySummary::isBlocking);
        out.setHandoverRequired(direct>0);out.setHandoverCompleted(direct==0);
        out.setReadyToComplete(employee.getEmploymentStatus()==EmployeeStatus.HANDOVER&&direct==0&&subordinates==0&&ledOrganizations==0);
        transitions(out.getStatusTransitions());
        if(employee.getEmploymentStatus()==EmployeeStatus.ACTIVE&&current.hasAuthority(PermissionCodes.USER_STATUS))out.getAllowedActions().add("DEPARTURE_START");
        if(employee.getEmploymentStatus()==EmployeeStatus.HANDOVER&&direct>0&&!domainBlocked)out.getAllowedActions().add("HANDOVER_CONFIRM");
        if(out.isReadyToComplete()&&current.hasAuthority(PermissionCodes.USER_STATUS))out.getAllowedActions().add("DEPARTURE_COMPLETE");
        if(current.hasAuthority(PermissionCodes.USER_STATUS))out.getAllowedActions().add("DEPARTURE_PRECHECK");
        if(domainBlocked)out.getUnavailableReasons().put("HANDOVER_CONFIRM","存在接收人资格、操作者范围或排期阻断");
        if(!out.isReadyToComplete())out.getUnavailableReasons().put("DEPARTURE_COMPLETE","仍有责任、下属或任职流程未完成");
        applyTransitionAvailability(out.getStatusTransitions(),out.getAllowedActions(),out.getUnavailableReasons(),employee.getEmploymentStatus().name());
        if(issueToken)out.setSnapshotToken(createSnapshot(user,employee,reason,out,at));return out;
    }

    private ResponsibilitySummary summary(DirectResourceType domain,Integer sourceUserId,List<ResponsibilityRow> rows,LocalDateTime at){
        ResponsibilitySummary out=new ResponsibilitySummary();out.setResourceType(domain.name());out.setResourceName(DOMAIN_NAMES.get(domain));out.setTransferMode("DIRECT_OWNER");
        out.setCount(rows.size());List<String> required=recipientPermissions(domain,rows,sourceUserId);
        List<HandoverCandidate> candidates=qualifiedCandidates(sourceUserId,required,at);
        if(domain==DirectResourceType.TEST_DRIVE&&!rows.isEmpty())for(HandoverCandidate candidate:candidates){Integer targetUserId=requireTargetEmployee(candidate.getId()).getUserId();int conflicts=0;
            for(ResponsibilityRow row:rows)if(row.getPlannedStartTime()!=null&&row.getPlannedEndTime()!=null)conflicts+=lifecycle.countTargetScheduleConflicts(targetUserId,row.getId(),row.getPlannedStartTime(),row.getPlannedEndTime());
            if(conflicts>0){candidate.setEligible(false);candidate.setQualificationCode("SCHEDULE_CONFLICT");candidate.setQualificationName("试驾排期冲突");candidate.setUnavailableReason("存在"+conflicts+"个重叠试驾时段");
                ResponsibilityConflict conflict=new ResponsibilityConflict();conflict.setConflictCode("TEST_DRIVE_SCHEDULE_CONFLICT");conflict.setConflictName("试驾排期冲突");conflict.setCount(conflicts);conflict.setReason("至少一个候选接收人存在重叠排期");out.getConflicts().add(conflict);}}
        if(!rows.isEmpty())for(HandoverCandidate candidate:candidates)if(candidate.isEligible()){
            Integer targetUserId=requireTargetEmployee(candidate.getId()).getUserId();String operatorCode=operatorPermission(domain);
            if(!current.hasAuthority(operatorCode)||!scopeCovers(operatorCode,sourceUserId,targetUserId)){candidate.setEligible(false);candidate.setQualificationCode("OPERATOR_SCOPE_DENIED");candidate.setQualificationName("操作者范围不覆盖交接双方");candidate.setUnavailableReason("接收人具备处理资格，但当前操作者无权执行该交接");}}
        out.setTargetCandidates(candidates);boolean hasEligible=candidates.stream().anyMatch(HandoverCandidate::isEligible);
        out.setTransferableCount(!rows.isEmpty()&&!hasEligible?0:rows.size());out.setBlockedCount(rows.size()-out.getTransferableCount());
        out.setBlocking(!rows.isEmpty()&&!hasEligible);out.setStatusCode(out.isBlocking()?"NO_QUALIFIED_TARGET":"READY");out.setStatusName(out.isBlocking()?"无合格接收人":"可交接");
        if(out.isBlocking())out.getBlockingReasons().add("没有同时满足在职、有效任职、账号可用和处理权限的接收人");return out;
    }

    private ResponsibilitySummary derived(String code,String name,int count){ResponsibilitySummary out=new ResponsibilitySummary();out.setResourceType(code);out.setResourceName(name);
        out.setTransferMode("DERIVED_IMPACT");out.setCount(count);out.setTransferableCount(count);out.setStatusCode("DERIVED_VALIDATED");out.setStatusName("随客户归属派生核验");return out;}

    private List<HandoverCandidate> qualifiedCandidates(Integer sourceUserId,List<String> permissions,LocalDateTime at){
        Set<Integer> scopedOrganizations=manageableOrganizationIds(at);List<HandoverCandidate> candidates=new ArrayList<>();
        for(HandoverCandidate candidate:lifecycle.selectQualifiedCandidates(sourceUserId,permissions,at)){
            TEmployee employee=employees.selectByPrimaryKey(candidate.getId());if(employee==null)continue;
            TEmployeeAssignment primary=assignments.selectCurrentPrimaryByEmployeeId(employee.getId(),at);
            if(primary==null||scopedOrganizations!=null&&!scopedOrganizations.contains(primary.getOrganizationUnitId()))continue;
            boolean handlesOwnData=true;for(String permission:permissions){AuthorizationDataScope scope=dataScopes.resolve(employee.getUserId(),permission);if(scope==null||(!scope.global()&&!scope.visibleUserIds().contains(employee.getUserId()))){handlesOwnData=false;break;}}
            if(!handlesOwnData)continue;
            candidate.setEligible(true);candidate.setQualificationCode("QUALIFIED");candidate.setQualificationName("具备接收资格");candidates.add(candidate);
        }return candidates;
    }

    private List<String> recipientPermissions(DirectResourceType domain,List<ResponsibilityRow> rows,Integer sourceUserId){
        LinkedHashSet<String> codes=new LinkedHashSet<>();switch(domain){
            case ACTIVITY->{codes.add(PermissionCodes.ACTIVITY_EDIT);if(rows.stream().anyMatch(r->"ENDED".equals(r.getStatus())))codes.add(PermissionCodes.ACTIVITY_REVIEW);}
            case CLUE->codes.add(PermissionCodes.CLUE_EDIT);
            case CUSTOMER->{codes.add(PermissionCodes.CUSTOMER_VIEW);if(lifecycle.countActiveQuotesByOwner(sourceUserId)>0)codes.add(PermissionCodes.QUOTE_EDIT);if(lifecycle.countActiveTransactionsByOwner(sourceUserId)>0)codes.add(PermissionCodes.TRAN_EDIT);}
            case OPPORTUNITY->codes.add(PermissionCodes.OPPORTUNITY_EDIT);
            case FOLLOW_TASK->codes.add(PermissionCodes.FOLLOW_TASK_UPDATE);
            case TEST_DRIVE->{if(rows.stream().anyMatch(r->!"CHECKED_IN".equals(r.getStatus())))codes.add(PermissionCodes.TEST_DRIVE_RESCHEDULE);
                if(rows.stream().anyMatch(r->"SCHEDULED".equals(r.getStatus())||"RESCHEDULED".equals(r.getStatus())))codes.add(PermissionCodes.TEST_DRIVE_CHECK_IN);
                if(rows.stream().anyMatch(r->"CHECKED_IN".equals(r.getStatus())))codes.add(PermissionCodes.TEST_DRIVE_COMPLETE);}
        }return new ArrayList<>(codes);
    }

    private void lockAndRevalidateTargets(TUser source,EnumMap<DirectResourceType,Integer> selections,
                                           EnumMap<DirectResourceType,List<ResponsibilityRow>> rows,LocalDateTime at){
        TreeSet<Integer> targetUserIds=new TreeSet<>();Map<Integer,Integer> employeeToUser=new HashMap<>();
        for(Integer employeeId:new TreeSet<>(selections.values())){TEmployee candidate=requireTargetEmployee(employeeId);if(candidate.getUserId()==null)throw new BusinessException(CodeEnum.USER_HANDOVER_QUALIFICATION_CHANGED);
            targetUserIds.add(candidate.getUserId());employeeToUser.put(employeeId,candidate.getUserId());}
        for(Integer targetUserId:targetUserIds){TUser locked=lifecycle.lockUserById(targetUserId);if(locked==null)throw new BusinessException(CodeEnum.USER_HANDOVER_QUALIFICATION_CHANGED);
            TEmployee lockedEmployee=lifecycle.lockEmployeeByUserId(targetUserId);if(lockedEmployee==null||lockedEmployee.getEmploymentStatus()!=EmployeeStatus.ACTIVE)throw new BusinessException(CodeEnum.USER_HANDOVER_QUALIFICATION_CHANGED);}
        for(var entry:selections.entrySet())if(!rows.get(entry.getKey()).isEmpty()){
            boolean qualified=qualifiedCandidates(source.getId(),recipientPermissions(entry.getKey(),rows.get(entry.getKey()),source.getId()),at).stream().anyMatch(c->c.getId().equals(entry.getValue()));
            if(!qualified)throw new BusinessException(CodeEnum.USER_HANDOVER_QUALIFICATION_CHANGED,DOMAIN_NAMES.get(entry.getKey())+"接收人资格已变化");
        }
    }

    private int transferOne(DirectResourceType domain,ResponsibilityRow row,Integer from,Integer to){return switch(domain){
        case ACTIVITY->lifecycle.transferActivity(row.getId(),from,to,row.getStatus());
        case CLUE->lifecycle.transferClue(row.getId(),from,to,row.getState());
        case CUSTOMER->lifecycle.transferCustomer(row.getId(),from,to,row.getStatus());
        case OPPORTUNITY->lifecycle.transferOpportunity(row.getId(),from,to,row.getStatus(),row.getVersion());
        case FOLLOW_TASK->lifecycle.transferFollowTask(row.getId(),from,to,row.getStatus(),row.getVersion());
        case TEST_DRIVE->lifecycle.transferTestDrive(row.getId(),from,to,row.getStatus(),row.getVersion());};}

    private List<ResponsibilityRow> rows(DirectResourceType domain,Integer owner,boolean lock){return switch(domain){
        case ACTIVITY->lifecycle.selectActivities(owner,lock);case CLUE->lifecycle.selectClues(owner,lock);case CUSTOMER->lifecycle.selectCustomers(owner,lock);
        case OPPORTUNITY->lifecycle.selectOpportunities(owner,lock);case FOLLOW_TASK->lifecycle.selectFollowTasks(owner,lock);case TEST_DRIVE->lifecycle.selectTestDrives(owner,lock);};}

    private void assertNoScheduleConflict(Integer target,ResponsibilityRow row){if(row.getPlannedStartTime()!=null&&row.getPlannedEndTime()!=null&&
            lifecycle.countTargetScheduleConflicts(target,row.getId(),row.getPlannedStartTime(),row.getPlannedEndTime())>0)
        throw new BusinessException(CodeEnum.USER_HANDOVER_SCHEDULE_CONFLICT,"目标接收人存在重叠试驾排期");}
    private void assertSourceResponsibilitiesEmpty(Integer userId){for(DirectResourceType domain:DOMAINS)if(!rows(domain,userId,false).isEmpty())
        throw new BusinessException(CodeEnum.USER_HANDOVER_COUNT_MISMATCH,"交接后仍存在"+DOMAIN_NAMES.get(domain)+"当前责任");}

    private Context context(TUser user,TEmployee employee,LocalDateTime at){return context(user,employee,at,null);}
    private Context context(TUser user,TEmployee employee,LocalDateTime at,Integer targetOrganizationId){Context out=new Context();out.setUserId(user.getId());out.setEmployeeId(employee.getId());
        out.setEmploymentStatus(employee.getEmploymentStatus().name());out.setEmployeeVersion(employee.getVersion());out.setCurrentAssignment(assignmentSummary(employee,at));
        out.setActiveRoleCount(lifecycle.countActiveRoles(user.getId(),at));out.setActivePersonalPermissionCount(lifecycle.countActivePersonalPermissions(user.getId(),at));
        out.setActiveSessionCount(lifecycle.countActiveSessions(user.getId(),at));out.setAdditionalAssignmentCount(lifecycle.countAdditionalAssignments(employee.getId(),at));out.setReportingRelationCount(lifecycle.countActiveReporting(employee.getId(),at));
        Set<Integer> manageableOrganizations=manageableOrganizationIds(at);
        for(TOrganizationUnit org:organizations.selectAll())if(Boolean.TRUE.equals(org.getEnabled())&&(manageableOrganizations==null||manageableOrganizations.contains(org.getId())))out.getOrganizationCandidates().add(candidate(org.getId(),org.getName(),org.getCode()));
        for(TPosition position:positions.selectManageable())if(Boolean.TRUE.equals(position.getEnabled()))out.getPositionCandidates().add(candidate(position.getId(),position.getName(),position.getCode()));
        TEmployeeAssignment targetPrimary=assignments.selectCurrentPrimaryByEmployeeId(employee.getId(),at);
        Integer managerAnchor=targetOrganizationId!=null?targetOrganizationId:(targetPrimary==null?null:targetPrimary.getOrganizationUnitId());
        if(managerAnchor!=null)for(TEmployee manager:directManagerPolicy.candidates(employee.getId(),managerAnchor,at))
            out.getManagerCandidates().add(candidate(manager.getId(),manager.getName(),manager.getEmployeeNo()));
        TOrganizationUnit managerOrganization=managerAnchor==null?null:organizations.selectByPrimaryKey(managerAnchor);
        boolean rootLeader=managerOrganization!=null&&managerOrganization.getType()==OrganizationUnitType.COMPANY
                && managerOrganization.getParentId()==null&&Objects.equals(managerOrganization.getLeaderEmployeeId(),employee.getId());
        out.setManagerRequired(!rootLeader);if(rootLeader)out.setManagerOptionalReason("根公司负责人无需直属管理者");
        for(HandoverCandidate value:qualifiedCandidates(user.getId(),List.of(),at))out.getHandoverCandidates().add(candidate(value.getId(),value.getLabel(),value.getSecondaryLabel()));
        transitions(out.getStatusTransitions());boolean statusAuthority=current.hasAuthority(PermissionCodes.USER_STATUS);
        switch(employee.getEmploymentStatus()){
            case ACTIVE->{if(statusAuthority&&current.hasAuthority(PermissionCodes.EMPLOYEE_ASSIGNMENT)&&current.hasAuthority(PermissionCodes.EMPLOYEE_REPORTING))out.getAllowedActions().add("TRANSFER");else out.getUnavailableReasons().put("TRANSFER","缺少账号状态、任职或汇报关系权限");if(statusAuthority)out.getAllowedActions().add("DEPARTURE_PRECHECK");else out.getUnavailableReasons().put("DEPARTURE_PRECHECK","缺少用户状态管理权限");}
            case HANDOVER->{if(statusAuthority)out.getAllowedActions().add("DEPARTURE_PRECHECK");else out.getUnavailableReasons().put("DEPARTURE_PRECHECK","缺少用户状态管理权限");}
            case LEFT->{if(statusAuthority&&current.hasAuthority(PermissionCodes.EMPLOYEE_ASSIGNMENT)&&current.hasAuthority(PermissionCodes.EMPLOYEE_REPORTING))out.getAllowedActions().add("REHIRE");else out.getUnavailableReasons().put("REHIRE","缺少账号状态、任职或汇报关系权限");}
            default->out.getUnavailableReasons().put("TRANSFER","待入职员工不能调岗");}
        applyTransitionAvailability(out.getStatusTransitions(),out.getAllowedActions(),out.getUnavailableReasons(),employee.getEmploymentStatus().name());
        return out;
    }

    private AssignmentSummary assignmentSummary(TEmployee employee,LocalDateTime at){TEmployeeAssignment primary=assignments.selectCurrentPrimaryByEmployeeId(employee.getId(),at);if(primary==null)return null;
        AssignmentSummary out=new AssignmentSummary();TOrganizationUnit org=organizations.selectByPrimaryKey(primary.getOrganizationUnitId());TPosition pos=positions.selectByPrimaryKey(primary.getPositionId());
        if(org!=null){out.setOrganizationCode(org.getCode());out.setOrganizationName(org.getName());}if(pos!=null){out.setPositionCode(pos.getCode());out.setPositionName(pos.getName());}
        TEmployeeReporting relation=reporting.selectCurrentDirectBySubordinateId(employee.getId(),at);if(relation!=null){TEmployee manager=employees.selectByPrimaryKey(relation.getManagerEmployeeId());if(manager!=null){out.setManagerEmployeeNo(manager.getEmployeeNo());out.setManagerName(manager.getName());}}
        out.setEffectiveFrom(offset(primary.getEffectiveFrom()));return out;}

    private void validateAssignmentChoice(TEmployee employee,AssignmentCommand request,LocalDateTime at){TOrganizationUnit org=organizations.selectByPrimaryKey(request.getOrganizationUnitId());TPosition pos=positions.selectByPrimaryKey(request.getPositionId());
        if(org==null||!Boolean.TRUE.equals(org.getEnabled())||Boolean.TRUE.equals(org.getMigrationPlaceholder()))throw new BusinessException(CodeEnum.ORGANIZATION_HIERARCHY_INVALID,"任职组织不可用");
        if(pos==null||!Boolean.TRUE.equals(pos.getEnabled())||Boolean.TRUE.equals(pos.getBuiltIn()))throw new BusinessException(CodeEnum.ASSIGNMENT_CONFLICT,"任职岗位不可用");
        Set<Integer> scope=manageableOrganizationIds(at);if(scope!=null&&!scope.contains(org.getId()))throw new BusinessException(CodeEnum.ACCESS_DENIED,"目标任职组织超出操作者范围");
        directManagerPolicy.validate(employee.getId(),request.getOrganizationUnitId(),request.getManagerEmployeeId(),at);}

    private void replaceAssignment(Integer employeeId,AssignmentCommand request,LocalDateTime at,Integer operator){closePrimaryAssignments(employeeId,at,operator);closeDirectReporting(employeeId,at,operator);
        TEmployeeAssignment assignment=new TEmployeeAssignment();assignment.setEmployeeId(employeeId);assignment.setOrganizationUnitId(request.getOrganizationUnitId());assignment.setPositionId(request.getPositionId());assignment.setAssignmentType(AssignmentType.PRIMARY);
        assignment.setStatus(AssignmentStatus.ACTIVE);assignment.setActivePrimaryMarker(true);assignment.setEffectiveFrom(at);assignment.setReason(request.getReason());assignment.setVersion(0);assignment.setCreateTime(at);assignment.setCreateBy(operator);
        if(assignments.insert(assignment)!=1)throw new BusinessException(CodeEnum.OPERATION_FAILED,"新任职事实写入失败");
        if(request.getManagerEmployeeId()!=null){TEmployeeReporting relation=new TEmployeeReporting();relation.setSubordinateEmployeeId(employeeId);relation.setManagerEmployeeId(request.getManagerEmployeeId());relation.setRelationType(ReportingType.DIRECT);relation.setStatus(ReportingStatus.ACTIVE);relation.setActiveDirectMarker(true);
            relation.setEffectiveFrom(at);relation.setReason(request.getReason());relation.setVersion(0);relation.setCreateTime(at);relation.setCreateBy(operator);if(reporting.insert(relation)!=1)throw new BusinessException(CodeEnum.OPERATION_FAILED,"新汇报关系写入失败");}}

    private void closeAssignments(Integer employeeId,LocalDateTime at,Integer operator){assignments.expireElapsedMarkers(employeeId,at,operator);for(TEmployeeAssignment fact:assignments.selectReplaceableByEmployeeId(employeeId,at)){
        int changed=!fact.getEffectiveFrom().isBefore(at)?assignments.cancelFutureByIdAndVersion(fact.getId(),fact.getVersion(),at,at,operator):assignments.endByIdAndVersion(fact.getId(),fact.getVersion(),at,at,operator);if(changed!=1)conflict();}}
    private void closePrimaryAssignments(Integer employeeId,LocalDateTime at,Integer operator){assignments.expireElapsedMarkers(employeeId,at,operator);for(TEmployeeAssignment fact:assignments.selectReplaceablePrimaryByEmployeeId(employeeId,at)){
        int changed=!fact.getEffectiveFrom().isBefore(at)?assignments.cancelFutureByIdAndVersion(fact.getId(),fact.getVersion(),at,at,operator):assignments.endByIdAndVersion(fact.getId(),fact.getVersion(),at,at,operator);if(changed!=1)conflict();}}
    private void closeOwnReporting(Integer employeeId,LocalDateTime at,Integer operator){reporting.expireElapsedMarkers(employeeId,at,operator);for(TEmployeeReporting fact:reporting.selectReplaceableBySubordinateId(employeeId,at)){
        int changed=!fact.getEffectiveFrom().isBefore(at)?reporting.cancelFutureByIdAndVersion(fact.getId(),fact.getVersion(),at,at,operator):reporting.endByIdAndVersion(fact.getId(),fact.getVersion(),at,at,operator);if(changed!=1)conflict();}}
    private void closeDirectReporting(Integer employeeId,LocalDateTime at,Integer operator){reporting.expireElapsedMarkers(employeeId,at,operator);for(TEmployeeReporting fact:reporting.selectReplaceableDirectBySubordinateId(employeeId,at)){
        int changed=!fact.getEffectiveFrom().isBefore(at)?reporting.cancelFutureByIdAndVersion(fact.getId(),fact.getVersion(),at,at,operator):reporting.endByIdAndVersion(fact.getId(),fact.getVersion(),at,at,operator);if(changed!=1)conflict();}}
    private void closeAuthorization(Integer userId,LocalDateTime at,String reason){List<TAuthorizationHistory> roleHistory=new ArrayList<>(),permissionHistory=new ArrayList<>();userRoles.expireElapsedMarkers(userId,at);for(TUserRole fact:userRoles.selectCurrentAndFutureByUserId(userId,at)){
        boolean future=fact.getEffectiveFrom()!=null&&!fact.getEffectiveFrom().isBefore(at);LocalDateTime end=future?fact.getEffectiveTo():at;
        if(userRoles.closeByIdAndVersion(fact.getId(),fact.getVersion(),at)!=1)conflict();TRole role=roles.selectByPrimaryKey(fact.getRoleId());TAuthorizationHistory history=new TAuthorizationHistory();history.setSubjectType(AuthorizationSubjectType.USER_ROLE);history.setSubjectId(userId+":"+fact.getRoleId());history.setChangeType(AuthorizationChangeType.REVOKE);history.setTargetUserId(userId);history.setRoleId(fact.getRoleId());history.setEffectiveFrom(fact.getEffectiveFrom());history.setEffectiveTo(end);history.setBeforeValue(json(Map.of("roleId",fact.getRoleId(),"roleCode",role==null?"":role.getRole(),"roleName",role==null?"":role.getRoleName(),"plannedEffectiveFrom",String.valueOf(fact.getEffectiveFrom()),"version",fact.getVersion())));history.setAfterValue(json(Map.of("active",false,"revokedAt",at)));history.setReason(reason);roleHistory.add(history);}
        for(TUserPermission fact:userPermissions.selectCurrentAndFutureByUserId(userId,at)){if(userPermissions.closeByIdAndVersion(fact.getId(),fact.getVersion(),at)!=1)conflict();TPermission permission=permissions.selectByPrimaryKey(fact.getPermissionId());TAuthorizationHistory history=new TAuthorizationHistory();history.setSubjectType(AuthorizationSubjectType.USER_PERMISSION);history.setSubjectId(userId+":"+fact.getPermissionId());history.setChangeType(AuthorizationChangeType.REVOKE);history.setTargetUserId(userId);history.setPermissionId(fact.getPermissionId());history.setEffect(fact.getEffect());history.setDataScopeCode(fact.getDataScopeCode());history.setEffectiveFrom(fact.getEffectiveFrom());history.setEffectiveTo(!fact.getEffectiveFrom().isBefore(at)?fact.getEffectiveTo():at);history.setBeforeValue(json(Map.of("permissionId",fact.getPermissionId(),"permissionCode",permission==null?"":permission.getCode(),"permissionName",permission==null?"":permission.getName(),"effect",fact.getEffect().name(),"plannedEffectiveFrom",fact.getEffectiveFrom(),"version",fact.getVersion())));history.setAfterValue(json(Map.of("active",false,"revokedAt",at)));history.setReason(reason);permissionHistory.add(history);}
        if(!roleHistory.isEmpty())authorizationAudit.recordAll(roleHistory,AuditActionEnum.USER_ROLE_CHANGE,String.valueOf(userId),json(Map.of("command","DEPARTURE_REVOKE","count",roleHistory.size())));
        if(!permissionHistory.isEmpty())authorizationAudit.recordAll(permissionHistory,AuditActionEnum.USER_PERMISSION_CHANGE,String.valueOf(userId),json(Map.of("command","DEPARTURE_REVOKE","count",permissionHistory.size())));}

    private EnumMap<DirectResourceType,Integer> validateSelections(List<TransferSelection> transfers){EnumMap<DirectResourceType,Integer> result=new EnumMap<>(DirectResourceType.class);
        if(transfers.isEmpty())throw new BusinessException(CodeEnum.PARAM_ERROR,"没有当前责任时无需执行交接确认");
        for(TransferSelection value:transfers)if(result.put(value.getResourceType(),value.getTargetEmployeeId())!=null)throw new BusinessException(CodeEnum.PARAM_ERROR,"交接责任域不能重复");return result;}
    private void validateSelectionCoverage(EnumMap<DirectResourceType,Integer> selections,EnumMap<DirectResourceType,List<ResponsibilityRow>> rows){for(DirectResourceType domain:DOMAINS){boolean required=!rows.get(domain).isEmpty(),provided=selections.containsKey(domain);
        if(required&&!provided)throw new BusinessException(CodeEnum.PARAM_ERROR,"有责任的交接域必须选择接收人");if(!required&&provided)throw new BusinessException(CodeEnum.PARAM_ERROR,"无当前责任的交接域不得提交接收人");}}
    private void requireOperatorAuthority(DirectResourceType domain,Integer sourceUserId,Integer targetUserId){String code=operatorPermission(domain);requireAuthority(code,"操作者缺少"+DOMAIN_NAMES.get(domain)+"交接执行权限");if(!scopeCovers(code,sourceUserId,targetUserId))throw new BusinessException(CodeEnum.ACCESS_DENIED,"操作者的"+DOMAIN_NAMES.get(domain)+"数据范围不覆盖交接双方");}
    private String operatorPermission(DirectResourceType domain){return switch(domain){case ACTIVITY->PermissionCodes.ACTIVITY_EDIT;case CLUE->PermissionCodes.CLUE_TRANSFER;case CUSTOMER->PermissionCodes.CUSTOMER_TRANSFER;case OPPORTUNITY->PermissionCodes.OPPORTUNITY_EDIT;case FOLLOW_TASK->PermissionCodes.FOLLOW_TASK_UPDATE;case TEST_DRIVE->PermissionCodes.TEST_DRIVE_RESCHEDULE;};}
    private boolean scopeCovers(String permissionCode,Integer sourceUserId,Integer targetUserId){AuthorizationDataScope scope=dataScopes.resolve(current.getCurrentUserId(),permissionCode);return scope!=null&&(scope.global()||(scope.visibleUserIds().contains(sourceUserId)&&scope.visibleUserIds().contains(targetUserId)));}
    private void requireAuthority(String code,String message){if(!current.hasAuthority(code))throw new BusinessException(CodeEnum.ACCESS_DENIED,message);}
    private void validateRehireAccount(TUser target,AccountActivationMode mode){LocalDateTime at=now();boolean credentialInvalid=!Integer.valueOf(1).equals(target.getCredentialsNoExpired())
            ||target.getPasswordExpiresAt()!=null&&!target.getPasswordExpiresAt().isAfter(at);if(!Integer.valueOf(1).equals(target.getAccountNoLocked())||Boolean.TRUE.equals(target.getManualLocked())
            ||target.getAutoLockedUntil()!=null&&target.getAutoLockedUntil().isAfter(at)||!Integer.valueOf(1).equals(target.getAccountNoExpired())
            ||target.getAccountExpiresAt()!=null&&!target.getAccountExpiresAt().isAfter(at)
            ||mode==AccountActivationMode.RECOVER&&credentialInvalid)
        throw new BusinessException(CodeEnum.USER_LIFECYCLE_CONFLICT,"返聘前必须先通过独立账号安全流程解除锁定和过期状态");
        if(mode==AccountActivationMode.INVITE)requireAuthority(PermissionCodes.USER_PASSWORD,"返聘邀请需要用户凭证管理权限");}

    private String createSnapshot(TUser user,TEmployee employee,String reason,DeparturePrecheck check,LocalDateTime at){byte[] raw=new byte[32];new SecureRandom().nextBytes(raw);String token=b64(raw);SnapshotFact fact=new SnapshotFact();fact.setTokenDigest(sha(token));fact.setUserId(user.getId());fact.setEmployeeId(employee.getId());fact.setEmployeeVersion(employee.getVersion());fact.setReasonDigest(sha(reason.trim()));fact.setFactDigest(snapshotFingerprint(user.getId(),check));fact.setExpiresAt(at.plus(SNAPSHOT_TTL));fact.setVersion(0);fact.setCreateTime(at);if(lifecycle.insertSnapshot(fact)!=1)throw new IllegalStateException("离职预检快照保存失败");return token;}
    private void verifySnapshot(String token,TUser user,TEmployee employee,String reason,DeparturePrecheck currentCheck,LocalDateTime at){SnapshotFact fact=lifecycle.lockSnapshotByDigest(sha(token));if(fact==null)invalidSnapshot();
        if(fact.getConsumedAt()!=null)invalidSnapshot();if(!fact.getExpiresAt().isAfter(at))throw new BusinessException(CodeEnum.USER_LIFECYCLE_SNAPSHOT_EXPIRED);
        if(!Objects.equals(fact.getUserId(),user.getId())||!Objects.equals(fact.getEmployeeId(),employee.getId())||!Objects.equals(fact.getEmployeeVersion(),employee.getVersion())||!Objects.equals(fact.getReasonDigest(),sha(reason.trim()))||!Objects.equals(fact.getFactDigest(),snapshotFingerprint(user.getId(),currentCheck)))invalidSnapshot();
        if(lifecycle.consumeSnapshot(fact.getId(),fact.getVersion(),at)!=1)invalidSnapshot();}
    private String factFingerprint(Integer userId){StringBuilder value=new StringBuilder();for(DirectResourceType domain:DOMAINS){value.append(domain.name());for(ResponsibilityRow row:rows(domain,userId,false))value.append(':').append(row.getId()).append(',').append(row.getOwnerId()).append(',').append(row.getStatus()).append(',').append(row.getState()).append(',').append(row.getVersion()).append(',').append(row.getPlannedStartTime()).append(',').append(row.getPlannedEndTime());value.append('|');}
        value.append("QUOTE:").append(lifecycle.countActiveQuotesByOwner(userId)).append("|TRAN:").append(lifecycle.countActiveTransactionsByOwner(userId));return sha(value.toString());}
    private String snapshotFingerprint(Integer userId,DeparturePrecheck check){TEmployee employee=requireEmployee(userId);StringBuilder value=new StringBuilder(factFingerprint(userId));List<String> lifecycleFacts=lifecycle.selectLifecycleFacts(userId,employee.getId(),now());if(lifecycleFacts!=null)for(String fact:lifecycleFacts)value.append("|FACT:").append(fact);value.append('|').append(check.getActiveRoleCount()).append('|').append(check.getActivePersonalPermissionCount()).append('|').append(check.getActiveSessionCount()).append('|').append(check.getActiveAssignmentCount()).append('|').append(check.getActiveReportingCount()).append('|').append(check.isReadyToComplete());
        for(String reason:check.getBlockingReasons())value.append('|').append(reason);for(ResponsibilitySummary item:check.getResponsibilities()){value.append('|').append(item.getResourceType()).append(':').append(item.getCount()).append(':').append(item.getTransferableCount()).append(':').append(item.getBlockedCount()).append(':').append(item.getStatusCode());for(HandoverCandidate candidate:item.getTargetCandidates())value.append(':').append(candidate.getId()).append(',').append(candidate.isEligible()).append(',').append(candidate.getQualificationCode());}return sha(value.toString());}
    private void invalidSnapshot(){throw new BusinessException(CodeEnum.USER_LIFECYCLE_CONFLICT,"离职预检快照无效、被篡改或事实已变化");}
    private String sha(String value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
    private String b64(byte[] value){return Base64.getUrlEncoder().withoutPadding().encodeToString(value);}

    private void recordEvent(AuditActionEnum action,String eventAction,TUser user,TEmployee employee,String before,String after,String reason,LocalDateTime at){recordEvent(UUID.randomUUID().toString(),action,eventAction,user,employee,before,after,reason,at);}
    private void recordEvent(String operationId,AuditActionEnum action,String eventAction,TUser user,TEmployee employee,String before,String after,String reason,LocalDateTime at){LifecycleEvent event=new LifecycleEvent();event.setOperationId(operationId);event.setRequestId(requestIds.currentRequestId());event.setAction(eventAction);event.setUserId(user.getId());event.setEmployeeId(employee.getId());event.setBeforeValue(before);event.setAfterValue(after);event.setReason(reason.trim());event.setOperatorId(current.getCurrentUserId());event.setOccurredTime(offset(at));
        if(lifecycle.insertEvent(event)!=1)throw new IllegalStateException("人员生命周期历史写入失败");audit.record(action,String.valueOf(user.getId()),"SUCCESS",json(Map.of("operationId",operationId,"employeeVersion",employee.getVersion())));}

    private TUser requireManagedUser(Integer userId){TUser target=users.selectByPrimaryKey(userId);if(target==null)throw new BusinessException(CodeEnum.NOT_FOUND,"用户不存在");policy.requireManage(target);return target;}
    private TUser lockManagedUser(Integer userId){TUser target=lifecycle.lockUserById(userId);if(target==null)throw new BusinessException(CodeEnum.NOT_FOUND,"用户不存在");policy.requireManage(target);return target;}
    private TEmployee requireEmployee(Integer userId){TEmployee value=employees.selectByUserId(userId);if(value==null)throw new BusinessException(CodeEnum.NOT_FOUND,"员工档案不存在");return value;}
    private TEmployee lockEmployee(Integer userId){TEmployee value=lifecycle.lockEmployeeByUserId(userId);if(value==null)throw new BusinessException(CodeEnum.NOT_FOUND,"员工档案不存在");return value;}
    private TEmployee requireTargetEmployee(Integer employeeId){TEmployee value=employees.selectByPrimaryKey(employeeId);if(value==null||value.getUserId()==null)throw new BusinessException(CodeEnum.USER_HANDOVER_QUALIFICATION_CHANGED,"交接接收员工不存在");return value;}
    private void requireStatus(TEmployee employee,EmployeeStatus status){if(employee.getEmploymentStatus()!=status)conflict();}
    private void requireVersion(TEmployee employee,Integer version){if(!Objects.equals(employee.getVersion(),version))conflict();}
    private void conflict(){throw new BusinessException(CodeEnum.USER_LIFECYCLE_CONFLICT);}
    private void lockGraph(String name){if(!name.equals(graphLocks.lockByName(name)))throw new IllegalStateException("生命周期串行化锁缺失: "+name);}
    private LocalDateTime now(){return LocalDateTime.ofInstant(clock.instant(),clock.getZone());}
    private LocalDateTime validateEffectiveAt(OffsetDateTime value){LocalDateTime at=LocalDateTime.ofInstant(value.toInstant(),BUSINESS_ZONE);LocalDateTime now=now();if(at.isAfter(now.plusSeconds(1)))throw new BusinessException(CodeEnum.ASSIGNMENT_CONFLICT,"本任务暂不接受未来生效任职");return now;}
    private OffsetDateTime offset(LocalDateTime value){return value.atZone(BUSINESS_ZONE).toOffsetDateTime();}
    private Candidate candidate(Integer id,String label,String secondary){Candidate value=new Candidate();value.setId(id);value.setLabel(label);value.setSecondaryLabel(secondary);return value;}
    private DomainResult domainResult(DirectResourceType domain,int expected,int transferred){DomainResult value=new DomainResult();value.setDomainCode(domain.name());value.setDomainName(DOMAIN_NAMES.get(domain));value.setExpectedCount(expected);value.setTransferredCount(transferred);value.setResultCode("SUCCESS");value.setResultName("交接成功");return value;}
    private void transitions(List<Transition> values){values.add(transition("TRANSFER","ACTIVE","ACTIVE","调岗"));values.add(transition("DEPARTURE_PRECHECK","ACTIVE","ACTIVE","离职预检"));values.add(transition("DEPARTURE_START","ACTIVE","HANDOVER","进入待交接"));values.add(transition("DEPARTURE_PRECHECK","HANDOVER","HANDOVER","重新预检"));values.add(transition("HANDOVER_CONFIRM","HANDOVER","HANDOVER","确认责任交接"));values.add(transition("DEPARTURE_COMPLETE","HANDOVER","LEFT","完成离职"));values.add(transition("REHIRE","LEFT","ACTIVE","返聘"));}
    private Transition transition(String action,String from,String to,String label){Transition value=new Transition();value.setAction(action);value.setFromStatus(from);value.setToStatus(to);value.setLabel(label);return value;}
    private void applyTransitionAvailability(List<Transition> transitions,List<String> allowed,Map<String,String> unavailable,String currentStatus){for(Transition transition:transitions)if(transition.getFromStatus().equals(currentStatus)&&!allowed.contains(transition.getAction()))transition.setDisabledReason(unavailable.getOrDefault(transition.getAction(),"当前权限或状态不允许执行"));}
    private Set<Integer> manageableOrganizationIds(LocalDateTime at){if(policy.isGlobalOperator())return null;TEmployee operator=employees.selectByUserId(current.getCurrentUserId());TEmployeeAssignment primary=operator==null?null:assignments.selectCurrentPrimaryByEmployeeId(operator.getId(),at);return primary==null?Set.of():new LinkedHashSet<>(organizations.selectDescendantIds(primary.getOrganizationUnitId()));}
    private boolean wouldCreateReportingCycle(Integer employeeId,Integer managerId,LocalDateTime at){Set<Integer> seen=new HashSet<>();Deque<Integer> pending=new ArrayDeque<>();pending.add(managerId);while(!pending.isEmpty()){Integer currentManager=pending.removeFirst();if(currentManager.equals(employeeId))return true;if(!seen.add(currentManager))continue;List<TEmployeeReporting> relations=reporting.selectReplaceableDirectBySubordinateId(currentManager,at);if(relations!=null)for(TEmployeeReporting relation:relations)if(relation.getManagerEmployeeId()!=null)pending.addLast(relation.getManagerEmployeeId());}return false;}
    private Object assignmentSnapshot(Integer employeeId,LocalDateTime at){TEmployeeAssignment primary=assignments.selectCurrentPrimaryByEmployeeId(employeeId,at);TEmployeeReporting relation=reporting.selectCurrentDirectBySubordinateId(employeeId,at);Map<String,Object> value=new LinkedHashMap<>();
        if(primary!=null){TOrganizationUnit org=organizations.selectByPrimaryKey(primary.getOrganizationUnitId());TPosition position=positions.selectByPrimaryKey(primary.getPositionId());value.put("organizationUnit",catalogSnapshot(primary.getOrganizationUnitId(),org==null?null:org.getCode(),org==null?null:org.getName()));value.put("position",catalogSnapshot(primary.getPositionId(),position==null?null:position.getCode(),position==null?null:position.getName()));}
        if(relation!=null){TEmployee manager=employees.selectByPrimaryKey(relation.getManagerEmployeeId());if(manager!=null)value.put("manager",employeeSnapshot(manager));}return value;}
    private OrganizationHistoryState organizationHistoryState(TEmployee employee,LocalDateTime at){
        return new OrganizationHistoryState(assignmentHistorySnapshot(employee,at),directReportingHistorySnapshot(employee.getId(),at),actingReportingHistorySnapshot(employee.getId(),at));}
    private String assignmentHistorySnapshot(TEmployee employee,LocalDateTime at){List<TEmployeeAssignment> facts=assignments.selectEffectiveByEmployeeId(employee.getId(),at);Map<String,Object> snapshot=new LinkedHashMap<>();
        snapshot.put("employee",historyRef(employee.getId(),employee.getEmployeeNo(),employee.getName()));
        TEmployeeAssignment primary=facts.stream().filter(value->value.getAssignmentType()==AssignmentType.PRIMARY).findFirst().orElse(null);
        snapshot.put("primaryAssignment",primary==null?Map.of():assignmentHistoryFact(primary));
        snapshot.put("additionalAssignments",facts.stream().filter(value->value.getAssignmentType()!=AssignmentType.PRIMARY).map(this::assignmentHistoryFact).toList());return json(snapshot);}
    private Map<String,Object> assignmentHistoryFact(TEmployeeAssignment fact){TOrganizationUnit organization=organizations.selectByPrimaryKey(fact.getOrganizationUnitId());TPosition position=positions.selectByPrimaryKey(fact.getPositionId());Map<String,Object> snapshot=new LinkedHashMap<>();
        snapshot.put("organizationUnit",historyRef(fact.getOrganizationUnitId(),organization==null?null:organization.getCode(),organization==null?null:organization.getName()));
        snapshot.put("position",historyRef(fact.getPositionId(),position==null?null:position.getCode(),position==null?null:position.getName()));
        snapshot.put("assignmentType",fact.getAssignmentType()==null?null:fact.getAssignmentType().name());snapshot.put("effectiveFrom",historyTime(fact.getEffectiveFrom()));snapshot.put("effectiveTo",historyTime(fact.getEffectiveTo()));return snapshot;}
    private String directReportingHistorySnapshot(Integer employeeId,LocalDateTime at){TEmployeeReporting relation=reporting.selectCurrentDirectBySubordinateId(employeeId,at);if(relation==null)return null;TEmployee manager=employees.selectByPrimaryKey(relation.getManagerEmployeeId());Map<String,Object> snapshot=new LinkedHashMap<>();
        snapshot.put("manager",historyRef(relation.getManagerEmployeeId(),manager==null?null:manager.getEmployeeNo(),manager==null?null:manager.getName()));snapshot.put("relationType",relation.getRelationType()==null?null:relation.getRelationType().name());snapshot.put("effectiveFrom",historyTimeText(relation.getEffectiveFrom()));snapshot.put("effectiveTo",historyTimeText(relation.getEffectiveTo()));return json(snapshot);}
    private String actingReportingHistorySnapshot(Integer employeeId,LocalDateTime at){List<Map<String,Object>> snapshot=new ArrayList<>();for(TEmployeeReporting relation:reporting.selectCurrentAndFutureActingBySubordinateId(employeeId,at)){TEmployee manager=employees.selectByPrimaryKey(relation.getManagerEmployeeId());Map<String,Object> item=new LinkedHashMap<>();item.put("relationId",relation.getId());item.put("managerEmployeeId",relation.getManagerEmployeeId());item.put("managerEmployeeNo",manager==null?null:manager.getEmployeeNo());item.put("managerName",manager==null?null:manager.getName());item.put("relationType",ReportingType.ACTING.name());item.put("effectiveFrom",historyTimeText(relation.getEffectiveFrom()));item.put("effectiveTo",historyTimeText(relation.getEffectiveTo()));snapshot.add(item);}return json(snapshot);}
    private void recordOrganizationHistory(TEmployee employee,OrganizationHistoryState before,OrganizationHistoryState after,String reason,String command){List<TAuthorizationHistory> histories=new ArrayList<>();
        if(!Objects.equals(before.assignments(),after.assignments()))histories.add(organizationHistory(AuthorizationSubjectType.ORGANIZATION_ASSIGNMENT,employee,before.assignments(),after.assignments(),reason));
        if(!Objects.equals(before.directReporting(),after.directReporting()))histories.add(organizationHistory(AuthorizationSubjectType.REPORTING_RELATION,employee,before.directReporting(),after.directReporting(),reason));
        if(!Objects.equals(before.actingReportings(),after.actingReportings()))histories.add(organizationHistory(AuthorizationSubjectType.REPORTING_RELATION,employee,before.actingReportings(),after.actingReportings(),reason));
        if(!histories.isEmpty())authorizationAudit.recordAll(histories,AuditActionEnum.EMPLOYEE_ASSIGNMENT_CHANGE,String.valueOf(employee.getId()),json(Map.of("command",command,"employeeId",employee.getId(),"historyCount",histories.size())));}
    private TAuthorizationHistory organizationHistory(AuthorizationSubjectType subjectType,TEmployee employee,String before,String after,String reason){TAuthorizationHistory history=new TAuthorizationHistory();history.setSubjectType(subjectType);history.setSubjectId(String.valueOf(employee.getId()));history.setChangeType(AuthorizationChangeType.UPDATE);history.setTargetUserId(employee.getUserId());history.setBeforeValue(before);history.setAfterValue(after);history.setReason(reason);return history;}
    private Map<String,Object> historyRef(Integer id,String code,String name){Map<String,Object> ref=new LinkedHashMap<>();ref.put("id",id);ref.put("code",code);ref.put("name",name);return ref;}
    private OffsetDateTime historyTime(LocalDateTime value){return value==null?null:offset(value);}
    private String historyTimeText(LocalDateTime value){OffsetDateTime time=historyTime(value);return time==null?null:time.toString();}
    private Object handoverEventSnapshot(EnumMap<DirectResourceType,List<ResponsibilityRow>> rows,EnumMap<DirectResourceType,Integer> targets){Map<String,Object> value=new LinkedHashMap<>();for(DirectResourceType domain:DOMAINS){Map<String,Object> item=new LinkedHashMap<>();Integer targetId=targets.get(domain);if(targetId!=null){TEmployee target=employees.selectByPrimaryKey(targetId);if(target!=null)item.put("targetEmployee",employeeSnapshot(target));}item.put("responsibilities",rows.get(domain));value.put(domain.name(),item);}return value;}
    private Map<String,Object> catalogSnapshot(Integer id,String code,String name){Map<String,Object> value=new LinkedHashMap<>();value.put("id",id);value.put("code",code);value.put("name",name);return value;}
    private Map<String,Object> employeeSnapshot(TEmployee employee){Map<String,Object> value=new LinkedHashMap<>();value.put("id",employee.getId());value.put("userId",employee.getUserId());value.put("employeeNo",employee.getEmployeeNo());value.put("name",employee.getName());return value;}
    private Map<String,Object> departureClosureSnapshot(Integer employeeId,Integer userId,LocalDateTime at){Map<String,Object> value=new LinkedHashMap<>();List<Object> assignmentFacts=new ArrayList<>();for(TEmployeeAssignment fact:assignments.selectReplaceableByEmployeeId(employeeId,at)){TOrganizationUnit org=organizations.selectByPrimaryKey(fact.getOrganizationUnitId());TPosition position=positions.selectByPrimaryKey(fact.getPositionId());Map<String,Object> item=new LinkedHashMap<>();item.put("id",fact.getId());item.put("assignmentType",fact.getAssignmentType().name());item.put("status",fact.getStatus().name());item.put("organizationUnit",catalogSnapshot(fact.getOrganizationUnitId(),org==null?null:org.getCode(),org==null?null:org.getName()));item.put("position",catalogSnapshot(fact.getPositionId(),position==null?null:position.getCode(),position==null?null:position.getName()));item.put("effectiveFrom",fact.getEffectiveFrom());assignmentFacts.add(item);}value.put("assignments",assignmentFacts);
        List<Object> reportingFacts=new ArrayList<>();for(TEmployeeReporting fact:reporting.selectReplaceableBySubordinateId(employeeId,at)){TEmployee manager=employees.selectByPrimaryKey(fact.getManagerEmployeeId());Map<String,Object> item=new LinkedHashMap<>();item.put("id",fact.getId());item.put("relationType",fact.getRelationType().name());item.put("status",fact.getStatus().name());if(manager!=null)item.put("manager",employeeSnapshot(manager));item.put("effectiveFrom",fact.getEffectiveFrom());reportingFacts.add(item);}value.put("reportingRelations",reportingFacts);
        List<Object> roleFacts=new ArrayList<>();for(TUserRole fact:userRoles.selectCurrentAndFutureByUserId(userId,at)){TRole role=roles.selectByPrimaryKey(fact.getRoleId());roleFacts.add(Map.of("roleId",fact.getRoleId(),"roleCode",role==null?"":role.getRole(),"roleName",role==null?"":role.getRoleName(),"effectiveFrom",String.valueOf(fact.getEffectiveFrom())));}value.put("roles",roleFacts);
        List<Object> permissionFacts=new ArrayList<>();for(TUserPermission fact:userPermissions.selectCurrentAndFutureByUserId(userId,at)){TPermission permission=permissions.selectByPrimaryKey(fact.getPermissionId());permissionFacts.add(Map.of("permissionId",fact.getPermissionId(),"permissionCode",permission==null?"":permission.getCode(),"permissionName",permission==null?"":permission.getName(),"effect",fact.getEffect().name(),"effectiveFrom",String.valueOf(fact.getEffectiveFrom())));}value.put("personalPermissions",permissionFacts);return value;}
    private void validateNotLastAdmin(TUser target,LocalDateTime at){boolean admin=users.selectRolesByUserId(target.getId()).stream().anyMatch(role->"admin".equals(role.getRole()));if(admin&&users.countAvailableAdminUsersExcluding(target.getId())<1)throw new BusinessException(CodeEnum.ACCESS_DENIED,"最后一个有效普通管理员不能离职");}
    private void recordOwnerHistory(DirectResourceType domain,Long resourceId,Integer from,Integer to,String reason,LocalDateTime at){if(domain==DirectResourceType.CLUE){TClueOwnerHistory value=new TClueOwnerHistory();value.setClueId(resourceId.intValue());value.setFromOwnerId(from);value.setToOwnerId(to);value.setAssignedBy(current.getCurrentUserId());value.setReason(reason);value.setAssignedTime(Date.from(at.atZone(BUSINESS_ZONE).toInstant()));if(clueOwnerHistory.insert(value)!=1)throw new IllegalStateException("线索负责人历史写入失败");}
        if(domain==DirectResourceType.CUSTOMER){TCustomerOwnerHistory value=new TCustomerOwnerHistory();value.setCustomerId(resourceId.intValue());value.setFromOwnerId(from);value.setToOwnerId(to);value.setOperatorId(current.getCurrentUserId());value.setReason(reason);value.setTransferTime(Date.from(at.atZone(BUSINESS_ZONE).toInstant()));if(customerOwnerHistory.insert(value)!=1)throw new IllegalStateException("客户负责人历史写入失败");}}
    private boolean isBlocking(DeparturePrecheck check){return !check.getBlockingReasons().isEmpty()||check.getResponsibilities().stream().anyMatch(ResponsibilitySummary::isBlocking);}
    private String json(Object value){try{return json.writeValueAsString(value);}catch(JsonProcessingException e){throw new IllegalStateException("生命周期快照序列化失败",e);}}
    private record OrganizationHistoryState(String assignments,String directReporting,String actingReportings){}
}
