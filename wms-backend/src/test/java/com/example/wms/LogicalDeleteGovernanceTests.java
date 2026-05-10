package com.example.wms;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.example.wms.audit.DeleteAuditScope;
import com.example.wms.audit.RequestAuditContext;
import com.example.wms.controller.MenuController;
import com.example.wms.controller.PermissionController;
import com.example.wms.controller.RoleController;
import com.example.wms.controller.TenantController;
import com.example.wms.controller.UserController;
import com.example.wms.controller.erp.ErpAssemblyOrderController;
import com.example.wms.controller.erp.ErpCategoryController;
import com.example.wms.controller.erp.ErpCustomerCategoryController;
import com.example.wms.controller.erp.ErpCustomerController;
import com.example.wms.controller.erp.ErpDeliveryMethodController;
import com.example.wms.controller.erp.ErpLocationController;
import com.example.wms.controller.erp.ErpPaymentMethodController;
import com.example.wms.controller.erp.ErpPrintTemplateController;
import com.example.wms.controller.erp.ErpProductController;
import com.example.wms.controller.erp.ErpProductFitmentController;
import com.example.wms.controller.erp.ErpPurchaseOrderController;
import com.example.wms.controller.erp.ErpPurchaseReturnController;
import com.example.wms.controller.erp.ErpSaleOrderController;
import com.example.wms.controller.erp.ErpSaleReturnController;
import com.example.wms.controller.erp.ErpSettlementMethodController;
import com.example.wms.controller.erp.ErpSupplierController;
import com.example.wms.controller.erp.ErpUnitController;
import com.example.wms.controller.erp.ErpVehicleBrandController;
import com.example.wms.controller.erp.ErpVehicleModelController;
import com.example.wms.controller.erp.ErpVehicleSeriesController;
import com.example.wms.controller.erp.ErpWarehouseController;
import com.example.wms.dto.DeleteRequest;
import com.example.wms.entity.IdempotencyRecord;
import com.example.wms.entity.Menu;
import com.example.wms.entity.Permission;
import com.example.wms.entity.Role;
import com.example.wms.entity.Tenant;
import com.example.wms.entity.TenantMenu;
import com.example.wms.entity.UserAccount;
import com.example.wms.entity.base.AuditableSoftDeleteEntity;
import com.example.wms.entity.base.SoftDeleteEntity;
import com.example.wms.entity.base.TenantAuditableSoftDeleteEntity;
import com.example.wms.entity.erp.ErpAccountsPayable;
import com.example.wms.entity.erp.ErpAccountsReceivable;
import com.example.wms.entity.erp.ErpPayment;
import com.example.wms.entity.erp.ErpReceipt;
import com.example.wms.entity.erp.ErpStockCount;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class LogicalDeleteGovernanceTests {
    @AfterEach
    void clearAuditContext() {
        RequestAuditContext.clear();
    }

    @Test
    void deleteAuditScopeRestoresPreviousReason() {
        RequestAuditContext context = new RequestAuditContext();
        context.setDeleteReason("before");
        RequestAuditContext.set(context);

        try (DeleteAuditScope ignored = DeleteAuditScope.bind("cleanup")) {
            assertThat(RequestAuditContext.get().getDeleteReason()).isEqualTo("cleanup");
        }

        assertThat(RequestAuditContext.get().getDeleteReason()).isEqualTo("before");
    }

    @Test
    void deleteEndpointsRequireDeleteRequestPayload() {
        for (Class<?> controllerClass : deleteControlledControllers()) {
            for (Method method : controllerClass.getDeclaredMethods()) {
                DeleteMapping mapping = method.getAnnotation(DeleteMapping.class);
                if (mapping == null) {
                    continue;
                }
                assertThat(method.getParameters())
                    .withFailMessage("%s.%s 缺少 DeleteRequest 删除原因入参", controllerClass.getSimpleName(), method.getName())
                    .anyMatch(parameter -> parameter.getType().equals(DeleteRequest.class));
            }
        }
    }

    @Test
    void controllersDoNotExposeRestoreRoutes() {
        for (Class<?> controllerClass : deleteControlledControllers()) {
            RequestMapping requestMapping = controllerClass.getAnnotation(RequestMapping.class);
            String classPath = requestMapping == null || requestMapping.value().length == 0 ? "" : requestMapping.value()[0];
            for (Method method : controllerClass.getDeclaredMethods()) {
                assertThat(method.getName().toLowerCase()).doesNotContain("restore");
                for (String path : mappedPaths(method)) {
                    assertThat((classPath + path).toLowerCase()).doesNotContain("restore");
                }
            }
        }
    }

    @Test
    void logicalDeleteEntitiesReuseCommonBaseClasses() {
        assertThat(Tenant.class.getSuperclass()).isEqualTo(AuditableSoftDeleteEntity.class);
        assertThat(Permission.class.getSuperclass()).isEqualTo(AuditableSoftDeleteEntity.class);
        assertThat(Menu.class.getSuperclass()).isEqualTo(AuditableSoftDeleteEntity.class);
        assertThat(UserAccount.class.getSuperclass()).isEqualTo(TenantAuditableSoftDeleteEntity.class);
        assertThat(Role.class.getSuperclass()).isEqualTo(TenantAuditableSoftDeleteEntity.class);
        assertThat(TenantMenu.class.getSuperclass()).isEqualTo(TenantAuditableSoftDeleteEntity.class);
        assertThat(IdempotencyRecord.class.getSuperclass()).isEqualTo(SoftDeleteEntity.class);
        assertThat(ErpAccountsReceivable.class.getSuperclass()).isEqualTo(TenantAuditableSoftDeleteEntity.class);
        assertThat(ErpAccountsPayable.class.getSuperclass()).isEqualTo(TenantAuditableSoftDeleteEntity.class);
        assertThat(ErpReceipt.class.getSuperclass()).isEqualTo(TenantAuditableSoftDeleteEntity.class);
        assertThat(ErpPayment.class.getSuperclass()).isEqualTo(TenantAuditableSoftDeleteEntity.class);
        assertThat(ErpStockCount.class.getSuperclass()).isEqualTo(TenantAuditableSoftDeleteEntity.class);
        assertThat(findDeletedAtField(AuditableSoftDeleteEntity.class).getAnnotation(TableLogic.class)).isNotNull();
        assertThat(findDeletedAtField(SoftDeleteEntity.class).getAnnotation(TableLogic.class)).isNotNull();
    }

    private static List<Class<?>> deleteControlledControllers() {
        return List.of(
            UserController.class,
            TenantController.class,
            PermissionController.class,
            MenuController.class,
            RoleController.class,
            ErpAssemblyOrderController.class,
            ErpCategoryController.class,
            ErpCustomerController.class,
            ErpCustomerCategoryController.class,
            ErpDeliveryMethodController.class,
            ErpLocationController.class,
            ErpPaymentMethodController.class,
            ErpPrintTemplateController.class,
            ErpProductController.class,
            ErpProductFitmentController.class,
            ErpPurchaseOrderController.class,
            ErpPurchaseReturnController.class,
            ErpSaleOrderController.class,
            ErpSaleReturnController.class,
            ErpSettlementMethodController.class,
            ErpSupplierController.class,
            ErpUnitController.class,
            ErpVehicleBrandController.class,
            ErpVehicleModelController.class,
            ErpVehicleSeriesController.class,
            ErpWarehouseController.class
        );
    }

    private static List<String> mappedPaths(Method method) {
        return Stream.of(
                annotationValues(method.getAnnotation(GetMapping.class)),
                annotationValues(method.getAnnotation(PostMapping.class)),
                annotationValues(method.getAnnotation(PutMapping.class)),
                annotationValues(method.getAnnotation(PatchMapping.class)),
                annotationValues(method.getAnnotation(DeleteMapping.class))
            )
            .flatMap(List::stream)
            .toList();
    }

    private static List<String> annotationValues(Annotation annotation) {
        if (annotation == null) {
            return List.of();
        }
        try {
            Method valueMethod = annotation.annotationType().getMethod("value");
            String[] values = (String[]) valueMethod.invoke(annotation);
            return values == null || values.length == 0 ? List.of("") : List.of(values);
        } catch (Exception ex) {
            return List.of();
        }
    }

    private static Field findDeletedAtField(Class<?> type) {
        Optional<Field> field = Stream.of(type.getDeclaredFields())
            .filter(item -> "deletedAt".equals(item.getName()))
            .findFirst();
        assertThat(field).isPresent();
        return field.orElseThrow();
    }
}
