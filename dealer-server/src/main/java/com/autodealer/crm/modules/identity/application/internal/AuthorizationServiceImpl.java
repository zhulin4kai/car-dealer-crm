package com.autodealer.crm.modules.identity.application.internal;

import com.autodealer.crm.modules.identity.application.api.enums.AuthorizationChangeType;
import com.autodealer.crm.modules.identity.application.api.enums.AuthorizationSubjectType;
import com.autodealer.crm.modules.identity.application.api.enums.DataScopeCode;
import com.autodealer.crm.modules.identity.application.api.enums.EmployeeStatus;
import com.autodealer.crm.modules.identity.application.api.enums.PermissionEffect;
import com.autodealer.crm.modules.identity.application.api.model.TUser;
import com.autodealer.crm.modules.identity.persistence.mapper.TAuthorizationGraphLockMapper;
import com.autodealer.crm.modules.identity.persistence.mapper.TEmployeeAssignmentMapper;
import com.autodealer.crm.modules.identity.persistence.mapper.TEmployeeMapper;
import com.autodealer.crm.modules.identity.persistence.mapper.TOrganizationUnitMapper;
import com.autodealer.crm.modules.identity.persistence.mapper.TPermissionMapper;
import com.autodealer.crm.modules.identity.persistence.mapper.TPositionMapper;
import com.autodealer.crm.modules.identity.persistence.mapper.TRoleMapper;
import com.autodealer.crm.modules.identity.persistence.mapper.TRolePermissionMapper;
import com.autodealer.crm.modules.identity.persistence.mapper.TRolePermissionOrganizationMapper;
import com.autodealer.crm.modules.identity.persistence.mapper.TUserMapper;
import com.autodealer.crm.modules.identity.persistence.mapper.TUserPermissionMapper;
import com.autodealer.crm.modules.identity.persistence.mapper.TUserPermissionOrganizationMapper;
import com.autodealer.crm.modules.identity.persistence.mapper.TUserRoleMapper;
import com.autodealer.crm.modules.identity.persistence.model.TAuthorizationHistory;
import com.autodealer.crm.modules.identity.persistence.model.TEmployee;
import com.autodealer.crm.modules.identity.persistence.model.TEmployeeAssignment;
import com.autodealer.crm.modules.identity.persistence.model.TOrganizationUnit;
import com.autodealer.crm.modules.identity.persistence.model.TPermission;
import com.autodealer.crm.modules.identity.persistence.model.TPosition;
import com.autodealer.crm.modules.identity.persistence.model.TRole;
import com.autodealer.crm.modules.identity.persistence.model.TRolePermission;
import com.autodealer.crm.modules.identity.persistence.model.TUserPermission;
import com.autodealer.crm.modules.identity.persistence.model.TUserPermissionOrganization;
import com.autodealer.crm.modules.identity.persistence.model.TUserRole;
import com.autodealer.crm.modules.audit.application.api.AuditActionEnum;
import com.autodealer.crm.modules.identity.application.api.AuthorizationAuditRecorder;
import com.autodealer.crm.modules.identity.application.api.security.CurrentUserProvider;
import com.autodealer.crm.modules.identity.application.api.dto.access.UserAuthorizationDtos.*;
import com.autodealer.crm.modules.identity.application.api.enums.*;
import com.autodealer.crm.shared.error.BusinessException;
import com.autodealer.crm.modules.identity.persistence.mapper.*;
import com.autodealer.crm.modules.identity.persistence.model.*;
import com.autodealer.crm.modules.identity.application.api.model.*;
import com.autodealer.crm.shared.error.CodeEnum;
import com.autodealer.crm.modules.identity.application.api.AuthorizationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;

@Service
public class AuthorizationServiceImpl implements AuthorizationService {
    private static final ZoneId ZONE = ZoneId.systemDefault();
    private static final int MAX_PERMISSION_SCHEDULE_YEARS = 1;
    private static final ObjectMapper HISTORY_OBJECT_MAPPER = new ObjectMapper().findAndRegisterModules();
    private final TUserMapper userMapper; private final TEmployeeMapper employeeMapper;
    private final TEmployeeAssignmentMapper assignmentMapper; private final TOrganizationUnitMapper organizationMapper;
    private final TPositionMapper positionMapper; private final TRoleMapper roleMapper;
    private final TRolePermissionMapper rolePermissionMapper; private final TPermissionMapper permissionMapper;
    private final TUserRoleMapper userRoleMapper; private final TUserPermissionMapper userPermissionMapper;
    private final TUserPermissionOrganizationMapper userPermissionOrganizationMapper;
    private final TRolePermissionOrganizationMapper rolePermissionOrganizationMapper;
    private final UserAuthorizationPolicy policy; private final CurrentUserProvider currentUserProvider;
    private final AuthorizationAuditRecorder auditRecorder;
    private final TAuthorizationGraphLockMapper graphLock;
    private final UserSecurityMutationCoordinator securityMutations;

