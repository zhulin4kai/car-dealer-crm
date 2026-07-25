package com.autodealer.crm.modules.identity.application.internal;

import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.autodealer.crm.modules.audit.application.api.AuditActionEnum;
import com.autodealer.crm.modules.identity.application.api.AuthorizationAuditRecorder;
import com.autodealer.crm.modules.identity.application.api.RoleAccessService;
import com.autodealer.crm.modules.identity.application.api.dto.access.RoleDtos.*;
import com.autodealer.crm.modules.identity.application.api.dto.access.RoleDtos.ChangeRoleStatusRequest;
import com.autodealer.crm.modules.identity.application.api.dto.access.RoleDtos.CopyRoleRequest;
import com.autodealer.crm.modules.identity.application.api.dto.access.RoleDtos.CreateRoleRequest;
import com.autodealer.crm.modules.identity.application.api.dto.access.RoleDtos.DifferenceItem;
import com.autodealer.crm.modules.identity.application.api.dto.access.RoleDtos.MatrixRequest;
import com.autodealer.crm.modules.identity.application.api.dto.access.RoleDtos.MatrixResponse;
import com.autodealer.crm.modules.identity.application.api.dto.access.RoleDtos.OrganizationOption;
import com.autodealer.crm.modules.identity.application.api.dto.access.RoleDtos.PermissionDataScopeCandidate;
import com.autodealer.crm.modules.identity.application.api.dto.access.RoleDtos.PermissionItem;
import com.autodealer.crm.modules.identity.application.api.dto.access.RoleDtos.PermissionScopeAssignment;
import com.autodealer.crm.modules.identity.application.api.dto.access.RoleDtos.PermissionScopeDifference;
import com.autodealer.crm.modules.identity.application.api.dto.access.RoleDtos.PermissionScopeOption;
import com.autodealer.crm.modules.identity.application.api.dto.access.RoleDtos.PreviewResponse;
import com.autodealer.crm.modules.identity.application.api.dto.access.RoleDtos.RoleResponse;
import com.autodealer.crm.modules.identity.application.api.dto.access.RoleDtos.UpdateMatrixRequest;
import com.autodealer.crm.modules.identity.application.api.dto.access.RoleDtos.UpdateMatrixResponse;
import com.autodealer.crm.modules.identity.application.api.dto.access.RoleDtos.UpdateRoleRequest;
import com.autodealer.crm.modules.identity.application.api.enums.AuthorizationChangeType;
import com.autodealer.crm.modules.identity.application.api.enums.AuthorizationSubjectType;
import com.autodealer.crm.modules.identity.application.api.enums.DataScopeCode;
import com.autodealer.crm.modules.identity.application.api.enums.PermissionSensitivityLevel;
import com.autodealer.crm.modules.identity.application.api.enums.RoleScopeType;
import com.autodealer.crm.modules.identity.application.api.model.TUser;
import com.autodealer.crm.modules.identity.application.api.security.CurrentUserProvider;
import com.autodealer.crm.modules.identity.persistence.mapper.TAuthorizationGraphLockMapper;
import com.autodealer.crm.modules.identity.persistence.mapper.TEmployeeAssignmentMapper;
import com.autodealer.crm.modules.identity.persistence.mapper.TEmployeeMapper;
import com.autodealer.crm.modules.identity.persistence.mapper.TEmployeeReportingMapper;
import com.autodealer.crm.modules.identity.persistence.mapper.TOrganizationUnitMapper;
import com.autodealer.crm.modules.identity.persistence.mapper.TPermissionMapper;
import com.autodealer.crm.modules.identity.persistence.mapper.TRoleMapper;
import com.autodealer.crm.modules.identity.persistence.mapper.TRoleOrganizationMapper;
import com.autodealer.crm.modules.identity.persistence.mapper.TRolePermissionMapper;
import com.autodealer.crm.modules.identity.persistence.mapper.TRolePermissionOrganizationMapper;
import com.autodealer.crm.modules.identity.persistence.mapper.TUserMapper;
import com.autodealer.crm.modules.identity.persistence.mapper.TUserRoleMapper;
import com.autodealer.crm.modules.identity.persistence.model.TAuthorizationHistory;
import com.autodealer.crm.modules.identity.persistence.model.TEmployee;
import com.autodealer.crm.modules.identity.persistence.model.TEmployeeAssignment;
import com.autodealer.crm.modules.identity.persistence.model.TOrganizationUnit;
import com.autodealer.crm.modules.identity.persistence.model.TPermission;
import com.autodealer.crm.modules.identity.persistence.model.TRole;
import com.autodealer.crm.modules.identity.persistence.model.TRoleOrganization;
import com.autodealer.crm.modules.identity.persistence.model.TRolePermission;
import com.autodealer.crm.modules.identity.persistence.model.TRolePermissionOrganization;
import com.autodealer.crm.modules.identity.persistence.model.TUserRole;
import com.autodealer.crm.shared.error.BusinessException;
import com.autodealer.crm.shared.error.CodeEnum;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
public class RoleAccessServiceImpl implements RoleAccessService {
    private final TRoleMapper roleMapper; private final TPermissionMapper permissionMapper;
    private final TRolePermissionMapper rolePermissionMapper; private final TRoleOrganizationMapper roleOrganizationMapper;
    private final TRolePermissionOrganizationMapper rolePermissionOrganizationMapper;
    private final TOrganizationUnitMapper organizationMapper; private final TUserRoleMapper userRoleMapper;
    private final TUserMapper userMapper; private final TEmployeeMapper employeeMapper;
    private final TEmployeeAssignmentMapper assignmentMapper; private final TEmployeeReportingMapper reportingMapper;
    private final AuthorizationAuditRecorder auditRecorder; private final CurrentUserProvider currentUser;
    private final ObjectMapper objectMapper;
    private final TAuthorizationGraphLockMapper graphLock;
    private final UserAuthorizationPolicy authorizationPolicy;
    private final UserSecurityMutationCoordinator securityMutations;

