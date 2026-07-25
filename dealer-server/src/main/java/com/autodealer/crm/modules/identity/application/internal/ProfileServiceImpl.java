package com.autodealer.crm.modules.identity.application.internal;

import com.autodealer.crm.modules.identity.application.api.model.TUser;
import com.autodealer.crm.modules.identity.persistence.mapper.TAuthorizationGraphLockMapper;
import com.autodealer.crm.modules.identity.persistence.mapper.TEmployeeAssignmentMapper;
import com.autodealer.crm.modules.identity.persistence.mapper.TEmployeeMapper;
import com.autodealer.crm.modules.identity.persistence.mapper.TEmployeeReportingMapper;
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
import com.autodealer.crm.modules.identity.persistence.model.TEmployee;
import com.autodealer.crm.modules.identity.persistence.model.TEmployeeAssignment;
import com.autodealer.crm.modules.identity.persistence.model.TEmployeeReporting;
import com.autodealer.crm.modules.identity.persistence.model.TOrganizationUnit;
import com.autodealer.crm.modules.identity.persistence.model.TPermission;
import com.autodealer.crm.modules.identity.persistence.model.TPosition;
import com.autodealer.crm.modules.identity.persistence.model.TRole;
import com.autodealer.crm.modules.identity.persistence.model.TRolePermission;
import com.autodealer.crm.modules.identity.persistence.model.TUserPermission;
import com.autodealer.crm.modules.identity.persistence.model.TUserRole;
import com.autodealer.crm.modules.audit.application.api.AuditActionEnum;
import com.autodealer.crm.modules.audit.application.api.OperationAuditRecorder;
import com.autodealer.crm.modules.identity.application.api.security.CurrentUserProvider;
import com.autodealer.crm.modules.identity.application.api.dto.profile.ProfileDtos.*;
import com.autodealer.crm.modules.identity.application.api.enums.AccountType;
import com.autodealer.crm.modules.identity.application.api.enums.PermissionEffect;
import com.autodealer.crm.shared.error.BusinessException;
import com.autodealer.crm.modules.identity.persistence.mapper.*;
import com.autodealer.crm.modules.identity.persistence.model.*;
import com.autodealer.crm.modules.identity.application.api.model.*;
import com.autodealer.crm.shared.error.CodeEnum;
import com.autodealer.crm.modules.identity.application.api.ProfileService;
import com.autodealer.crm.modules.identity.application.api.CredentialService;
import com.autodealer.crm.modules.identity.application.api.command.UserManagementCommand;
import com.autodealer.crm.modules.identity.application.api.PhoneNormalizer;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class ProfileServiceImpl implements ProfileService {
    private static final ObjectMapper AUDIT_JSON = JsonMapper.builder().build();
    private final CurrentUserProvider current; private final TUserMapper users; private final TEmployeeMapper employees;
    private final TEmployeeAssignmentMapper assignments; private final TEmployeeReportingMapper reporting;
    private final TOrganizationUnitMapper organizations; private final TPositionMapper positions;
    private final TRoleMapper roles; private final TUserRoleMapper userRoles; private final TRolePermissionMapper rolePermissions;
    private final TRolePermissionOrganizationMapper rolePermissionOrganizations;
    private final TUserPermissionMapper userPermissions; private final TUserPermissionOrganizationMapper userPermissionOrganizations;
    private final TPermissionMapper permissions;
    private final OperationAuditRecorder audit;
    private final CredentialService credentials;
    private final TAuthorizationGraphLockMapper graphLocks;
    private final UserSecurityMutationCoordinator securityMutations;

    public ProfileServiceImpl(CurrentUserProvider current,TUserMapper users,TEmployeeMapper employees,
      TEmployeeAssignmentMapper assignments,TEmployeeReportingMapper reporting,TOrganizationUnitMapper organizations,
      TPositionMapper positions,TRoleMapper roles,TUserRoleMapper userRoles,TRolePermissionMapper rolePermissions,
      TRolePermissionOrganizationMapper rolePermissionOrganizations,TUserPermissionMapper userPermissions,
      TUserPermissionOrganizationMapper userPermissionOrganizations,TPermissionMapper permissions,OperationAuditRecorder audit,
      CredentialService credentials,TAuthorizationGraphLockMapper graphLocks,UserSecurityMutationCoordinator securityMutations){
      this.current=current;this.users=users;this.employees=employees;this.assignments=assignments;this.reporting=reporting;
      this.organizations=organizations;this.positions=positions;this.roles=roles;this.userRoles=userRoles;this.rolePermissions=rolePermissions;
      this.rolePermissionOrganizations=rolePermissionOrganizations;this.userPermissions=userPermissions;
      this.userPermissionOrganizations=userPermissionOrganizations;this.permissions=permissions;this.audit=audit;this.credentials=credentials;this.graphLocks=graphLocks;this.securityMutations=securityMutations;
    }

    @Override public Profile getOwn(){return build(current.getCurrentUserId());}

    @Override @Transactional @UserManagementCommand("PROFILE_UPDATE_OWN") public Profile updateOwn(UpdateRequest request){
        lockGraph("AVAILABLE_ADMIN_GUARD");
        Integer userId=current.getCurrentUserId(); TUser user=users.selectByPrimaryKeyForUpdate(userId);
        if(user==null)throw new BusinessException(CodeEnum.NOT_FOUND,"账号不存在");
        TEmployee beforeEmployee=employees.selectByUserIdForUpdate(userId);
        String oldName=beforeEmployee==null?user.getName():beforeEmployee.getName();
        String initialPhone=beforeEmployee==null?user.getPhone():beforeEmployee.getPhone();
        String initialEmail=beforeEmployee==null?user.getEmail():beforeEmployee.getEmail();
        boolean oldPhoneVerified=beforeEmployee!=null&&Boolean.TRUE.equals(beforeEmployee.getPhoneVerified());
        boolean oldEmailVerified=beforeEmployee!=null&&Boolean.TRUE.equals(beforeEmployee.getEmailVerified());
        String phone=normalizePhone(request.getPhone()); String email=normalizeEmail(request.getEmail());
        String avatar=normalizeAvatar(request.getAvatarUrl()); String name=request.getName().trim();
        boolean keepsVerifiedPhone=beforeEmployee!=null&&oldPhoneVerified&&Objects.equals(phone,initialPhone)&&phone!=null;
        boolean keepsVerifiedEmail=beforeEmployee!=null&&oldEmailVerified&&Objects.equals(email,initialEmail)&&email!=null;
        if(beforeEmployee!=null&&(oldPhoneVerified||oldEmailVerified)&&!keepsVerifiedPhone&&!keepsVerifiedEmail
                &&isAdministrator(userId)&&users.countAvailableAdminUsersExcluding(userId)<1)
            throw new BusinessException(CodeEnum.LAST_AVAILABLE_ADMIN_REQUIRED,"最后一个可恢复普通管理员必须保留至少一个已验证联系方式");
        try {
            TEmployee employee=beforeEmployee;
            if(employee==null){
                if(user.getAccountType()!=AccountType.SYSTEM)throw new BusinessException(CodeEnum.NOT_FOUND,"员工档案不存在");
                ensureSystemUnique(userId,phone,email);
                if(users.updateSystemProfileByVersion(userId,request.getProfileVersion(),name,phone,email,avatar,userId)!=1)
                    throw new BusinessException(CodeEnum.PROFILE_VERSION_CONFLICT);
            } else {
                ensureEmployeeUnique(userId,phone,email);
                String oldPhone=employee.getPhone(),oldEmail=employee.getEmail();
                employee.setName(name);employee.setPhone(phone);employee.setEmail(email);employee.setAvatarUrl(avatar);
                employee.setPhoneVerified(Objects.equals(phone,oldPhone)&&Boolean.TRUE.equals(employee.getPhoneVerified()));
                employee.setEmailVerified(Objects.equals(email,oldEmail)&&Boolean.TRUE.equals(employee.getEmailVerified()));
                employee.setProfileCompleted(true);employee.setEditTime(LocalDateTime.now());employee.setEditBy(userId);
                if(employees.updateProfileByVersion(employee,request.getProfileVersion())!=1)
                    throw new BusinessException(CodeEnum.PROFILE_VERSION_CONFLICT);
                if(users.updateProfileProjection(userId,name,phone,email,userId)!=1)
                    throw new BusinessException(CodeEnum.OPERATION_FAILED,"账号资料投影同步失败");
            }
        }catch(DuplicateKeyException exception){throw new BusinessException(CodeEnum.DUPLICATE,"手机号或邮箱已存在");}
        List<String> changed=new ArrayList<>();if(!Objects.equals(oldName,name))changed.add("NAME");if(!Objects.equals(initialPhone,phone))changed.add("PHONE");if(!Objects.equals(initialEmail,email))changed.add("EMAIL");if(!Objects.equals(user.getAvatarUrl(),avatar))changed.add("AVATAR");
        if(!Objects.equals(initialPhone,phone)||!Objects.equals(initialEmail,email))credentials.revokeAll(userId);
        TEmployee afterEmployee=employees.selectByUserId(userId);
        Map<String,Object> before=new LinkedHashMap<>();before.put("name",oldName);before.put("phoneChanged",false);before.put("emailChanged",false);before.put("phoneVerified",oldPhoneVerified);before.put("emailVerified",oldEmailVerified);
        Map<String,Object> after=new LinkedHashMap<>();after.put("name",name);after.put("phoneChanged",!Objects.equals(initialPhone,phone));after.put("emailChanged",!Objects.equals(initialEmail,email));after.put("phoneVerified",afterEmployee!=null&&Boolean.TRUE.equals(afterEmployee.getPhoneVerified()));after.put("emailVerified",afterEmployee!=null&&Boolean.TRUE.equals(afterEmployee.getEmailVerified()));
        if(!Objects.equals(oldName,name))securityMutations.ownerEligibilityChanged();
        audit.record(AuditActionEnum.USER_PROFILE_UPDATE,String.valueOf(userId),"SUCCESS",auditJson(Map.of("scope","OWN_PROFILE","changedFieldCodes",changed,"before",before,"after",after)));
        return build(userId);
    }

    private Profile build(Integer userId){
        TUser user=users.selectByPrimaryKey(userId); TEmployee employee=employees.selectByUserId(userId);
        if(user==null)throw new BusinessException(CodeEnum.NOT_FOUND,"个人资料不存在");
        if(employee==null&&user.getAccountType()!=AccountType.SYSTEM)throw new BusinessException(CodeEnum.NOT_FOUND,"员工档案不存在");
        Profile out=new Profile();out.setId(userId);out.setLoginAct(user.getLoginAct());
        out.setName(employee==null?user.getName():employee.getName());out.setPhone(employee==null?user.getPhone():employee.getPhone());
        out.setEmail(employee==null?user.getEmail():employee.getEmail());out.setAvatarUrl(employee==null?user.getAvatarUrl():employee.getAvatarUrl());
        out.setPhoneVerified(employee!=null&&Boolean.TRUE.equals(employee.getPhoneVerified()));out.setEmailVerified(employee!=null&&Boolean.TRUE.equals(employee.getEmailVerified()));
        out.setProfileVersion(employee==null?(user.getProfileVersion()==null?0:user.getProfileVersion()):(employee.getProfileVersion()==null?0:employee.getProfileVersion()));
        LocalDateTime now=LocalDateTime.now();
        if(employee!=null){out.setEmployeeNo(employee.getEmployeeNo());out.setEmploymentStatus(employee.getEmploymentStatus()==null?null:employee.getEmploymentStatus().name());TEmployeeAssignment primary=assignments.selectCurrentPrimaryByEmployeeId(employee.getId(),now);if(primary!=null){TOrganizationUnit org=organizations.selectByPrimaryKey(primary.getOrganizationUnitId());TPosition pos=positions.selectByPrimaryKey(primary.getPositionId());out.setOrganizationName(org==null?null:org.getName());out.setPositionName(pos==null?null:pos.getName());}TEmployeeReporting manager=reporting.selectCurrentDirectBySubordinateId(employee.getId(),now);if(manager!=null){TEmployee value=employees.selectByPrimaryKey(manager.getManagerEmployeeId());out.setManagerName(value==null?null:value.getName());}}
        buildAuthorization(userId,now,out);return out;
    }

    private static String auditJson(Object value){try{return AUDIT_JSON.writeValueAsString(value);}catch(JacksonException e){throw new IllegalStateException("个人资料审计摘要序列化失败",e);}}

    private void buildAuthorization(Integer userId,LocalDateTime now,Profile out){
        Map<Integer,List<PermissionSourceDetail>> sources=new HashMap<>();
        Map<Integer,TUserRole> roleFacts=userRoles.selectEffectiveByUserId(userId,now).stream()
                .collect(java.util.stream.Collectors.toMap(TUserRole::getRoleId,value->value,(left,right)->left));
        for(TRole role:roles.selectByUserId(userId)){
            RoleItem item=new RoleItem();item.setId(role.getId());item.setCode(role.getRole());item.setName(role.getRoleName());item.setSourceDescription(role.getDescription());out.getRoles().add(item);
            TUserRole roleFact=roleFacts.get(role.getId());
            for(TRolePermission rp:rolePermissions.selectByRoleId(role.getId())){
                String dataScopeCode=rp.getDataScopeCode()==null?null:rp.getDataScopeCode().name();
                addPermissionSource(sources,rp.getPermissionId(),"ROLE",role.getRoleName(),dataScopeCode,
                        roleFact==null?null:roleFact.getEffectiveFrom(),roleFact==null?null:roleFact.getEffectiveTo(),
                        roleOrganizations(role.getId(),rp.getPermissionId(),dataScopeCode));
            }
        }
        for(TUserPermission personal:userPermissions.selectEffectiveByUserId(userId,now)){
            if(personal.getEffect()!=PermissionEffect.GRANT)continue;
            String dataScopeCode=personal.getDataScopeCode()==null?null:personal.getDataScopeCode().name();
            addPermissionSource(sources,personal.getPermissionId(),"PERSONAL_GRANT","个人授权",dataScopeCode,
                    personal.getEffectiveFrom(),personal.getEffectiveTo(),personalOrganizations(personal,dataScopeCode));
        }
        List<TPermission> effective=new ArrayList<>();effective.addAll(permissions.selectMenuPermissionByUserId(userId));effective.addAll(permissions.selectButtonPermissionByUserId(userId));
        effective.stream().filter(p->p.getCode()!=null).collect(java.util.stream.Collectors.toMap(TPermission::getId,p->p,(a,b)->a,LinkedHashMap::new)).values().forEach(p->{
            PermissionSource value=new PermissionSource();value.setPermissionCode(p.getCode());value.setPermissionName(p.getName());
            List<PermissionSourceDetail> details=new ArrayList<>(sources.getOrDefault(p.getId(),List.of()));
            if(details.isEmpty())addPermissionSource(Map.of(p.getId(),details),p.getId(),"SYSTEM_EFFECTIVE","系统有效权限",null,null,null,List.of());
            value.setSources(details);
            value.setSourceNames(details.stream().map(PermissionSourceDetail::getSourceName).distinct().toList());
            value.setDataScopeLabel(details.stream().map(PermissionSourceDetail::getDataScopeLabel).filter(Objects::nonNull).distinct().collect(java.util.stream.Collectors.joining(" / ")));
            if(value.getDataScopeLabel().isBlank())value.setDataScopeLabel(null);
            value.setEffectiveTo(details.stream().map(PermissionSourceDetail::getEffectiveTo).filter(Objects::nonNull).max(LocalDateTime::compareTo).orElse(null));
            out.getEffectivePermissions().add(value);
        });
    }

    private static void addPermissionSource(Map<Integer,List<PermissionSourceDetail>> sources,Integer permissionId,String sourceType,String sourceName,String dataScopeCode,LocalDateTime effectiveFrom,LocalDateTime effectiveTo,List<PermissionSourceOrganization> scopedOrganizations){
        PermissionSourceDetail detail=new PermissionSourceDetail();detail.setSourceType(sourceType);detail.setSourceName(sourceName);detail.setDataScopeCode(dataScopeCode);detail.setDataScopeLabel(dataScopeLabel(dataScopeCode));detail.setEffectiveFrom(effectiveFrom);detail.setEffectiveTo(effectiveTo);detail.setOrganizations(new ArrayList<>(scopedOrganizations));
        List<PermissionSourceDetail> details=sources.computeIfAbsent(permissionId,ignored->new ArrayList<>());
        boolean duplicate=details.stream().anyMatch(existing->Objects.equals(existing.getSourceType(),sourceType)&&Objects.equals(existing.getSourceName(),sourceName)&&Objects.equals(existing.getDataScopeCode(),dataScopeCode)&&Objects.equals(existing.getEffectiveFrom(),effectiveFrom)&&Objects.equals(existing.getEffectiveTo(),effectiveTo)&&Objects.equals(existing.getOrganizations(),scopedOrganizations));
        if(!duplicate)details.add(detail);
    }

    private List<PermissionSourceOrganization> roleOrganizations(Integer roleId,Integer permissionId,String dataScopeCode){
        if(!"CUSTOM_ORGS".equals(dataScopeCode))return List.of();
        return organizationScope(rolePermissionOrganizations.selectOrganizationIds(roleId,permissionId));
    }

    private List<PermissionSourceOrganization> personalOrganizations(TUserPermission personal,String dataScopeCode){
        if(!"CUSTOM_ORGS".equals(dataScopeCode)||personal.getId()==null)return List.of();
        return organizationScope(userPermissionOrganizations.selectOrganizationIds(personal.getId()));
    }

    private List<PermissionSourceOrganization> organizationScope(List<Integer> organizationUnitIds){
        if(organizationUnitIds==null||organizationUnitIds.isEmpty())return List.of();
        return organizations.selectByIds(organizationUnitIds).stream().map(organization->{
            PermissionSourceOrganization item=new PermissionSourceOrganization();item.setId(organization.getId());item.setCode(organization.getCode());item.setName(organization.getName());return item;
        }).toList();
    }

    private static String dataScopeLabel(String code){
        if(code==null)return null;
        return switch(code){case "SELF"->"本人";case "DIRECT_REPORTS"->"直属下属";case "REPORTING_TREE"->"完整汇报树";case "PRIMARY_ORG"->"主要组织";case "ORG_TREE"->"组织及下级";case "CUSTOM_ORGS"->"指定组织";case "GLOBAL"->"全局";default->code;};
    }

    private String normalizePhone(String value){String phone=PhoneNormalizer.normalizeMainlandMobile(value);if(phone!=null&&!PhoneNormalizer.isMainlandMobile(phone))throw new BusinessException(CodeEnum.PARAM_ERROR,"手机号格式不正确");return phone;}
    private String normalizeEmail(String value){return value==null||value.isBlank()?null:value.trim().toLowerCase(Locale.ROOT);}
    private String normalizeAvatar(String value){if(value==null||value.isBlank())return null;String avatar=value.trim();if(!(avatar.startsWith("http://")||avatar.startsWith("https://")))throw new BusinessException(CodeEnum.PARAM_ERROR,"头像地址必须使用 http(s)");return avatar;}
    private void ensureEmployeeUnique(Integer uid,String phone,String email){if(phone!=null&&employees.selectByPhoneExcludeUserId(phone,uid)!=null)throw new BusinessException(CodeEnum.DUPLICATE,"手机号已存在");if(email!=null&&employees.selectByEmailExcludeUserId(email,uid)!=null)throw new BusinessException(CodeEnum.DUPLICATE,"邮箱已存在");}
    private void ensureSystemUnique(Integer uid,String phone,String email){if(phone!=null&&users.selectByPhoneExcludeId(phone,uid)!=null)throw new BusinessException(CodeEnum.DUPLICATE,"手机号已存在");if(email!=null&&users.selectByEmailExcludeId(email,uid)!=null)throw new BusinessException(CodeEnum.DUPLICATE,"邮箱已存在");}
    private boolean isAdministrator(Integer userId){return users.selectRolesByUserId(userId).stream().anyMatch(role->"admin".equals(role.getRole()));}
    private void lockGraph(String name){if(!name.equals(graphLocks.lockByName(name)))throw new IllegalStateException("个人资料治理图锁缺失: "+name);}
}