    public AuthorizationServiceImpl(TUserMapper userMapper, TEmployeeMapper employeeMapper,
                                    TEmployeeAssignmentMapper assignmentMapper, TOrganizationUnitMapper organizationMapper,
                                    TPositionMapper positionMapper, TRoleMapper roleMapper,
                                    TRolePermissionMapper rolePermissionMapper, TPermissionMapper permissionMapper,
                                    TUserRoleMapper userRoleMapper, TUserPermissionMapper userPermissionMapper,
                                    TUserPermissionOrganizationMapper userPermissionOrganizationMapper,
                                    TRolePermissionOrganizationMapper rolePermissionOrganizationMapper,
                                    UserAuthorizationPolicy policy, CurrentUserProvider currentUserProvider,
                                    AuthorizationAuditRecorder auditRecorder,
                                    TAuthorizationGraphLockMapper graphLock,
                                    UserSecurityMutationCoordinator securityMutations) {
        this.userMapper=userMapper; this.employeeMapper=employeeMapper; this.assignmentMapper=assignmentMapper;
        this.organizationMapper=organizationMapper; this.positionMapper=positionMapper; this.roleMapper=roleMapper;
        this.rolePermissionMapper=rolePermissionMapper; this.permissionMapper=permissionMapper;
        this.userRoleMapper=userRoleMapper; this.userPermissionMapper=userPermissionMapper;
        this.userPermissionOrganizationMapper=userPermissionOrganizationMapper;
        this.rolePermissionOrganizationMapper=rolePermissionOrganizationMapper; this.policy=policy;
        this.currentUserProvider=currentUserProvider; this.auditRecorder=auditRecorder; this.graphLock=graphLock;
        this.securityMutations=securityMutations;
    }

    @Override public Detail get(Integer userId) {
        TUser target = requireUser(userId); policy.requireView(target); return build(target, LocalDateTime.now());
    }

    @Override @Transactional(rollbackFor = Exception.class)
    public Detail replaceRoles(Integer userId, UpdateRolesRequest request) {
        lockMembership();
        lockGraph("ORGANIZATION_HIERARCHY");
        lockGraph("REPORTING_GRAPH");
        lockGraph("AVAILABLE_ADMIN_GUARD");
        LocalDateTime now = LocalDateTime.now();
        LinkedHashSet<Integer> requested=uniqueIds(request.getRoleIds(),"角色不能重复");
        List<TAuthorizationHistory> histories = new ArrayList<>();
        AppliedTarget applied=replaceRolesLocked(userId,request.getAuthorizationVersion(),requested,request.getReason(),now,histories);
        if(applied.removedAdmin())requireAvailableAdmin();
        if(applied.changed()){
            auditRecorder.recordAll(histories,AuditActionEnum.USER_ROLE_CHANGE,String.valueOf(userId),
                    json(Map.of("command","ROLE_REPLACE","count",histories.size())));
            scheduleCleanup(userId);
        }
        return build(applied.target(),now);
    }

    @Override @Transactional(rollbackFor = Exception.class)
    public Detail updatePermissions(Integer userId, UpdatePermissionsRequest request) {
        lockMembership();
        lockGraph("ORGANIZATION_HIERARCHY");
        lockGraph("REPORTING_GRAPH");
        LocalDateTime now = LocalDateTime.now();
        validateUniquePermissionChanges(request.getChanges());
        List<TAuthorizationHistory> histories = new ArrayList<>();
        AppliedTarget applied=updatePermissionsLocked(userId,request.getAuthorizationVersion(),request.getChanges(),
                request.getReason(),now,histories);
        if(applied.changed()){
            auditRecorder.recordAll(histories,AuditActionEnum.USER_PERMISSION_CHANGE,String.valueOf(userId),
                    json(Map.of("command","PERMISSION_UPDATE","count",histories.size())));
            scheduleCleanup(userId);
        }
        return build(applied.target(),now);
    }

    @Override @Transactional(rollbackFor = Exception.class)
    public BatchResult batchUpdateRoles(BatchUpdateRolesRequest request){
        lockMembership();lockGraph("ORGANIZATION_HIERARCHY");lockGraph("REPORTING_GRAPH");lockGraph("AVAILABLE_ADMIN_GUARD");
        LocalDateTime now=LocalDateTime.now();List<BatchTarget> targets=uniqueTargets(request.getTargets());
        LinkedHashSet<Integer> roleIds=uniqueIds(request.getRoleIds(),"角色不能重复");
        List<TAuthorizationHistory> histories=new ArrayList<>();List<AppliedTarget> applied=new ArrayList<>();
        for(BatchTarget target:targets){
            userRoleMapper.expireElapsedMarkers(target.getUserId(),now);
            List<TUserRole> facts=userRoleMapper.selectCurrentAndFutureByUserId(target.getUserId(),now);
            LinkedHashSet<Integer> requested=facts.stream().map(TUserRole::getRoleId)
                    .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
            if(request.getOperation()==BatchRoleOperation.ASSIGN)requested.addAll(roleIds);else requested.removeAll(roleIds);
            applied.add(replaceRolesLocked(target.getUserId(),target.getAuthorizationVersion(),requested,request.getReason(),now,histories));
        }
        if(applied.stream().anyMatch(AppliedTarget::removedAdmin))requireAvailableAdmin();
        return finishBatch(applied,histories,AuditActionEnum.USER_ROLE_CHANGE,"ROLE_"+request.getOperation());
    }