    public RoleAccessServiceImpl(TRoleMapper roleMapper, TPermissionMapper permissionMapper,
      TRolePermissionMapper rolePermissionMapper, TRoleOrganizationMapper roleOrganizationMapper,
      TRolePermissionOrganizationMapper rolePermissionOrganizationMapper,
      TOrganizationUnitMapper organizationMapper, TUserRoleMapper userRoleMapper, TUserMapper userMapper,
      TEmployeeMapper employeeMapper, TEmployeeAssignmentMapper assignmentMapper,
      TEmployeeReportingMapper reportingMapper, AuthorizationAuditRecorder auditRecorder,
      CurrentUserProvider currentUser, ObjectMapper objectMapper,
      TAuthorizationGraphLockMapper graphLock, UserAuthorizationPolicy authorizationPolicy,
      UserSecurityMutationCoordinator securityMutations) {
        this.roleMapper=roleMapper; this.permissionMapper=permissionMapper; this.rolePermissionMapper=rolePermissionMapper;
        this.rolePermissionOrganizationMapper=rolePermissionOrganizationMapper;
        this.roleOrganizationMapper=roleOrganizationMapper; this.organizationMapper=organizationMapper;
        this.userRoleMapper=userRoleMapper; this.userMapper=userMapper; this.employeeMapper=employeeMapper;
        this.assignmentMapper=assignmentMapper; this.reportingMapper=reportingMapper; this.auditRecorder=auditRecorder;
        this.currentUser=currentUser; this.objectMapper=objectMapper; this.graphLock=graphLock;
        this.authorizationPolicy=authorizationPolicy; this.securityMutations=securityMutations;
    }

    @Override public PageInfo<RoleResponse> page(int page,int size,String keyword,Boolean enabled) {
        List<TRole> rows;
        if(isSecurityAdmin()) {
            PageHelper.startPage(Math.max(1,page), Math.min(Math.max(1,size),200));
            rows=roleMapper.selectFiltered(keyword,enabled);
        } else {
            List<Integer> visible=visibleOrganizations().stream().map(TOrganizationUnit::getId).toList();
            if(visible.isEmpty()) return emptyPage(page,size);
            PageHelper.startPage(Math.max(1,page), Math.min(Math.max(1,size),200));
            rows=roleMapper.selectFilteredVisible(keyword,enabled,operatorLevel(),visible);
        }
        PageInfo<TRole> raw=new PageInfo<>(rows);
        List<RoleResponse> filtered=rows.stream().map(this::response).toList();
        PageInfo<RoleResponse> out=new PageInfo<>(); out.setList(filtered); out.setPageNum(raw.getPageNum()); out.setPageSize(raw.getPageSize()); out.setTotal(raw.getTotal()); out.setPages(raw.getPages()); return out;
    }
    @Override public RoleResponse detail(Integer id){ return response(requireReadableRole(id)); }

