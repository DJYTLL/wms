package com.example.wms.controller.erp;

import com.example.wms.dto.ApiResponse;
import com.example.wms.dto.erp.ErpVehicleFitmentBootstrapData;
import com.example.wms.entity.erp.ErpProduct;
import com.example.wms.service.erp.ErpVehicleFitmentBootstrapService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/erp/vehicle-fitments")
public class ErpVehicleFitmentBootstrapController {
    private final ErpVehicleFitmentBootstrapService erpVehicleFitmentBootstrapService;

    public ErpVehicleFitmentBootstrapController(ErpVehicleFitmentBootstrapService erpVehicleFitmentBootstrapService) {
        this.erpVehicleFitmentBootstrapService = erpVehicleFitmentBootstrapService;
    }

    @GetMapping("/bootstrap")
    @PreAuthorize(
        "hasAuthority('PERM_erp-product-fitment:view') and " +
        "hasAuthority('PERM_erp-vehicle-brand:view') and " +
        "hasAuthority('PERM_erp-vehicle-series:view') and " +
        "hasAuthority('PERM_erp-vehicle-model:view') and " +
        "hasAuthority('PERM_erp-product:view')"
    )
    public ResponseEntity<ApiResponse<ErpVehicleFitmentBootstrapData>> bootstrap() {
        ErpVehicleFitmentBootstrapData data = erpVehicleFitmentBootstrapService.loadBootstrapData();
        stripCostPriceIfNeeded(data.products());
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    private void stripCostPriceIfNeeded(List<ErpProduct> products) {
        if (products == null || canViewCostPrice()) {
            return;
        }
        for (ErpProduct product : products) {
            if (product != null) {
                product.setCostPrice(null);
            }
        }
    }

    private boolean canViewCostPrice() {
        return hasAuthority("PERM_erp-product:cost:view") || hasAuthority("PERM_erp-product:cost:edit");
    }

    private boolean hasAuthority(String authority) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getAuthorities() == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
            .anyMatch(item -> authority.equals(item.getAuthority()));
    }
}
