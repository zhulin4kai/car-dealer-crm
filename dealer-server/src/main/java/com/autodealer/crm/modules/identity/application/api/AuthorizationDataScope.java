package com.autodealer.crm.modules.identity.application.api;

import com.autodealer.crm.modules.identity.application.api.enums.DataScopeCode;

import java.util.LinkedHashSet;
import java.util.Set;

/** 某一用户、某一权限动作解析后的结构化范围。 */
public record AuthorizationDataScope(boolean global, Set<DataScopeCode> sourceScopes,
                                     Set<Integer> visibleUserIds, Set<Integer> visibleOrganizationIds) {
    public AuthorizationDataScope {
        sourceScopes = Set.copyOf(sourceScopes == null ? Set.of() : sourceScopes);
        visibleUserIds = Set.copyOf(visibleUserIds == null ? Set.of() : visibleUserIds);
        visibleOrganizationIds = Set.copyOf(visibleOrganizationIds == null ? Set.of() : visibleOrganizationIds);
    }
    public static AuthorizationDataScope none() { return new AuthorizationDataScope(false, Set.of(), Set.of(), Set.of()); }
    public static AuthorizationDataScope global(Set<DataScopeCode> sources) { return new AuthorizationDataScope(true, sources, Set.of(), Set.of()); }
    public static Builder builder() { return new Builder(); }
    public static final class Builder {
        private final Set<DataScopeCode> scopes=new LinkedHashSet<>(); private final Set<Integer> users=new LinkedHashSet<>(); private final Set<Integer> orgs=new LinkedHashSet<>();
        public Builder scope(DataScopeCode v){scopes.add(v);return this;} public Builder user(Integer v){if(v!=null)users.add(v);return this;} public Builder org(Integer v){if(v!=null)orgs.add(v);return this;}
        public Builder users(java.util.Collection<Integer> v){users.addAll(v);return this;} public Builder orgs(java.util.Collection<Integer> v){orgs.addAll(v);return this;}
        public AuthorizationDataScope build(){return new AuthorizationDataScope(false,scopes,users,orgs);}
    }
}
