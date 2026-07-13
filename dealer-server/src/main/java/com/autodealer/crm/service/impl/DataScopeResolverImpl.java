package com.autodealer.crm.service.impl;

import com.autodealer.crm.enums.*;
import com.autodealer.crm.mapper.*;
import com.autodealer.crm.model.*;
import com.autodealer.crm.service.AuthorizationDataScope;
import com.autodealer.crm.service.DataScopeResolver;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class DataScopeResolverImpl implements DataScopeResolver {
    private final TPermissionMapper permissionMapper; private final TUserPermissionMapper userPermissionMapper;
    private final TUserRoleMapper userRoleMapper; private final TRoleMapper roleMapper;
    private final TRolePermissionMapper rolePermissionMapper; private final TRoleOrganizationMapper roleOrganizationMapper;
    private final TRolePermissionOrganizationMapper rolePermissionOrganizationMapper;
    private final TUserPermissionOrganizationMapper userPermissionOrganizationMapper;
    private final TEmployeeMapper employeeMapper; private final TEmployeeAssignmentMapper assignmentMapper;
    private final TEmployeeReportingMapper reportingMapper; private final TOrganizationUnitMapper organizationMapper;

    public DataScopeResolverImpl(TPermissionMapper permissionMapper,TUserPermissionMapper userPermissionMapper,
                                 TUserRoleMapper userRoleMapper,TRoleMapper roleMapper,TRolePermissionMapper rolePermissionMapper,
                                 TRoleOrganizationMapper roleOrganizationMapper,
                                 TRolePermissionOrganizationMapper rolePermissionOrganizationMapper,
                                 TUserPermissionOrganizationMapper userPermissionOrganizationMapper,TEmployeeMapper employeeMapper,
                                 TEmployeeAssignmentMapper assignmentMapper,TEmployeeReportingMapper reportingMapper,
                                 TOrganizationUnitMapper organizationMapper){this.permissionMapper=permissionMapper;this.userPermissionMapper=userPermissionMapper;this.userRoleMapper=userRoleMapper;this.roleMapper=roleMapper;this.rolePermissionMapper=rolePermissionMapper;this.roleOrganizationMapper=roleOrganizationMapper;this.rolePermissionOrganizationMapper=rolePermissionOrganizationMapper;this.userPermissionOrganizationMapper=userPermissionOrganizationMapper;this.employeeMapper=employeeMapper;this.assignmentMapper=assignmentMapper;this.reportingMapper=reportingMapper;this.organizationMapper=organizationMapper;}

    @Override public AuthorizationDataScope resolve(Integer userId,String permissionCode){
        TPermission permission=permissionMapper.selectByCode(permissionCode);if(permission==null||permission.getEnabled()==null||permission.getEnabled()!=1)return AuthorizationDataScope.none();
        LocalDateTime now=LocalDateTime.now();TUserPermission personal=userPermissionMapper.selectCurrentEffective(userId,permission.getId(),now);
        if(personal!=null&&personal.getEffect()==PermissionEffect.DENY)return AuthorizationDataScope.none();
        Set<DataScopeCode> scopes=new LinkedHashSet<>();Set<Integer> customOrgs=new LinkedHashSet<>();
        for(TUserRole assignment:userRoleMapper.selectEffectiveByUserId(userId,now)){TRole role=roleMapper.selectByPrimaryKey(assignment.getRoleId());if(!roleApplicable(role,userId,now))continue;
            for(TRolePermission source:rolePermissionMapper.selectByRoleId(role.getId()))if(source.getPermissionId().equals(permission.getId())){scopes.add(source.getDataScopeCode());if(source.getDataScopeCode()==DataScopeCode.CUSTOM_ORGS)customOrgs.addAll(rolePermissionOrganizationMapper.selectOrganizationIds(role.getId(),permission.getId()));}}
        if(personal!=null&&personal.getEffect()==PermissionEffect.GRANT){scopes.add(personal.getDataScopeCode());if(personal.getDataScopeCode()==DataScopeCode.CUSTOM_ORGS)customOrgs.addAll(userPermissionOrganizationMapper.selectOrganizationIds(personal.getId()));}
        if(scopes.contains(DataScopeCode.GLOBAL))return AuthorizationDataScope.global(scopes);
        TEmployee employee=employeeMapper.selectByUserId(userId);if(employee==null)return scopes.contains(DataScopeCode.SELF)?new AuthorizationDataScope(false,scopes,Set.of(userId),Set.of()):AuthorizationDataScope.none();
        AuthorizationDataScope.Builder result=AuthorizationDataScope.builder();scopes.forEach(result::scope);
        TEmployeeAssignment primary=assignmentMapper.selectCurrentPrimaryByEmployeeId(employee.getId(),now);
        for(DataScopeCode scope:scopes)switch(scope){
            case SELF->result.user(userId);
            case DIRECT_REPORTS->result.users(reportingMapper.selectEffectiveSubordinates(employee.getId(),now).stream().map(TEmployeeReporting::getSubordinateEmployeeId).map(employeeMapper::selectByPrimaryKey).filter(Objects::nonNull).map(TEmployee::getUserId).filter(Objects::nonNull).toList());
            case REPORTING_TREE->result.users(reportingTreeUsers(employee.getId(),now));
            case PRIMARY_ORG->{if(primary!=null){result.org(primary.getOrganizationUnitId());result.users(userIds(List.of(primary.getOrganizationUnitId()),now));}}
            case ORG_TREE->{if(primary!=null){List<Integer>orgs=descendantOrganizations(primary.getOrganizationUnitId());result.orgs(orgs);result.users(userIds(orgs,now));}}
            case CUSTOM_ORGS->{result.orgs(customOrgs);result.users(userIds(new ArrayList<>(customOrgs),now));}
            case GLOBAL->throw new IllegalStateException("GLOBAL 已提前返回");
        }
        return result.build();
    }

    private boolean roleApplicable(TRole role,Integer userId,LocalDateTime now){if(role==null||role.getEnabled()==null||role.getEnabled()!=1)return false;if(role.getScopeType()==RoleScopeType.GLOBAL)return true;TEmployee employee=employeeMapper.selectByUserId(userId);TEmployeeAssignment primary=employee==null?null:assignmentMapper.selectCurrentPrimaryByEmployeeId(employee.getId(),now);return primary!=null&&roleOrganizationMapper.selectByRoleId(role.getId()).stream().anyMatch(v->isDescendant(primary.getOrganizationUnitId(),v.getOrganizationUnitId()));}
    private Set<Integer> reportingTreeUsers(Integer managerId,LocalDateTime now){Set<Integer>employeeIds=new LinkedHashSet<>();Deque<Integer>queue=new ArrayDeque<>();queue.add(managerId);while(!queue.isEmpty()){Integer current=queue.removeFirst();for(TEmployeeReporting relation:reportingMapper.selectEffectiveSubordinates(current,now))if(employeeIds.add(relation.getSubordinateEmployeeId()))queue.addLast(relation.getSubordinateEmployeeId());}Set<Integer>users=new LinkedHashSet<>();for(Integer id:employeeIds){TEmployee e=employeeMapper.selectByPrimaryKey(id);if(e!=null&&e.getUserId()!=null)users.add(e.getUserId());}return users;}
    private List<Integer>descendantOrganizations(Integer root){List<Integer> ids=organizationMapper.selectDescendantIds(root);return ids==null?List.of():ids;}
    private boolean isDescendant(Integer id,Integer ancestor){Set<Integer>visited=new HashSet<>();Integer current=id;while(current!=null&&visited.add(current)){if(current.equals(ancestor))return true;TOrganizationUnit unit=organizationMapper.selectByPrimaryKey(current);current=unit==null?null:unit.getParentId();}return false;}
    private List<Integer>userIds(List<Integer>orgs,LocalDateTime now){return orgs.isEmpty()?List.of():employeeMapper.selectUserIdsByOrganizationUnitIds(orgs,now);}
}
