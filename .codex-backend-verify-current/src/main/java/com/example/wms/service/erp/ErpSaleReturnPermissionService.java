package com.example.wms.service.erp;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service("erpSaleReturnPermissionService")
public class ErpSaleReturnPermissionService {
    private static final String SOURCE_SALE_ORDER_VIEW = "PERM_erp-sale-return-draft:source-view";

    public boolean canViewSourceSaleOrders() {
        return hasAuthority(SOURCE_SALE_ORDER_VIEW);
    }

    private boolean hasAuthority(String authority) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        return authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .anyMatch(authority::equals);
    }
}