    @Override @Transactional(rollbackFor = Exception.class)
    public BatchResult batchUpdatePermissions(BatchUpdatePermissionsRequest request){
        lockMembership();lockGraph("ORGANIZATION_HIERARCHY");lockGraph("REPORTING_GRAPH");
        LocalDateTime now=LocalDateTime.now();List<BatchTarget> targets=uniqueTargets(request.getTargets());
        validateUniquePermissionChanges(request.getChanges());
        List<TAuthorizationHistory> histories=new ArrayList<>();List<AppliedTarget> applied=new ArrayList<>();
        for(BatchTarget target:targets)applied.add(updatePermissionsLocked(target.getUserId(),target.getAuthorizationVersion(),
                request.getChanges(),request.getReason(),now,histories));
        return finishBatch(applied,histories,AuditActionEnum.USER_PERMISSION_CHANGE,"PERMISSION_BATCH_UPDATE");
    }

    private AppliedTarget replaceRolesLocked(Integer userId,Integer expectedVersion,Set<Integer> requested,
                                             String reason,LocalDateTime now,List<TAuthorizationHistory> histories){
        TUser target=requireUser(userId);requireAuthorizationEligible(userId);userRoleMapper.expireElapsedMarkers(userId,now);
        List<TUserRole> existingFacts=userRoleMapper.selectCurrentAndFutureByUserId(userId,now);
        Set<Integer> existing=existingFacts.stream().map(TUserRole::getRoleId).collect(java.util.stream.Collectors.toSet());
        Set<Integer> added=new LinkedHashSet<>(requested);added.removeAll(existing);
        Set<Integer> removed=new LinkedHashSet<>(existing);removed.removeAll(requested);
        Set<Integer> changedRoleIds=union(added,removed);
        boolean protectedAdministratorOnly=!changedRoleIds.isEmpty()
                && changedRoleIds.stream().allMatch(this::isProtectedAdministratorRoleId);
        policy.requireRoleManage(target,protectedAdministratorOnly);
        if(added.isEmpty()&&removed.isEmpty()){requireExpectedVersion(target,expectedVersion);return new AppliedTarget(target,false,false);}
        for(Integer roleId:added)if(!policy.canDelegateRole(roleMapper.selectByPrimaryKey(roleId),target))
            throw new BusinessException(CodeEnum.ACCESS_DENIED,"新增角色超过操作者委派上限或目标适用范围");
        for(Integer roleId:removed)if(!policy.canRevokeRole(roleMapper.selectByPrimaryKey(roleId),target))
            throw new BusinessException(CodeEnum.ACCESS_DENIED,"不能撤销受保护角色或超过操作者级别的角色");
        casTarget(target,expectedVersion);
        for(TUserRole fact:existingFacts)if(removed.contains(fact.getRoleId())){
            if(userRoleMapper.closeByIdAndVersion(fact.getId(),fact.getVersion(),now)!=1)
                throw new BusinessException(CodeEnum.ROLE_VERSION_CONFLICT,"用户角色事实已被并发修改");
        }
        for(Integer roleId:added){TUserRole value=new TUserRole();value.setUserId(userId);value.setRoleId(roleId);
            value.setGrantedBy(currentUserProvider.getCurrentUserId());value.setReason(reason);value.setEffectiveFrom(now);
            value.setActiveMarker(true);value.setVersion(0);if(userRoleMapper.insert(value)!=1)
                throw new BusinessException(CodeEnum.SYSTEM_ERROR,"角色分配写入失败");}
        added.forEach(roleId->histories.add(roleHistory(userId,roleId,AuthorizationChangeType.ASSIGN,reason,now)));
        removed.forEach(roleId->histories.add(roleHistory(userId,roleId,AuthorizationChangeType.UNASSIGN,reason,now)));
        boolean removedAdmin=removed.stream().map(roleMapper::selectByPrimaryKey).filter(Objects::nonNull)
                .anyMatch(role->"admin".equals(role.getRole()));
        target.setAuthorizationVersion(expectedVersion+1);return new AppliedTarget(target,true,removedAdmin);
    }

