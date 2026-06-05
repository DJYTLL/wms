package com.example.wms.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Component("systemConfigPermissionEvaluator")
public class SystemConfigPermissionEvaluator {
    private static final Set<String> SQL_TIMING_KEYS = Set.of(
        "wms.monitor.sql-timing-enabled",
        "wms.monitor.sql-timing-log-params"
    );
    private static final String SQL_TIMING_VIEW = "PERM_system-config:sql-timing:view";
    private static final String SQL_TIMING_EDIT = "PERM_system-config:sql-timing:edit";

    public boolean canView(String key) {
        return hasPermission(key, SQL_TIMING_VIEW);
    }

    public boolean canEdit(String key) {
        return hasPermission(key, SQL_TIMING_EDIT);
    }

    private boolean hasPermission(String key, String requiredAuthority) {
        if (!SQL_TIMING_KEYS.contains(key)) {
            return false;
        }
        Authentication authentication = org.springframework.security.core.context.SecurityContextHolder
            .getContext()
            .getAuthentication();
        if (authentication == null) {
            return false;
        }
        Set<String> authorities = authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.toSet());
        return authorities.contains(requiredAuthority);
    }
}