    @Override @Transactional public RoleResponse create(CreateRoleRequest q){
        lockGraph("ORGANIZATION_HIERARCHY");
        validateRoleRequest(q.getAuthorizationLevel(),q.getDefaultDataScope(),q.getScopeType(),q.getOrganizationUnitIds());
        if(roleMapper.selectByCode(q.getCode())!=null) throw new BusinessException(CodeEnum.DUPLICATE,"角色编码已存在");
        TRole r=new TRole(); r.setRole(q.getCode()); r.setRoleName(q.getName()); r.setDescription(q.getDescription());
        r.setProtectedRole(false); r.setAuthorizationLevel(q.getAuthorizationLevel()); r.setDefaultDataScope(q.getDefaultDataScope());
        r.setScopeType(q.getScopeType()); r.setEnabled(1); r.setVersion(0);
        if(roleMapper.insert(r)!=1) throw new BusinessException(CodeEnum.OPERATION_FAILED);
        replaceOrganizations(r.getId(),q.getOrganizationUnitIds()); recordRole(r,AuthorizationChangeType.CREATE,null,q.getDescription()); return response(r);
    }
    @Override @Transactional public RoleResponse update(Integer id,UpdateRoleRequest q){
        lockMembership();
        lockAuthorizationScope();
        TRole r=requireMutableRole(id); requireVersion(r,q.getExpectedVersion()); validateRoleRequest(q.getAuthorizationLevel(),q.getDefaultDataScope(),q.getScopeType(),q.getOrganizationUnitIds());
        validateAffectedUsers(r);
        String before=roleSnapshot(r); r.setRoleName(q.getName()); r.setDescription(q.getDescription()); r.setAuthorizationLevel(q.getAuthorizationLevel());
        r.setDefaultDataScope(q.getDefaultDataScope()); r.setScopeType(q.getScopeType());
        if(roleMapper.updateMutableByIdAndVersion(r,q.getExpectedVersion())!=1) throw conflict(); r.setVersion(q.getExpectedVersion()+1);
        replaceOrganizations(id,q.getOrganizationUnitIds()); validateExistingMatrixOrganizations(r);
        recordRole(r,AuthorizationChangeType.UPDATE,before,"编辑角色"); invalidateRoleMembers(id); return response(r);
    }
    @Override @Transactional public RoleResponse copy(Integer sourceId,CopyRoleRequest q){
        lockGraph("ORGANIZATION_HIERARCHY");
        requireMutableRole(sourceId); Map<Integer,ScopeValue> sourceScopes=currentScopes(sourceId);
        RoleResponse created=create(q); List<TRolePermission> sourcePermissions=rolePermissionMapper.selectByRoleId(sourceId);
        List<TPermission> copiedPermissions=sourcePermissions.stream().map(rp->permissionMapper.selectByPrimaryKey(rp.getPermissionId())).toList();
        copiedPermissions.forEach(this::validatePermissionAssignable); TRole createdTarget=requireRole(created.getId());
        validateChangedAssignments(copiedPermissions,createdTarget,sourceScopes); validateScopeOrganizations(createdTarget,sourceScopes);
        insertMatrix(createdTarget,sourceScopes,copiedPermissions);
        TRole createdRole=requireRole(created.getId()); createdRole.setVersion(1); roleMapper.incrementVersionByExpected(created.getId(),0);
        recordMatrix(createdRole,Map.of(),sourceScopes,q.getReason(),List.of()); return response(createdRole);
    }
    @Override @Transactional public RoleResponse status(Integer id,ChangeRoleStatusRequest q,boolean enabled){
        lockMembership();
        lockAuthorizationScope();
        TRole r=requireMutableRole(id); requireVersion(r,q.getExpectedVersion()); if((r.getEnabled()==1)==enabled)return response(r);
        validateAffectedUsers(r);
        if(!enabled&&!affectedUserIds(id).isEmpty())throw new BusinessException(CodeEnum.ROLE_IN_USE,"角色仍有当前或未来成员，不能停用");
        r.setEnabled(enabled?1:0); if(roleMapper.updateMutableByIdAndVersion(r,q.getExpectedVersion())!=1)throw conflict(); r.setVersion(q.getExpectedVersion()+1);
        recordRole(r,enabled?AuthorizationChangeType.ENABLE:AuthorizationChangeType.DISABLE,null,q.getReason()); invalidateRoleMembers(id); return response(r);
    }
    @Override public List<OrganizationOption> organizationOptions(){ return visibleOrganizations().stream().map(this::orgOption).toList(); }
    @Override public List<PermissionItem> permissionTree(){
        List<TPermission> all=permissionMapper.selectAll(); Map<Integer,PermissionItem> map=new LinkedHashMap<>(); all.forEach(p->map.put(p.getId(),permissionItem(p)));
        List<PermissionItem> roots=new ArrayList<>(); for(PermissionItem p:map.values()){PermissionItem parent=map.get(p.getParentId()); if(parent==null)roots.add(p);else parent.getChildren().add(p);} return roots;
    }
    @Override public MatrixResponse matrix(Integer roleId){TRole r=requireReadableRole(roleId); MatrixResponse m=new MatrixResponse();m.setRoleId(r.getId());m.setRoleName(r.getRoleName());m.setExpectedVersion(r.getVersion());List<TRolePermission>assignments=rolePermissionMapper.selectByRoleId(roleId);m.setSelectedPermissionIds(assignments.stream().map(TRolePermission::getPermissionId).toList());for(TRolePermission assignment:assignments){PermissionScopeAssignment scope=new PermissionScopeAssignment();scope.setPermissionId(assignment.getPermissionId());scope.setDataScopeCode(assignment.getDataScopeCode());scope.setOrganizationUnitIds(rolePermissionOrganizationMapper.selectOrganizationIds(roleId,assignment.getPermissionId()));m.getPermissionScopes().add(scope);}m.setEditable(isEditable(r));m.setDisabledReason(m.getEditable()?null:disabledReason(r));for(TPermission permission:permissionMapper.selectAll()){if(permission.getCode()==null)continue;m.getPermissionScopeOptions().add(permissionScopeOption(r,permission,Boolean.TRUE.equals(m.getEditable())));}return m;}
    @Override public PreviewResponse preview(Integer roleId,MatrixRequest q){TRole r=requireMutableRole(roleId);requireVersion(r,q.getExpectedVersion());return buildPreview(r,q);}
    @Override @Transactional public UpdateMatrixResponse updateMatrix(Integer roleId,UpdateMatrixRequest q){
        lockMembership();
        lockAuthorizationScope();
        TRole r=requireMutableRole(roleId);requireVersion(r,q.getExpectedVersion());buildPreview(r,q);
        Map<Integer,ScopeValue> before=currentScopes(roleId); List<TPermission> requested=validatePermissions(q.getPermissionIds());
        Map<Integer,ScopeValue> desired=requestedScopes(r,q,requested);
        if(before.equals(desired)){UpdateMatrixResponse same=new UpdateMatrixResponse();same.setRoleId(roleId);same.setVersion(r.getVersion());same.setPermissionIds(requested.stream().map(TPermission::getId).toList());same.setPermissionScopes(scopeAssignments(desired));same.setAffectedUserCount(0);same.setSecurityVersionUpdatedCount(0);same.setSessionCleanupWarningCount(null);return same;}
        validateAffectedUsers(r); if(roleMapper.incrementVersionByExpected(roleId,q.getExpectedVersion())!=1)throw conflict();
        rolePermissionOrganizationMapper.deleteByRoleId(roleId);rolePermissionMapper.deleteByRoleId(roleId);insertMatrix(r,desired,requested);
        r.setVersion(q.getExpectedVersion()+1); List<Integer> users=affectedUserIds(roleId);
        recordMatrix(r,before,desired,q.getReason(),users);
        int updated=users.isEmpty()?0:userMapper.incrementAuthVersionByIds(users);if(updated!=users.size())throw new IllegalStateException("受影响用户安全版本更新不完整");scheduleCleanup(users);
        UpdateMatrixResponse out=new UpdateMatrixResponse();out.setRoleId(roleId);out.setVersion(r.getVersion());out.setPermissionIds(requested.stream().map(TPermission::getId).toList());out.setPermissionScopes(scopeAssignments(desired));out.setAffectedUserCount(users.size());out.setSecurityVersionUpdatedCount(updated);out.setSessionCleanupWarningCount(null);return out;
    }