    private AppliedTarget updatePermissionsLocked(Integer userId,Integer expectedVersion,List<PermissionChange> changes,
                                                   String reason,LocalDateTime now,List<TAuthorizationHistory> histories){
        TUser target=requireUser(userId);policy.requireAuthorizationManage(target);requireAuthorizationEligible(userId);
        if(changes.isEmpty()){requireExpectedVersion(target,expectedVersion);return new AppliedTarget(target,false,false);}
        for(PermissionChange change:changes)validatePermissionChange(target,change,now);
        Map<Integer,TUserPermission> beforeByPermission=new LinkedHashMap<>();boolean changed=false;
        for(PermissionChange change:changes){TUserPermission before=userPermissionMapper.selectCurrent(userId,change.getPermissionId());
            beforeByPermission.put(change.getPermissionId(),before);
            if(change.getState()!=PersonalState.INHERIT||isCurrentOrFuture(before,now))changed=true;}
        if(!changed){requireExpectedVersion(target,expectedVersion);return new AppliedTarget(target,false,false);}
        casTarget(target,expectedVersion);
        for(PermissionChange change:changes){
            TUserPermission before=beforeByPermission.get(change.getPermissionId());
            List<Integer> beforeOrganizationIds=before==null?List.of():userPermissionOrganizationMapper.selectOrganizationIds(before.getId());
            if(change.getState()==PersonalState.INHERIT){
                if(!isCurrentOrFuture(before,now))continue;
                if(userPermissionMapper.closeByIdAndVersion(before.getId(),before.getVersion(),now)!=1)
                    throw new BusinessException(CodeEnum.ROLE_VERSION_CONFLICT,"个人权限已被并发修改");
                TAuthorizationHistory history=permissionHistory(userId,change.getPermissionId(),AuthorizationChangeType.REVOKE,
                        before.getEffect(),before.getDataScopeCode(),before.getEffectiveFrom(),before.getEffectiveTo(),reason);
                history.setBeforeValue(permissionSnapshot(before,beforeOrganizationIds));history.setAfterValue(null);histories.add(history);continue;
            }
            PermissionEffect effect=change.getState()==PersonalState.GRANT?PermissionEffect.GRANT:PermissionEffect.DENY;
            ScopeSelection selection=effect==PermissionEffect.GRANT?parseScopeSelection(change):new ScopeSelection(null,List.of());
            TUserPermission value=new TUserPermission();value.setUserId(userId);value.setPermissionId(change.getPermissionId());
            value.setEffect(effect);value.setDataScopeCode(selection.scope());value.setEffectiveFrom(resolveEffectiveFrom(change,now));
            value.setEffectiveTo(change.getEffectiveTo()==null?null:local(change.getEffectiveTo()));value.setReason(reason);
            value.setGrantedBy(currentUserProvider.getCurrentUserId());value.setUpdateTime(now);
            if(before==null){value.setVersion(0);value.setCreateTime(now);if(userPermissionMapper.insert(value)!=1)
                throw new BusinessException(CodeEnum.ROLE_VERSION_CONFLICT,"个人权限已被并发创建");}
            else{userPermissionOrganizationMapper.deleteByUserPermissionId(before.getId());value.setId(before.getId());
                if(userPermissionMapper.updateCurrentByVersion(value,before.getVersion())!=1)
                    throw new BusinessException(CodeEnum.ROLE_VERSION_CONFLICT,"个人权限已被并发修改");}
            replaceUserPermissionOrganizations(value.getId(),selection.organizationIds());
            TAuthorizationHistory history=permissionHistory(userId,change.getPermissionId(),
                    effect==PermissionEffect.GRANT?AuthorizationChangeType.GRANT:AuthorizationChangeType.DENY,
                    effect,selection.scope(),value.getEffectiveFrom(),value.getEffectiveTo(),reason);
            history.setBeforeValue(permissionSnapshot(before,beforeOrganizationIds));
            history.setAfterValue(permissionSnapshot(value,selection.organizationIds()));histories.add(history);
        }
        target.setAuthorizationVersion(expectedVersion+1);return new AppliedTarget(target,true,false);
    }

    private void requireAuthorizationEligible(Integer userId){
        TEmployee employee=employeeMapper.selectByUserId(userId);
        if(employee!=null&&employee.getEmploymentStatus()==EmployeeStatus.LEFT)
            throw new BusinessException(CodeEnum.USER_LIFECYCLE_CONFLICT,"已离职员工不能接受角色或个人权限");
    }

    private BatchResult finishBatch(List<AppliedTarget> applied,List<TAuthorizationHistory> histories,
                                    AuditActionEnum action,String command){
        List<Integer> changedUserIds=applied.stream().filter(AppliedTarget::changed).map(value->value.target().getId()).toList();
        if(!histories.isEmpty())auditRecorder.recordAll(histories,action,"batch:"+command,
                batchSummary(command,applied.stream().map(value->value.target().getId()).toList(),histories.size()));
        changedUserIds.forEach(this::scheduleCleanup);
        BatchResult result=new BatchResult();result.setTargetCount(applied.size());result.setChangedTargetCount(changedUserIds.size());
        for(AppliedTarget value:applied){BatchTargetResult item=new BatchTargetResult();item.setUserId(value.target().getId());
            item.setAuthorizationVersion(value.target().getAuthorizationVersion());item.setChanged(value.changed());result.getTargets().add(item);}
        return result;
    }

    private List<BatchTarget> uniqueTargets(List<BatchTarget> targets){Set<Integer>ids=new HashSet<>();
        for(BatchTarget target:targets){if(target==null||target.getUserId()==null||target.getAuthorizationVersion()==null)
            throw new BusinessException(CodeEnum.PARAM_ERROR,"批量授权目标和版本不能为空");
            if(!ids.add(target.getUserId()))throw new BusinessException(CodeEnum.PARAM_ERROR,"批量授权目标不能重复");}
        return targets;}
    private LinkedHashSet<Integer> uniqueIds(List<Integer>ids,String message){LinkedHashSet<Integer>unique=new LinkedHashSet<>(ids);
        if(unique.size()!=ids.size()||unique.contains(null))throw new BusinessException(CodeEnum.PARAM_ERROR,message);return unique;}
    private void validateUniquePermissionChanges(List<PermissionChange>changes){Set<Integer>ids=new HashSet<>();
        for(PermissionChange change:changes){if(change==null||change.getPermissionId()==null||change.getState()==null)
            throw new BusinessException(CodeEnum.PARAM_ERROR,"个人权限变更项不能为空");
            if(!ids.add(change.getPermissionId()))throw new BusinessException(CodeEnum.PARAM_ERROR,"个人权限变更项不能重复");}}
    private String batchSummary(String command,List<Integer>targetIds,int historyCount){String batchId=UUID.randomUUID().toString();
        return json(Map.of("batchId",batchId,"command",command,"targetIds",targetIds,"totalCount",targetIds.size(),
                "successCount",targetIds.size(),"failureCount",0,"targetResultCode","SUCCESS","targetResultName","成功","historyCount",historyCount));}
    private void requireAvailableAdmin(){if(userMapper.countAdminUsers()<1)
        throw new BusinessException(CodeEnum.LAST_AVAILABLE_ADMIN_REQUIRED,"不能移除最后一个可用普通管理员");}
    private record AppliedTarget(TUser target,boolean changed,boolean removedAdmin){}

