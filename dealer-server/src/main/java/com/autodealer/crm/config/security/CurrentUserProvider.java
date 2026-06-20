package com.autodealer.crm.config.security;

import com.autodealer.crm.model.TUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUserProvider {
    public TUser getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof TUser user)) {
            throw new IllegalStateException("当前用户不存在");
        }
        return user;
    }

    public Integer getCurrentUserId() {
        return getCurrentUser().getId();
    }

    public boolean isAdmin() {
        return getCurrentUser().getAuthorities().stream()
                .anyMatch(authority -> "admin".equals(authority.getAuthority()));
    }

    public Integer getDataScopeUserId() {
        if (isAdmin()) {
            return null;
        }
        Integer userId = getCurrentUserId();
        if (userId == null) {
            throw new IllegalStateException("当前用户 ID 不能为空");
        }
        return userId;
    }
}
