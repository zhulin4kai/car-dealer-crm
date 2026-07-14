package com.autodealer.crm.modules.identity.application.api.dto.user;

import com.autodealer.crm.modules.identity.application.api.dto.credential.CredentialDtos.ManagedDeliveryResult;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.util.*;
import java.time.OffsetDateTime;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;

public final class ManagedUserDtos {
  private ManagedUserDtos(){}
  @Data public static class CreateRequest {
    @NotBlank @Size(max=32) private String loginAct;@NotBlank @Size(max=50) private String name;
    @Size(max=18) private String phone;@Email @Size(max=64) private String email;@NotBlank @Size(max=32) private String employeeNo;
    @NotNull private Integer organizationUnitId;@NotNull private Integer positionId;private Integer managerEmployeeId;
    @NotNull @Size(max=100) private List<Integer> roleIds=new ArrayList<>();
    private boolean bootstrapRootLeader;
    @Min(0) private Integer expectedRootOrganizationVersion;
    @JsonAnySetter public void rejectUnknown(String name,Object value){throw new IllegalArgumentException("用户创建不允许字段: "+name);}
  }
  @Data public static class Detail {
    private Integer id;private Integer employeeId;private String employeeNo;private String loginAct;private String name;private String phone;private String email;
    private String organizationName;private String positionName;private String managerName;private String employmentStatus;private String accountStatus;private String lockStatus;private String lockReason;
    private Boolean accountExpired;private Boolean credentialExpired;private OffsetDateTime accountExpiresAt;private OffsetDateTime credentialExpiresAt;
    private Date lastLoginTime;private Integer profileVersion;private Integer accountVersion;private Integer employeeVersion;
    private Integer authorizationVersion;private Long sessionRevision;private List<String> roleNames=new ArrayList<>();
    private List<StatusCommandOption> statusCommands=new ArrayList<>();private List<String> allowedActions=new ArrayList<>();private Map<String,String> unavailableReasons=new LinkedHashMap<>();
  }
  @Data public static class Summary {
    private Integer id;private Integer employeeId;private String employeeNo;private String name;private String loginAct;
    private String organizationName;private String positionName;private String managerName;private List<String> roleNames=new ArrayList<>();
    private String employmentStatus;private String accountStatus;private String lockStatus;private Date lastLoginTime;
    private List<String> allowedActions=new ArrayList<>();private Map<String,String> unavailableReasons=new LinkedHashMap<>();
    @JsonIgnore private String accountType;@JsonIgnore private Boolean protectedAccount;
  }
  @Data public static class FilterOption { private Object id;private String label; }
  @Data public static class FilterOptions {
    private List<FilterOption> organizations=new ArrayList<>();private List<FilterOption> positions=new ArrayList<>();
    private List<FilterOption> managers=new ArrayList<>();private List<FilterOption> roles=new ArrayList<>();
    private List<FilterOption> assignableRoles=new ArrayList<>();
    private List<FilterOption> employmentStatuses=new ArrayList<>();private List<FilterOption> accountStatuses=new ArrayList<>();
    private List<FilterOption> lockStatuses=new ArrayList<>();
    private boolean bootstrapRequired;private boolean bootstrapAllowed;
    private Integer bootstrapRootOrganizationId;private Integer bootstrapRootOrganizationVersion;
  }
  @Data public static class RoleNameRow { private Integer userId;private String roleName; }
  @Data public static class StatusCommandOption {
    private String command;private String label;private boolean destructive;private String disabledReason;
    public StatusCommandOption(){}
    public StatusCommandOption(String command,String label,boolean destructive,String disabledReason){this.command=command;this.label=label;this.destructive=destructive;this.disabledReason=disabledReason;}
  }
  public record CreateResult(Detail user, ManagedDeliveryResult credentialDelivery){}
  @Data public static class ProfileRequest {
    @NotNull @Min(0) private Integer profileVersion;
    @NotBlank @Size(max=50) private String name;
    @Size(max=18) private String phone;
    @Email @Size(max=64) private String email;
    @JsonAnySetter public void rejectUnknown(String name,Object value){throw new IllegalArgumentException("受管资料不允许字段: "+name);}
  }
  @Data public static class StatusRequest { @NotNull @Min(0) private Integer accountVersion;@NotBlank @Pattern(regexp="ENABLE|DISABLE|LOCK|UNLOCK") private String command;@NotBlank @Size(max=500) private String reason;@JsonAnySetter public void rejectUnknown(String name,Object value){throw new IllegalArgumentException("账号状态命令不允许字段: "+name);} }
  @Data public static class LoginAccountRequest {
    @NotNull @Min(0) private Integer accountVersion;
    @NotBlank @Size(min=3,max=32) @Pattern(regexp="[A-Za-z0-9._@-]+") private String loginAct;
    @NotBlank @Size(max=500) private String reason;
    @JsonAnySetter public void rejectUnknown(String name,Object value){throw new IllegalArgumentException("登录账号命令不允许字段: "+name);}
  }
  @Data public static class SecurityExpirationRequest {
    @NotNull @Min(0) private Integer accountVersion;
    private OffsetDateTime accountExpiresAt;
    private OffsetDateTime credentialExpiresAt;
    @NotBlank @Size(max=500) private String reason;
    @JsonAnySetter public void rejectUnknown(String name,Object value){throw new IllegalArgumentException("账号安全到期命令不允许字段: "+name);}
  }
}