    private void validatePermissionChange(TUser target, PermissionChange change, LocalDateTime now) {
        TPermission permission = permissionMapper.selectByPrimaryKey(change.getPermissionId());
        if (permission == null || permission.getEnabled() == null || permission.getEnabled()!=1) throw new BusinessException(CodeEnum.NOT_FOUND, "权限不存在或已停用");
        if (change.getState()==PersonalState.INHERIT) {
            if (change.getDataScopeCandidateKey()!=null
                    || (change.getCustomOrganizationUnitIds()!=null && !change.getCustomOrganizationUnitIds().isEmpty())
                    || change.getEffectiveFrom()!=null || change.getEffectiveTo()!=null) {
                throw new BusinessException(CodeEnum.PARAM_ERROR, "取消个人权限不得携带范围或有效期");
            }
            return;
        }
        LocalDateTime effectiveFrom=resolveEffectiveFrom(change,now);
        if (change.getEffectiveTo()!=null && !local(change.getEffectiveTo()).isAfter(effectiveFrom))
            throw new BusinessException(CodeEnum.PARAM_ERROR, "个人授权失效时间必须晚于生效时间");
        if (change.getState()==PersonalState.GRANT) {
            ScopeSelection selection = parseScopeSelection(change);
            DataScopeCode scope = selection.scope();
            validateCustomOrganizations(permission, target, scope, selection.organizationIds());
            if (!policy.canDelegatePermission(permission, scope, selection.organizationIds(), target)) throw new BusinessException(CodeEnum.ACCESS_DENIED, "权限或数据范围超过操作者委派上限");
        } else if (change.getState()==PersonalState.DENY && (change.getDataScopeCandidateKey()!=null
                || (change.getCustomOrganizationUnitIds()!=null && !change.getCustomOrganizationUnitIds().isEmpty()))) {
            throw new BusinessException(CodeEnum.PARAM_ERROR, "个人拒绝不得携带数据范围");
        } else if (change.getState()==PersonalState.DENY && !policy.isGlobalOperator()
                && !policy.canDelegatePermission(permission, DataScopeCode.SELF, target)) {
            throw new BusinessException(CodeEnum.ACCESS_DENIED, "不能操作未持有或不可委派的权限");
        }
    }