    private PreviewResponse buildPreview(TRole r,MatrixRequest q){List<TPermission> requested=validatePermissions(q.getPermissionIds());Map<Integer,ScopeValue> before=currentScopes(r.getId());Map<Integer,ScopeValue> desired=requestedScopes(r,q,requested);Set<Integer> old=before.keySet(),now=desired.keySet();List<TPermission> added=requested.stream().filter(x->!old.contains(x.getId())).toList();List<TPermission> changed=requested.stream().filter(x->old.contains(x.getId())&&!Objects.equals(before.get(x.getId()),desired.get(x.getId()))).toList();validateChangedAssignments(unionPermissions(added,changed),r,desired);PreviewResponse p=new PreviewResponse();p.setRoleId(r.getId());p.setExpectedVersion(r.getVersion());added.stream().map(this::difference).forEach(p.getAddedPermissions()::add);List<Integer> removedIds=old.stream().filter(x->!now.contains(x)).toList();if(!removedIds.isEmpty())permissionMapper.selectByIds(removedIds).stream().map(this::difference).forEach(p.getRemovedPermissions()::add);for(Integer permissionId:unionIds(old,now)){ScopeValue b=before.get(permissionId),a=desired.get(permissionId);if(!Objects.equals(b,a))p.getScopeDifferences().add(scopeDifference(permissionId,b,a));}int users=affectedUserIds(r.getId()).size();p.setAffectedUserCount(users);p.setAffectedOrganizationCount(r.getScopeType()==RoleScopeType.GLOBAL?organizationMapper.selectAll().size():roleOrganizationMapper.selectByRoleId(r.getId()).size());p.setSessionRevocationCount(users);if(p.getAddedPermissions().stream().anyMatch(x->x.getSensitivityLevel()!=PermissionSensitivityLevel.NORMAL)||p.getRemovedPermissions().stream().anyMatch(x->x.getSensitivityLevel()!=PermissionSensitivityLevel.NORMAL)||changed.stream().anyMatch(x->x.getSensitivityLevel()!=PermissionSensitivityLevel.NORMAL))p.getWarnings().add("包含敏感权限变更");return p;}
    private List<TPermission> validatePermissions(List<Integer> ids){List<Integer> input=ids==null?List.of():ids;List<Integer> unique=input.stream().distinct().toList();if(unique.size()!=input.size())throw new BusinessException(CodeEnum.PARAM_ERROR,"权限不能重复");if(unique.isEmpty())return List.of();List<TPermission> values=permissionMapper.selectByIds(unique);if(values.size()!=unique.size())throw new BusinessException(CodeEnum.ROLE_PERMISSION_INVALID,"包含未知权限");values.forEach(this::validatePermissionAssignable);return values;}
    private void validatePermissionAssignable(TPermission p){if(p==null||p.getEnabled()!=1)throw new BusinessException(CodeEnum.ROLE_PERMISSION_INVALID,"权限不存在或已停用");}
    private void validateChangedAssignments(List<TPermission> changed,TRole targetRole,Map<Integer,ScopeValue> desired){if(isSecurityAdmin())return;Map<Integer,Set<DataScopeCode>>owned=operatorPermissionScopes();for(TPermission p:changed){ScopeValue value=desired.get(p.getId());if(value==null||!Boolean.TRUE.equals(p.getDelegable())||p.getSensitivityLevel()!=PermissionSensitivityLevel.NORMAL||!UserAuthorizationPolicy.scopeMatrixCovers(owned.get(p.getId()),value.scope(),true))throw new BusinessException(CodeEnum.ROLE_PERMISSION_LIMIT,"不能授予操作者未拥有、不可委派、敏感或数据范围超限的权限");}}
    private Set<Integer> operatorPermissionIds(){Set<Integer> ids=new HashSet<>();permissionMapper.selectMenuPermissionByUserId(currentUser.getCurrentUserId()).forEach(p->ids.add(p.getId()));permissionMapper.selectButtonPermissionByUserId(currentUser.getCurrentUserId()).forEach(p->ids.add(p.getId()));return ids;}
    private Map<Integer,Set<DataScopeCode>> operatorPermissionScopes(){Integer userId=currentUser.getCurrentUserId();LocalDateTime now=LocalDateTime.now();Map<Integer,Set<DataScopeCode>>result=new HashMap<>();for(TUserRole assignment:userRoleMapper.selectEffectiveByUserId(userId,now)){TRole role=roleMapper.selectByPrimaryKey(assignment.getRoleId());if(role==null||role.getEnabled()==null||role.getEnabled()!=1||!roleAppliesToUser(role,userId,now))continue;for(TRolePermission item:rolePermissionMapper.selectByRoleId(role.getId())){TPermission permission=permissionMapper.selectByPrimaryKey(item.getPermissionId());if(permission!=null&&permission.getEnabled()==1&&Boolean.TRUE.equals(permission.getDelegable())&&permission.getSensitivityLevel()==PermissionSensitivityLevel.NORMAL&&Boolean.TRUE.equals(item.getDelegable()))result.computeIfAbsent(item.getPermissionId(),ignored->new HashSet<>()).add(item.getDataScopeCode());}}return result;}
    private boolean roleAppliesToUser(TRole role,Integer userId,LocalDateTime now){if(role.getScopeType()==RoleScopeType.GLOBAL)return true;TEmployee employee=employeeMapper.selectByUserId(userId);TEmployeeAssignment primary=employee==null?null:assignmentMapper.selectCurrentPrimaryByEmployeeId(employee.getId(),now);return primary!=null&&roleOrganizationMapper.selectByRoleId(role.getId()).stream().anyMatch(scope->isDescendant(primary.getOrganizationUnitId(),scope.getOrganizationUnitId()));}
    private void validateRoleRequest(int level,DataScopeCode dataScope,RoleScopeType scope,List<Integer> orgIds){if(!isSecurityAdmin()){if(scope==RoleScopeType.GLOBAL||dataScope==DataScopeCode.GLOBAL||dataScope==DataScopeCode.CUSTOM_ORGS)throw new BusinessException(CodeEnum.ACCESS_DENIED);if(level>=operatorLevel())throw new BusinessException(CodeEnum.ROLE_PERMISSION_LIMIT);}if(scope==RoleScopeType.ORGANIZATION){if(orgIds==null||orgIds.isEmpty())throw new BusinessException(CodeEnum.PARAM_ERROR,"组织级角色必须选择适用组织");Set<Integer> visible=visibleOrganizations().stream().map(TOrganizationUnit::getId).collect(Collectors.toSet());if(!visible.containsAll(orgIds))throw new BusinessException(CodeEnum.ACCESS_DENIED);}else if(orgIds!=null&&!orgIds.isEmpty())throw new BusinessException(CodeEnum.PARAM_ERROR,"全局角色不能携带适用组织");if(dataScope==DataScopeCode.CUSTOM_ORGS&&(orgIds==null||orgIds.isEmpty()))throw new BusinessException(CodeEnum.PARAM_ERROR,"指定组织数据范围必须明确组织");}
    private void validateAffectedUsers(TRole r){
        if(!isSecurityAdmin()&&r.getAuthorizationLevel()>=operatorLevel())throw new BusinessException(CodeEnum.ROLE_PERMISSION_LIMIT);
        for(Integer uid:affectedUserIds(r.getId())){
            TUser target=userMapper.selectByPrimaryKey(uid);
            if(Objects.equals(uid,currentUser.getCurrentUserId()))throw new BusinessException(CodeEnum.SELF_MANAGEMENT_FORBIDDEN,"不能通过共享角色矩阵间接调整自己的权限");
            if(target==null||!authorizationPolicy.canManageAuthorization(target))throw new BusinessException(CodeEnum.ACCESS_DENIED,"角色包含当前或未来同级、上级或范围外成员");
        }
    }
    private List<TOrganizationUnit> visibleOrganizations(){if(isSecurityAdmin())return organizationMapper.selectAll();TEmployee op=employeeMapper.selectByUserId(currentUser.getCurrentUserId());TEmployeeAssignment primary=op==null?null:assignmentMapper.selectCurrentPrimaryByEmployeeId(op.getId(),LocalDateTime.now());if(primary==null)return List.of();List<Integer> ids=organizationMapper.selectDescendantIds(primary.getOrganizationUnitId());return ids==null||ids.isEmpty()?List.of():organizationMapper.selectByIds(ids);}
    private boolean isDescendant(Integer child,Integer ancestor){Set<Integer>s=new HashSet<>();while(child!=null&&s.add(child)){if(child.equals(ancestor))return true;TOrganizationUnit u=organizationMapper.selectByPrimaryKey(child);child=u==null?null:u.getParentId();}return false;}
    private int operatorLevel(){Integer userId=currentUser.getCurrentUserId();LocalDateTime now=LocalDateTime.now();return userRoleMapper.selectEffectiveByUserId(userId,now).stream().map(value->roleMapper.selectByPrimaryKey(value.getRoleId())).filter(Objects::nonNull).filter(role->role.getEnabled()!=null&&role.getEnabled()==1&&roleAppliesToUser(role,userId,now)).map(TRole::getAuthorizationLevel).filter(Objects::nonNull).max(Integer::compareTo).orElse(0);}
    private boolean isSecurityAdmin(){return authorizationPolicy.isGlobalOperator();}
    private TRole requireRole(Integer id){TRole r=roleMapper.selectByPrimaryKey(id);if(r==null)throw new BusinessException(CodeEnum.NOT_FOUND,"角色不存在");return r;}
    private TRole requireReadableRole(Integer id){TRole r=requireRole(id);if(!isSecurityAdmin()&&!isEditable(r))throw new BusinessException(CodeEnum.ACCESS_DENIED,"角色超出对象级可见范围");return r;}
    private TRole requireMutableRole(Integer id){TRole r=requireRole(id);if(Boolean.TRUE.equals(r.getProtectedRole()))throw new BusinessException(CodeEnum.PROTECTED_ROLE_FORBIDDEN);if(r.getScopeType()==RoleScopeType.GLOBAL&&!isSecurityAdmin())throw new BusinessException(CodeEnum.ACCESS_DENIED);if(!isSecurityAdmin()){if(r.getAuthorizationLevel()>=operatorLevel())throw new BusinessException(CodeEnum.ROLE_PERMISSION_LIMIT);Set<Integer> visible=visibleOrganizations().stream().map(TOrganizationUnit::getId).collect(Collectors.toSet());Set<Integer> applicable=roleOrganizationMapper.selectByRoleId(id).stream().map(TRoleOrganization::getOrganizationUnitId).collect(Collectors.toSet());if(!visible.containsAll(applicable))throw new BusinessException(CodeEnum.ACCESS_DENIED,"角色适用组织超出操作者范围");}return r;}
    private void requireVersion(TRole r,Integer v){if(!Objects.equals(r.getVersion(),v))throw conflict();} private BusinessException conflict(){return new BusinessException(CodeEnum.ROLE_VERSION_CONFLICT);}
    private boolean isEditable(TRole r){if(Boolean.TRUE.equals(r.getProtectedRole()))return false;if(isSecurityAdmin())return true;if(r.getScopeType()!=RoleScopeType.ORGANIZATION||r.getAuthorizationLevel()>=operatorLevel())return false;Set<Integer> visible=visibleOrganizations().stream().map(TOrganizationUnit::getId).collect(Collectors.toSet());return visible.containsAll(roleOrganizationMapper.selectByRoleId(r.getId()).stream().map(TRoleOrganization::getOrganizationUnitId).toList());}
    private String disabledReason(TRole r){return Boolean.TRUE.equals(r.getProtectedRole())?"受保护恢复角色不可修改":"超出角色管理范围";}
    private void replaceOrganizations(Integer roleId,List<Integer> ids){roleOrganizationMapper.deleteByRoleId(roleId);if(ids!=null)for(Integer id:ids){TRoleOrganization x=new TRoleOrganization();x.setRoleId(roleId);x.setOrganizationUnitId(id);roleOrganizationMapper.insert(x);}}
    private Map<Integer,ScopeValue> requestedScopes(TRole role,MatrixRequest request,List<TPermission> permissions){List<PermissionScopeAssignment> values=request.getPermissionScopes()==null?List.of():request.getPermissionScopes();Map<Integer,ScopeValue> result=new LinkedHashMap<>();for(PermissionScopeAssignment value:values){if(value==null||value.getPermissionId()==null||value.getDataScopeCode()==null)throw new BusinessException(CodeEnum.PARAM_ERROR,"每项权限必须提供完整数据范围");List<Integer> organizations=value.getOrganizationUnitIds()==null?List.of():value.getOrganizationUnitIds();List<Integer> normalized=organizations.stream().filter(Objects::nonNull).distinct().sorted().toList();if(normalized.size()!=organizations.size())throw new BusinessException(CodeEnum.PARAM_ERROR,"指定组织不能包含空值或重复值");if(value.getDataScopeCode()==DataScopeCode.CUSTOM_ORGS&&normalized.isEmpty())throw new BusinessException(CodeEnum.PARAM_ERROR,"CUSTOM_ORGS 必须指定独立组织来源");if(value.getDataScopeCode()!=DataScopeCode.CUSTOM_ORGS&&!normalized.isEmpty())throw new BusinessException(CodeEnum.PARAM_ERROR,"非 CUSTOM_ORGS 不能携带组织来源");if(result.put(value.getPermissionId(),new ScopeValue(value.getDataScopeCode(),normalized))!=null)throw new BusinessException(CodeEnum.PARAM_ERROR,"同一权限只能提供一个数据范围");}Set<Integer> expected=permissions.stream().map(TPermission::getId).collect(Collectors.toCollection(LinkedHashSet::new));if(!result.keySet().equals(expected))throw new BusinessException(CodeEnum.PARAM_ERROR,"permissionScopes 必须与 permissionIds 一一对应");validateScopeOrganizations(role,result);return result;}
    private void validateScopeOrganizations(TRole role,Map<Integer,ScopeValue> scopes){Set<Integer> visible=visibleOrganizations().stream().map(TOrganizationUnit::getId).collect(Collectors.toSet());List<Integer> applicable=roleOrganizationMapper.selectByRoleId(role.getId()).stream().map(TRoleOrganization::getOrganizationUnitId).toList();for(ScopeValue value:scopes.values())for(Integer organizationId:value.organizationIds()){TOrganizationUnit organization=organizationMapper.selectByPrimaryKey(organizationId);if(organization==null||!Boolean.TRUE.equals(organization.getEnabled())||Boolean.TRUE.equals(organization.getPlaceholder()))throw new BusinessException(CodeEnum.PARAM_ERROR,"权限指定组织不存在或不可用");if(!isSecurityAdmin()&&!visible.contains(organizationId))throw new BusinessException(CodeEnum.ACCESS_DENIED,"权限指定组织超出操作者可见范围");if(role.getScopeType()==RoleScopeType.ORGANIZATION&&applicable.stream().noneMatch(root->isDescendant(organizationId,root)))throw new BusinessException(CodeEnum.PARAM_ERROR,"权限指定组织必须位于角色适用组织范围内");}}
    private void validateExistingMatrixOrganizations(TRole role){validateScopeOrganizations(role,currentScopes(role.getId()));}
    private Map<Integer,ScopeValue> currentScopes(Integer roleId){Map<Integer,ScopeValue> result=new LinkedHashMap<>();for(TRolePermission permission:rolePermissionMapper.selectByRoleId(roleId)){List<Integer> organizations=permission.getDataScopeCode()==DataScopeCode.CUSTOM_ORGS?rolePermissionOrganizationMapper.selectOrganizationIds(roleId,permission.getPermissionId()).stream().distinct().sorted().toList():List.of();result.put(permission.getPermissionId(),new ScopeValue(permission.getDataScopeCode(),organizations));}return result;}
    private void insertMatrix(TRole role,Map<Integer,ScopeValue> scopes,List<TPermission> permissions){Map<Integer,TPermission> catalog=permissions.stream().collect(Collectors.toMap(TPermission::getId,value->value));for(Map.Entry<Integer,ScopeValue> entry:scopes.entrySet()){TPermission permission=catalog.get(entry.getKey());if(permission==null)throw new BusinessException(CodeEnum.ROLE_PERMISSION_INVALID,"权限目录缺失");TRolePermission value=new TRolePermission();value.setRoleId(role.getId());value.setPermissionId(permission.getId());value.setDelegable(permission.getDelegable());value.setDataScopeCode(entry.getValue().scope());if(rolePermissionMapper.insert(value)!=1)throw new BusinessException(CodeEnum.OPERATION_FAILED,"角色权限写入失败");for(Integer organizationId:entry.getValue().organizationIds()){TRolePermissionOrganization organization=new TRolePermissionOrganization();organization.setRoleId(role.getId());organization.setPermissionId(permission.getId());organization.setOrganizationUnitId(organizationId);if(rolePermissionOrganizationMapper.insert(organization)!=1)throw new BusinessException(CodeEnum.OPERATION_FAILED,"角色权限指定组织写入失败");}}}
    private List<PermissionScopeAssignment> scopeAssignments(Map<Integer,ScopeValue> scopes){return scopes.entrySet().stream().sorted(Map.Entry.comparingByKey()).map(entry->{PermissionScopeAssignment value=new PermissionScopeAssignment();value.setPermissionId(entry.getKey());value.setDataScopeCode(entry.getValue().scope());value.setOrganizationUnitIds(entry.getValue().organizationIds());return value;}).toList();}
    private PermissionScopeDifference scopeDifference(Integer permissionId,ScopeValue before,ScopeValue after){PermissionScopeDifference value=new PermissionScopeDifference();TPermission permission=permissionMapper.selectByPrimaryKey(permissionId);value.setPermissionId(permissionId);value.setPermissionCode(permission==null?String.valueOf(permissionId):permission.getCode());value.setPermissionName(permission==null?String.valueOf(permissionId):permission.getName());value.setBeforeDataScopeCode(before==null?null:before.scope());value.setAfterDataScopeCode(after==null?null:after.scope());value.setBeforeOrganizationNames(before==null?List.of():organizationNames(before.organizationIds()));value.setAfterOrganizationNames(after==null?List.of():organizationNames(after.organizationIds()));return value;}
    private List<String> organizationNames(List<Integer> ids){return ids.stream().map(organizationMapper::selectByPrimaryKey).filter(Objects::nonNull).map(TOrganizationUnit::getName).toList();}
    private List<TPermission> unionPermissions(List<TPermission> first,List<TPermission> second){Map<Integer,TPermission> result=new LinkedHashMap<>();first.forEach(value->result.put(value.getId(),value));second.forEach(value->result.put(value.getId(),value));return new ArrayList<>(result.values());}
    private Set<Integer> unionIds(Set<Integer> first,Set<Integer> second){Set<Integer> result=new LinkedHashSet<>(first);result.addAll(second);return result;}
    private RoleResponse response(TRole r){RoleResponse x=new RoleResponse();x.setId(r.getId());x.setCode(r.getRole());x.setName(r.getRoleName());x.setDescription(r.getDescription());x.setProtectedRole(r.getProtectedRole());x.setProtectedReason(Boolean.TRUE.equals(r.getProtectedRole())?"系统恢复角色，禁止普通维护":null);x.setAuthorizationLevel(r.getAuthorizationLevel());x.setDefaultDataScope(r.getDefaultDataScope());x.setScopeType(r.getScopeType()==null?RoleScopeType.GLOBAL:r.getScopeType());x.setApplicableOrganizations(roleOrganizationMapper.selectByRoleId(r.getId()).stream().map(v->orgOption(organizationMapper.selectByPrimaryKey(v.getOrganizationUnitId()))).toList());x.setMemberCount(roleMapper.countMembers(r.getId()));x.setEnabled(r.getEnabled()==1);x.setVersion(r.getVersion());x.setEditable(isEditable(r));x.setDisabledReason(x.getEditable()?null:disabledReason(r));if(Boolean.TRUE.equals(x.getEditable()))x.setAllowedActions(List.of("EDIT","COPY","STATUS_CHANGE"));else for(String action:List.of("EDIT","STATUS_CHANGE"))x.getUnavailableReasons().put(action,x.getDisabledReason());return x;}
    private OrganizationOption orgOption(TOrganizationUnit u){OrganizationOption x=new OrganizationOption();x.setId(u.getId());x.setName(u.getName());x.setPathName(organizationPath(u));return x;}
    private String organizationPath(TOrganizationUnit unit){if(unit==null)return null;Deque<String> names=new ArrayDeque<>();Set<Integer>seen=new HashSet<>();TOrganizationUnit current=unit;while(current!=null&&seen.add(current.getId())){names.addFirst(current.getName());current=current.getParentId()==null?null:organizationMapper.selectByPrimaryKey(current.getParentId());}return String.join(" / ",names);}
    private PermissionScopeOption permissionScopeOption(TRole role,TPermission permission,boolean roleEditable){PermissionScopeOption option=new PermissionScopeOption();option.setPermissionId(permission.getId());boolean editable=roleEditable&&permission.getEnabled()==1&&(isSecurityAdmin()||(Boolean.TRUE.equals(permission.getDelegable())&&permission.getSensitivityLevel()==PermissionSensitivityLevel.NORMAL&&operatorPermissionIds().contains(permission.getId())));option.setEditable(editable);if(!editable)option.setUnavailableReason("权限已停用、不可委派、敏感或超过操作者上限");Set<DataScopeCode> owned=isSecurityAdmin()?EnumSet.allOf(DataScopeCode.class):operatorPermissionScopes().getOrDefault(permission.getId(),Set.of());for(DataScopeCode scope:DataScopeCode.values()){if(!isSecurityAdmin()&&!UserAuthorizationPolicy.scopeMatrixCovers(owned,scope,true))continue;PermissionDataScopeCandidate candidate=new PermissionDataScopeCandidate();candidate.setCode(scope);candidate.setLabel(scope.name());if(scope==DataScopeCode.CUSTOM_ORGS)candidate.setOrganizationOptions(matrixOrganizationOptions(role));option.getDataScopeCandidates().add(candidate);}return option;}
    private List<OrganizationOption> matrixOrganizationOptions(TRole role){List<TOrganizationUnit> visible=visibleOrganizations();if(role.getScopeType()==RoleScopeType.GLOBAL)return visible.stream().map(this::orgOption).toList();List<Integer> roots=roleOrganizationMapper.selectByRoleId(role.getId()).stream().map(TRoleOrganization::getOrganizationUnitId).toList();return visible.stream().filter(org->roots.stream().anyMatch(root->isDescendant(org.getId(),root))).map(this::orgOption).toList();}
    private PermissionItem permissionItem(TPermission p){PermissionItem x=new PermissionItem();x.setId(p.getId());x.setName(p.getName());x.setCode(p.getCode());x.setModule(p.getModule());x.setType(p.getType());x.setDescription(p.getDescription());x.setSensitivityLevel(p.getSensitivityLevel());x.setDelegable(p.getDelegable());x.setEnabled(p.getEnabled()==1);x.setOrderNo(p.getOrderNo());x.setParentId(p.getParentId());boolean assign=p.getEnabled()==1&&(isSecurityAdmin()||(Boolean.TRUE.equals(p.getDelegable())&&operatorPermissionIds().contains(p.getId())));x.setAssignable(assign);x.setRestrictionReason(assign?null:"权限已停用、不可委派或操作者未拥有");return x;}
    private DifferenceItem difference(TPermission p){DifferenceItem x=new DifferenceItem();x.setPermissionId(p.getId());x.setCode(p.getCode());x.setName(p.getName());x.setSensitivityLevel(p.getSensitivityLevel());return x;}
    private void recordRole(TRole r,AuthorizationChangeType t,String before,String reason){List<Integer>affected=affectedUserIds(r.getId());TAuthorizationHistory h=new TAuthorizationHistory();h.setSubjectType(AuthorizationSubjectType.ROLE);h.setSubjectId(String.valueOf(r.getId()));h.setRoleId(r.getId());h.setChangeType(t);h.setBeforeValue(before);h.setAfterValue(roleSnapshot(r));h.setAffectedUserIds(affected.isEmpty()?null:","+affected.stream().map(String::valueOf).collect(Collectors.joining(",")) + ",");h.setAffectedUsersSnapshot(affected.isEmpty()?null:affectedUserSnapshot(affected));h.setReason(reason==null?"角色变更":reason);auditRecorder.record(h,AuditActionEnum.ROLE_CATALOG_CHANGE,String.valueOf(r.getId()),json(Map.of("roleId",r.getId(),"change",t.name(),"affectedUserCount",affected.size())));}
    private String roleSnapshot(TRole r){List<Integer>ids=roleOrganizationMapper.selectByRoleId(r.getId()).stream().map(TRoleOrganization::getOrganizationUnitId).toList();Map<String,Object>snapshot=new LinkedHashMap<>();snapshot.put("roleId",r.getId());snapshot.put("roleCode",r.getRole());snapshot.put("roleName",r.getRoleName());snapshot.put("authorizationLevel",r.getAuthorizationLevel());snapshot.put("defaultDataScopeCode",r.getDefaultDataScope()==null?null:r.getDefaultDataScope().name());snapshot.put("scopeTypeCode",r.getScopeType()==null?null:r.getScopeType().name());snapshot.put("enabled",r.getEnabled()!=null&&r.getEnabled()==1);snapshot.put("organizations",organizationSnapshots(ids));return json(snapshot);}
    private List<Integer> affectedUserIds(Integer roleId){List<Integer> ids=userRoleMapper.selectCurrentAndFutureUserIdsByRoleId(roleId,LocalDateTime.now());return ids==null?List.of():ids;}
    private void invalidateRoleMembers(Integer roleId){List<Integer> ids=affectedUserIds(roleId);if(ids.isEmpty())return;int rows=userMapper.incrementAuthVersionByIds(ids);if(rows!=ids.size())throw new IllegalStateException("受影响用户安全版本更新不完整");scheduleCleanup(ids);}
    private void recordMatrix(TRole role,Map<Integer,ScopeValue> before,Map<Integer,ScopeValue> after,String reason,List<Integer> affectedUsers){List<TAuthorizationHistory> histories=new ArrayList<>();String affectedIds=affectedUsers.isEmpty()?null:","+affectedUsers.stream().map(String::valueOf).collect(Collectors.joining(","))+",";String affectedSnapshot=affectedUsers.isEmpty()?null:affectedUserSnapshot(affectedUsers);for(Integer permissionId:unionIds(before.keySet(),after.keySet())){ScopeValue oldValue=before.get(permissionId),newValue=after.get(permissionId);if(Objects.equals(oldValue,newValue))continue;TPermission permission=permissionMapper.selectByPrimaryKey(permissionId);TAuthorizationHistory history=new TAuthorizationHistory();history.setSubjectType(AuthorizationSubjectType.ROLE_PERMISSION);history.setSubjectId(role.getId()+":"+permissionId);history.setRoleId(role.getId());history.setPermissionId(permissionId);history.setChangeType(oldValue==null?AuthorizationChangeType.GRANT:newValue==null?AuthorizationChangeType.REVOKE:AuthorizationChangeType.UPDATE);history.setBeforeValue(oldValue==null?null:matrixSnapshot(role,permission,oldValue));history.setAfterValue(newValue==null?null:matrixSnapshot(role,permission,newValue));history.setAffectedUserIds(affectedIds);history.setAffectedUsersSnapshot(affectedSnapshot);history.setReason(reason);histories.add(history);}if(!histories.isEmpty())auditRecorder.recordAll(histories,AuditActionEnum.ROLE_MATRIX_CHANGE,String.valueOf(role.getId()),json(Map.of("roleId",role.getId(),"version",role.getVersion(),"affectedUserCount",affectedUsers.size())));}
    private String matrixSnapshot(TRole role,TPermission permission,ScopeValue value){Map<String,Object> snapshot=new LinkedHashMap<>();snapshot.put("roleId",role.getId());snapshot.put("roleCode",role.getRole());snapshot.put("roleName",role.getRoleName());snapshot.put("permissionId",permission==null?null:permission.getId());snapshot.put("permissionCode",permission==null?null:permission.getCode());snapshot.put("permissionName",permission==null?null:permission.getName());snapshot.put("dataScopeCode",value.scope().name());snapshot.put("organizations",organizationSnapshots(value.organizationIds()));return json(snapshot);}
    private List<Map<String,Object>> organizationSnapshots(List<Integer>ids){List<Map<String,Object>>out=new ArrayList<>();for(Integer id:ids){TOrganizationUnit organization=organizationMapper.selectByPrimaryKey(id);Map<String,Object>item=new LinkedHashMap<>();item.put("id",id);item.put("code",organization==null?null:organization.getCode());item.put("name",organization==null?null:organization.getName());out.add(item);}return out;}
    private String affectedUserSnapshot(List<Integer> userIds){List<Map<String,Object>> snapshots=new ArrayList<>();for(Integer userId:userIds){TUser user=userMapper.selectByPrimaryKey(userId);if(user==null)continue;Map<String,Object> snapshot=new LinkedHashMap<>();snapshot.put("id",user.getId());snapshot.put("code",user.getLoginAct());snapshot.put("name",user.getName());snapshots.add(snapshot);}return json(snapshots);}
    private void scheduleCleanup(List<Integer> ids){securityMutations.accessChanged(ids,"角色或权限矩阵变化");}
    private String json(Object x){try{return objectMapper.writeValueAsString(x);}catch(JacksonException e){throw new IllegalStateException(e);}}
    private void lockMembership(){lockGraph("AUTHORIZATION_MEMBERSHIP_GUARD");}
    private void lockAuthorizationScope(){lockGraph("ORGANIZATION_HIERARCHY");lockGraph("REPORTING_GRAPH");}
    private void lockGraph(String name){if(!name.equals(graphLock.lockByName(name)))throw new IllegalStateException("授权图锁缺失: "+name);}
    private PageInfo<RoleResponse> emptyPage(int page,int size){PageInfo<RoleResponse> out=new PageInfo<>();out.setList(List.of());out.setPageNum(Math.max(1,page));out.setPageSize(Math.min(Math.max(1,size),200));out.setTotal(0);out.setPages(0);return out;}
    private record ScopeValue(DataScopeCode scope,List<Integer> organizationIds){private ScopeValue{organizationIds=List.copyOf(organizationIds);}}
}
