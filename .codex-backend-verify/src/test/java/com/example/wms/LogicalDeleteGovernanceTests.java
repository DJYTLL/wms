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

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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

    @Test
    void userUsernameActiveUniqueIndexIsTenantScoped() throws IOException {
        String migration = Files.readString(Path.of(
            "src/main/resources/db/migration/V115__tenant_scoped_user_username_unique.sql"
        ));

        assertThat(migration).contains("DROP INDEX IF EXISTS uq_app_user_username_active");
        assertThat(migration).contains("ON app_user (tenant_id, username)");
        assertThat(migration).contains("WHERE deleted_at IS NULL");
    }

    @Test
    void businessWriteServiceMethodsDeclareTransactions() throws IOException {
        Path mainRoot = Path.of("src/main/java/com/example/wms");
        Pattern methodPattern = Pattern.compile(
            "(?ms)^\\s*public\\s+(?!class\\b|interface\\b|record\\b)[\\w<>?,\\s\\.\\[\\]]+\\s+(\\w+)\\s*\\([^;]*?\\)\\s*(?:throws [^{]+)?\\{"
        );
        Pattern writePattern = Pattern.compile(
            "\\w+Mapper\\.(?:insert|update|delete|softDelete|increment|insertIgnore|updateById|deleteById|update\\w*|delete\\w*)\\w*\\s*\\("
                + "|refreshTokenService\\.(?:issueTokens|refresh|revoke)\\s*\\("
                + "|refreshTokenMapper\\.(?:insert|update|delete|revoke)\\w*\\s*\\("
        );
        List<String> missing = new ArrayList<>();
        Set<String> exclusions = Set.of(
            "service/impl/QzTraySigningServiceImpl.java#sign"
        );

        try (Stream<Path> paths = Files.walk(mainRoot)) {
            for (Path path : paths.filter(Files::isRegularFile)
                .filter(item -> item.getFileName().toString().endsWith(".java"))
                .filter(item -> {
                    String normalized = mainRoot.relativize(item).toString().replace('\\', '/');
                    return !normalized.startsWith("mapper/")
                        && !normalized.startsWith("entity/")
                        && !normalized.startsWith("dto/");
                })
                .toList()) {
                String text = Files.readString(path);
                boolean classTransactional = Pattern.compile(
                    "(?s)@Transactional\\s*(?:\\([^)]*\\)\\s*)?\\R\\s*public\\s+class"
                ).matcher(text).find();
                Matcher matcher = methodPattern.matcher(text);
                while (matcher.find()) {
                    String methodName = matcher.group(1);
                    int brace = text.indexOf('{', matcher.start());
                    int end = findMethodEnd(text, brace);
                    String body = text.substring(brace, end);
                    String prefix = text.substring(Math.max(0, matcher.start() - 350), matcher.start());
                    String key = mainRoot.relativize(path).toString().replace('\\', '/') + "#" + methodName;
                    if (writePattern.matcher(body).find()
                        && !classTransactional
                        && !prefix.contains("@Transactional")
                        && !body.contains("transactionTemplate.execute")
                        && !exclusions.contains(key)) {
                        missing.add(key);
                    }
                }
            }
        }

        assertThat(missing)
            .withFailMessage("以下公开写业务方法缺少 @Transactional: %s", missing)
            .isEmpty();
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

    private static int findMethodEnd(String text, int brace) {
        int depth = 0;
        for (int i = brace; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch == '{') {
                depth++;
            } else if (ch == '}') {
                depth--;
                if (depth == 0) {
                    return i + 1;
                }
            }
        }
        return text.length();
    }
}