    private Detail build(TUser target, LocalDateTime now) {
        Detail detail = new Detail(); detail.setUser(targetUser(target, now)); detail.setAuthorizationVersion(target.getAuthorizationVersion());
        boolean manageable = policy.canManageAuthorization(target);
        boolean roleManageable = policy.canManageRoleAssignments(target);
        if (roleManageable) detail.getAllowedActions().add("ROLE_UPDATE");
        else detail.getUnavailableReasons().put("ROLE_UPDATE", "不能调整本人、受保护账号或角色管理级别之外的用户");
        if (manageable) detail.getAllowedActions().add("PERMISSION_UPDATE");
        else detail.getUnavailableReasons().put("PERMISSION_UPDATE", "不能调整本人、受保护账号或管理范围外用户");
        List<TUserRole> assignments = userRoleMapper.selectEffectiveByUserId(target.getId(), now);
        Set<Integer> roleIds = new LinkedHashSet<>();
        for (TUserRole assignment : assignments) {
            TRole role = roleMapper.selectByPrimaryKey(assignment.getRoleId()); if (role==null || role.getEnabled()==null || role.getEnabled()!=1 || !policy.roleAppliesToTarget(role,target)) continue;
            roleIds.add(role.getId()); RoleAssignment item = new RoleAssignment(); item.setRoleId(role.getId()); item.setRoleCode(role.getRole()); item.setRoleName(role.getRoleName());
            item.setSource(Boolean.TRUE.equals(role.getProtectedRole()) ? "PROTECTED" : "DIRECT"); item.setSourceDescription(assignment.getReason());
            item.setEffectiveFrom(offset(assignment.getEffectiveFrom())); item.setEffectiveTo(offset(assignment.getEffectiveTo())); detail.getRoleAssignments().add(item);
        }
        for (TRole role : policy.roleCandidates(target)) {
            RoleCandidate candidate = new RoleCandidate(); candidate.setRoleId(role.getId()); candidate.setRoleCode(role.getRole()); candidate.setRoleName(role.getRoleName());
            candidate.setAuthorizationLevel(role.getAuthorizationLevel()); candidate.setDefaultDataScope(role.getDefaultDataScope()); candidate.setSelected(roleIds.contains(role.getId()));
            candidate.setEditable(roleManageable && (candidate.isSelected()
                    ? policy.canRevokeRole(role,target) : policy.canDelegateRole(role,target)));
            if(!candidate.isEditable()) candidate.setUnavailableReason("受保护、停用、级别/权限/范围超过委派上限或不适用于目标组织");
            detail.getRoleCandidates().add(candidate);
        }
        Map<Integer,List<PermissionSource>> sources = roleSources(assignments, target, now);
        Map<Integer,TUserPermission> personal = new HashMap<>();
        for(TUserPermission item:userPermissionMapper.selectCurrentAndFutureByUserId(target.getId(),now)) personal.put(item.getPermissionId(),item);
        Map<Integer,TUserPermission> effectivePersonal = new HashMap<>();
        for(TUserPermission item:userPermissionMapper.selectEffectiveByUserId(target.getId(),now)) effectivePersonal.put(item.getPermissionId(),item);
        for (TPermission permission : permissionMapper.selectAll()) {
            if(permission.getCode()==null || permission.getEnabled()==null || permission.getEnabled()!=1) continue;
            PermissionItem item = new PermissionItem(); item.setPermissionId(permission.getId()); item.setCode(permission.getCode()); item.setName(permission.getName());
            item.setModule(permission.getModule()); item.setDescription(permission.getDescription()); item.setSensitivityLevel(permission.getSensitivityLevel()); item.setDelegable(Boolean.TRUE.equals(permission.getDelegable()));
            List<PermissionSource> rolePermissionSources=sources.getOrDefault(permission.getId(),List.of());
            item.getSources().addAll(rolePermissionSources); TUserPermission personalValue=personal.get(permission.getId());
            TUserPermission effectivePersonalValue=effectivePersonal.get(permission.getId());
            if(personalValue==null) item.setPersonalState(PersonalState.INHERIT); else { List<Integer> personalOrganizations=userPermissionOrganizationMapper.selectOrganizationIds(personalValue.getId());item.setPersonalState(personalValue.getEffect()==PermissionEffect.GRANT?PersonalState.GRANT:PersonalState.DENY); item.setPersonalDataScopeCandidateKey(scopeCandidateKey(personalValue.getDataScopeCode(),personalOrganizations));item.setPersonalOrganizationIds(personalOrganizations); item.setPersonalEffectiveFrom(offset(personalValue.getEffectiveFrom())); item.setPersonalEffectiveTo(offset(personalValue.getEffectiveTo())); item.getSources().add(personalSource(personalValue,personalOrganizations,effectivePersonalValue!=null)); }
            boolean roleEffective=!rolePermissionSources.isEmpty();
            item.setEffective(effectivePersonalValue==null?roleEffective
                    :effectivePersonalValue.getEffect()!=PermissionEffect.DENY
                    &&(roleEffective||effectivePersonalValue.getEffect()==PermissionEffect.GRANT));
            List<DataScopeCode> scopes=policy.delegableScopes(permission,target); for(DataScopeCode scope:scopes){if(scope==DataScopeCode.CUSTOM_ORGS){for(Integer organizationId:policy.delegableCustomOrganizationIds(permission,target)){TOrganizationUnit organization=organizationMapper.selectByPrimaryKey(organizationId);if(organization!=null)item.getDataScopeCandidates().add(scopeCandidate(scope,List.of(organization.getId())));}}else item.getDataScopeCandidates().add(scopeCandidate(scope,List.of()));}
            item.setEditable(manageable && (!scopes.isEmpty() || (personalValue!=null && personalValue.getEffect()==PermissionEffect.DENY)));
            if(!item.isEditable())item.setUnavailableReason("权限敏感级别、可委派标记或数据范围超过操作者上限"); detail.getPermissions().add(item);
        }
        return detail;
    }

    private Map<Integer,List<PermissionSource>> roleSources(List<TUserRole> assignments, TUser target, LocalDateTime now){
        Map<Integer,List<PermissionSource>> result=new HashMap<>(); for(TUserRole assignment:assignments){TRole role=roleMapper.selectByPrimaryKey(assignment.getRoleId());if(role==null||role.getEnabled()==null||role.getEnabled()!=1)continue;
            if(!policy.roleAppliesToTarget(role,target))continue;
            for(TRolePermission rp:rolePermissionMapper.selectByRoleId(role.getId())){PermissionSource source=new PermissionSource();source.setType(SourceType.ROLE);source.setSourceId(role.getId());source.setSourceName(role.getRoleName());source.setDataScopeLabel(rp.getDataScopeCode().name());if(rp.getDataScopeCode()==DataScopeCode.CUSTOM_ORGS){List<Integer>orgIds=rolePermissionOrganizationMapper.selectOrganizationIds(role.getId(),rp.getPermissionId());source.setOrganizationIds(orgIds);source.setOrganizationNames(organizationNames(orgIds));}source.setEffectiveFrom(offset(assignment.getEffectiveFrom()));source.setEffectiveTo(offset(assignment.getEffectiveTo()));source.setActive(true);result.computeIfAbsent(rp.getPermissionId(),ignored->new ArrayList<>()).add(source);}}return result;}
    private PermissionSource personalSource(TUserPermission value,List<Integer>organizationIds,boolean active){PermissionSource source=new PermissionSource();source.setType(value.getEffect()==PermissionEffect.GRANT?SourceType.PERSONAL_GRANT:SourceType.PERSONAL_DENY);source.setSourceId(value.getId()==null?null:value.getId().intValue());source.setSourceName(value.getEffect()==PermissionEffect.GRANT?"个人增加":"个人拒绝");source.setDataScopeLabel(value.getDataScopeCode()==null?null:value.getDataScopeCode().name());source.setOrganizationIds(organizationIds);source.setOrganizationNames(organizationNames(organizationIds));source.setEffectiveFrom(offset(value.getEffectiveFrom()));source.setEffectiveTo(offset(value.getEffectiveTo()));source.setActive(active);return source;}
    private ScopeCandidate scopeCandidate(DataScopeCode scope,List<Integer>organizationIds){ScopeCandidate candidate=new ScopeCandidate();candidate.setCandidateKey(scopeCandidateKey(scope,organizationIds));candidate.setCode(scope);candidate.setLabel(scope.name());candidate.setOrganizationIds(organizationIds);candidate.setOrganizationNames(organizationNames(organizationIds));return candidate;}
    private TargetUser targetUser(TUser user,LocalDateTime now){TargetUser value=new TargetUser();value.setId(user.getId());value.setLoginAct(user.getLoginAct());value.setName(user.getName());value.setAccountEnabled(user.getAccountEnabled()!=null&&user.getAccountEnabled()==1);value.setProtectedAccount(Boolean.TRUE.equals(user.getProtectedAccount()));TEmployee employee=employeeMapper.selectByUserId(user.getId());if(employee!=null){value.setEmployeeNo(employee.getEmployeeNo());TEmployeeAssignment primary=assignmentMapper.selectCurrentPrimaryByEmployeeId(employee.getId(),now);if(primary!=null){TOrganizationUnit org=organizationMapper.selectByPrimaryKey(primary.getOrganizationUnitId());TPosition position=positionMapper.selectByPrimaryKey(primary.getPositionId());value.setOrganizationName(org==null?null:org.getName());value.setPositionName(position==null?null:position.getName());}}return value;}
    private void casTarget(TUser target,Integer expected){if(!Objects.equals(target.getAuthorizationVersion(),expected)||userMapper.incrementAuthorizationVersionsByExpected(target.getId(),expected)!=1)throw new BusinessException(CodeEnum.ROLE_VERSION_CONFLICT,"用户授权版本冲突");}
    private void requireExpectedVersion(TUser target,Integer expected){if(!Objects.equals(target.getAuthorizationVersion(),expected))throw new BusinessException(CodeEnum.ROLE_VERSION_CONFLICT,"用户授权版本冲突");}
    private TUser requireUser(Integer id){TUser user=userMapper.selectByPrimaryKey(id);if(user==null)throw new BusinessException(CodeEnum.NOT_FOUND,"用户不存在");return user;}
    private ScopeSelection parseScopeSelection(PermissionChange change){String key=change.getDataScopeCandidateKey();if(key==null||key.isBlank())throw new BusinessException(CodeEnum.PARAM_ERROR,"个人增加必须选择数据范围");try{DataScopeCode scope=DataScopeCode.valueOf(key);List<Integer>raw=change.getCustomOrganizationUnitIds()==null?List.of():change.getCustomOrganizationUnitIds();List<Integer>ids=raw.stream().filter(Objects::nonNull).distinct().sorted().toList();if(ids.size()!=raw.size())throw new BusinessException(CodeEnum.PARAM_ERROR,"指定组织不能包含空值或重复值");if(scope==DataScopeCode.CUSTOM_ORGS){if(ids.isEmpty())throw new BusinessException(CodeEnum.PARAM_ERROR,"指定组织范围必须选择组织");return new ScopeSelection(scope,ids);}if(!ids.isEmpty())throw new BusinessException(CodeEnum.PARAM_ERROR,"非指定组织范围不得携带组织");return new ScopeSelection(scope,List.of());}catch(IllegalArgumentException ex){throw new BusinessException(CodeEnum.PARAM_ERROR,"未知数据范围");}}
    private LocalDateTime resolveEffectiveFrom(PermissionChange change,LocalDateTime now){if(change.getEffectiveFrom()==null)return now;LocalDateTime effectiveFrom=local(change.getEffectiveFrom());if(effectiveFrom.isBefore(now))throw new BusinessException(CodeEnum.PARAM_ERROR,"个人授权生效时间不能早于当前时间");if(effectiveFrom.isAfter(now.plusYears(MAX_PERMISSION_SCHEDULE_YEARS)))throw new BusinessException(CodeEnum.PARAM_ERROR,"个人授权预约生效时间不能超过一年");return effectiveFrom;}
    private boolean isCurrentOrFuture(TUserPermission value,LocalDateTime now){return value!=null&&Boolean.TRUE.equals(value.getActiveMarker())&&(value.getEffectiveTo()==null||value.getEffectiveTo().isAfter(now));}
    private void validateCustomOrganizations(TPermission permission,TUser target,DataScopeCode scope,List<Integer>organizationIds){if(scope!=DataScopeCode.CUSTOM_ORGS){if(!organizationIds.isEmpty())throw new BusinessException(CodeEnum.PARAM_ERROR,"非指定组织范围不得携带组织");return;}if(organizationIds.isEmpty())throw new BusinessException(CodeEnum.PARAM_ERROR,"指定组织范围不能为空");for(Integer id:organizationIds){TOrganizationUnit organization=organizationMapper.selectByPrimaryKey(id);if(organization==null||!Boolean.TRUE.equals(organization.getEnabled())||Boolean.TRUE.equals(organization.getPlaceholder()))throw new BusinessException(CodeEnum.PARAM_ERROR,"指定组织不存在或不可用");}Set<Integer>delegable=new HashSet<>(policy.delegableCustomOrganizationIds(permission,target));if(!delegable.containsAll(organizationIds))throw new BusinessException(CodeEnum.ACCESS_DENIED,"指定组织超过操作者可委派范围或不适用于目标用户");}
    private void replaceUserPermissionOrganizations(Long permissionId,List<Integer>organizationIds){for(Integer organizationId:organizationIds){TUserPermissionOrganization value=new TUserPermissionOrganization();value.setUserPermissionId(permissionId);value.setOrganizationUnitId(organizationId);if(userPermissionOrganizationMapper.insert(value)!=1)throw new BusinessException(CodeEnum.SYSTEM_ERROR,"个人权限指定组织写入失败");}}
    private String permissionSnapshot(TUserPermission value,List<Integer>organizationIds){if(value==null)return null;TPermission permission=permissionMapper.selectByPrimaryKey(value.getPermissionId());Map<String,Object>snapshot=new LinkedHashMap<>();snapshot.put("permissionId",value.getPermissionId());snapshot.put("permissionCode",permission==null?null:permission.getCode());snapshot.put("permissionName",permission==null?null:permission.getName());snapshot.put("effect",value.getEffect()==null?null:value.getEffect().name());snapshot.put("dataScopeCode",value.getDataScopeCode()==null?null:value.getDataScopeCode().name());List<Map<String,Object>>organizations=new ArrayList<>();for(Integer id:organizationIds){TOrganizationUnit organization=organizationMapper.selectByPrimaryKey(id);Map<String,Object>item=new LinkedHashMap<>();item.put("id",id);item.put("code",organization==null?null:organization.getCode());item.put("name",organization==null?null:organization.getName());organizations.add(item);}snapshot.put("organizations",organizations);snapshot.put("effectiveFrom",value.getEffectiveFrom());snapshot.put("effectiveTo",value.getEffectiveTo());return json(snapshot);}
    private String scopeCandidateKey(DataScopeCode scope,List<Integer>organizationIds){return scope==null?null:scope.name();}
    private List<String>organizationNames(List<Integer>ids){return ids.stream().map(organizationMapper::selectByPrimaryKey).filter(Objects::nonNull).map(TOrganizationUnit::getName).toList();}
    private record ScopeSelection(DataScopeCode scope,List<Integer>organizationIds){}
    private String json(Object value){try{return HISTORY_OBJECT_MAPPER.writeValueAsString(value);}catch(JsonProcessingException exception){throw new IllegalStateException("授权历史快照序列化失败",exception);}}
    private Set<Integer> union(Set<Integer>a,Set<Integer>b){Set<Integer>r=new LinkedHashSet<>(a);r.addAll(b);return r;}
    private boolean isProtectedAdministratorRoleId(Integer roleId){TRole role=roleMapper.selectByPrimaryKey(roleId);
        return role!=null&&Boolean.TRUE.equals(role.getProtectedRole())&&"admin".equals(role.getRole());}
    private TAuthorizationHistory roleHistory(Integer userId,Integer roleId,AuthorizationChangeType type,String reason,LocalDateTime now){TRole role=roleMapper.selectByPrimaryKey(roleId);Map<String,Object>snapshot=new LinkedHashMap<>();snapshot.put("roleId",roleId);snapshot.put("roleCode",role==null?null:role.getRole());snapshot.put("roleName",role==null?null:role.getRoleName());String stable=json(snapshot);TAuthorizationHistory h=new TAuthorizationHistory();h.setSubjectType(AuthorizationSubjectType.USER_ROLE);h.setSubjectId(userId+":"+roleId);h.setTargetUserId(userId);h.setRoleId(roleId);h.setChangeType(type);if(type==AuthorizationChangeType.UNASSIGN)h.setBeforeValue(stable);else h.setAfterValue(stable);h.setReason(reason);h.setEffectiveFrom(now);return h;}
    private TAuthorizationHistory permissionHistory(Integer userId,Integer permissionId,AuthorizationChangeType type,PermissionEffect effect,DataScopeCode scope,LocalDateTime from,LocalDateTime to,String reason){TAuthorizationHistory h=new TAuthorizationHistory();h.setSubjectType(AuthorizationSubjectType.USER_PERMISSION);h.setSubjectId(userId+":"+permissionId);h.setTargetUserId(userId);h.setPermissionId(permissionId);h.setChangeType(type);h.setEffect(effect);h.setDataScopeCode(scope);h.setEffectiveFrom(from);h.setEffectiveTo(to);h.setReason(reason);return h;}
    private void scheduleCleanup(Integer userId){securityMutations.accessChanged(userId,"授权变化");}
    private void lockMembership(){if(!"AUTHORIZATION_MEMBERSHIP_GUARD".equals(graphLock.lockByName("AUTHORIZATION_MEMBERSHIP_GUARD")))throw new IllegalStateException("授权成员图锁缺失");}
    private void lockGraph(String name){if(!name.equals(graphLock.lockByName(name)))throw new IllegalStateException("授权范围图锁缺失: "+name);}
    private LocalDateTime local(OffsetDateTime value){return LocalDateTime.ofInstant(value.toInstant(),ZONE);}
    private OffsetDateTime offset(LocalDateTime value){return value==null?null:value.atZone(ZONE).toOffsetDateTime();}
}
