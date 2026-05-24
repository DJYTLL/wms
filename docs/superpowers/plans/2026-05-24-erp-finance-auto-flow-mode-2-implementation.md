# ERP Finance Auto Flow Mode 2 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Decouple audit-time inventory effects from post-audit financial auto-flow, add one global finance auto-flow mode, and unify manual-takeover blocking across sale/purchase order and return flows.

**Architecture:** Keep audit status changes and stock mutations inside each source document service, but move receivable/payable and receipt/payment generation into a shared `ErpFinanceAutoFlowService` plus guard helpers. Persist the global mode and source metadata in the database, return structured blocking results to the front end, and stage “restore system default” as a second-phase capability after the new source metadata and guard rules are stable.

**Tech Stack:** Spring Boot 3, MyBatis-Plus, Flyway, Java 17, Vue 3, TypeScript, Element Plus, JUnit 5, Mockito, Maven Wrapper

---

## File Structure

### Existing files to modify

- `wms-backend/src/main/resources/db/migration/*.sql`
  Responsibility: add schema/config support for auto-flow mode and structured source metadata.
- `wms-backend/src/main/java/com/example/wms/service/impl/TenantSettingServiceImpl.java`
  Responsibility: expose and validate the tenant-scoped global finance auto-flow mode.
- `wms-backend/src/main/java/com/example/wms/service/erp/impl/ErpSaleOrderServiceImpl.java`
  Responsibility: keep audit + stock behavior local, delegate post-audit finance auto-flow.
- `wms-backend/src/main/java/com/example/wms/service/erp/impl/ErpPurchaseOrderServiceImpl.java`
  Responsibility: same split for purchase orders.
- `wms-backend/src/main/java/com/example/wms/service/erp/impl/ErpSaleReturnServiceImpl.java`
  Responsibility: same split for sale returns.
- `wms-backend/src/main/java/com/example/wms/service/erp/impl/ErpPurchaseReturnServiceImpl.java`
  Responsibility: same split for purchase returns.
- `wms-backend/src/main/java/com/example/wms/service/erp/impl/ErpReceiptServiceImpl.java`
  Responsibility: enforce structured manual-takeover guard behavior for auto receipts.
- `wms-backend/src/main/java/com/example/wms/service/erp/impl/ErpPaymentServiceImpl.java`
  Responsibility: enforce structured manual-takeover guard behavior for auto payments.
- `auto-parts-wms-vue/src/views/system/TenantSettingManagement.vue`
  Responsibility: expose the unified finance auto-flow mode in tenant settings.
- `auto-parts-wms-vue/src/locales/zh.ts`
- `auto-parts-wms-vue/src/locales/en.ts`
  Responsibility: add labels and blocking copy.

### New files to create

- `wms-backend/src/main/java/com/example/wms/service/erp/support/FinanceAutoFlowMode.java`
- `wms-backend/src/main/java/com/example/wms/service/erp/support/AutoFlowDocumentType.java`
- `wms-backend/src/main/java/com/example/wms/service/erp/support/AutoFlowCommand.java`
- `wms-backend/src/main/java/com/example/wms/service/erp/support/AutoFlowBlockResult.java`
- `wms-backend/src/main/java/com/example/wms/service/erp/support/ErpFinanceAutoFlowService.java`
- `wms-backend/src/main/java/com/example/wms/service/erp/support/ErpFinanceAutoFlowGuardService.java`
  Responsibility: shared mode parsing, orchestration, and blocking decisions.
- `wms-backend/src/test/java/com/example/wms/ErpFinanceAutoFlowServiceTest.java`
- `wms-backend/src/test/java/com/example/wms/ErpFinanceAutoFlowGuardServiceTest.java`
- `wms-backend/src/test/java/com/example/wms/TenantSettingFinanceAutoFlowModeTest.java`
  Responsibility: isolated tests for orchestration, guard decisions, and config parsing.

## Task 1: Add schema and config support for the global mode and source metadata

**Files:**
- Modify: `wms-backend/src/main/resources/db/migration`
- Modify: `wms-backend/src/main/java/com/example/wms/service/impl/TenantSettingServiceImpl.java`
- Test: `wms-backend/src/test/java/com/example/wms/TenantSettingFinanceAutoFlowModeTest.java`

- [ ] **Step 1: Verify migration numbering before writing SQL**

Run:

```powershell
Get-ChildItem -Path 'D:\project\wms-backend\src\main\resources\db\migration' | Sort-Object Name | Select-Object -ExpandProperty Name | Select-Object -Last 5
```

Expected: highest version remains `V112__seed_api_latency_monitor_menu_and_permission.sql`, so the next migration is `V113__erp_finance_auto_flow_mode.sql` with no conflict.

- [ ] **Step 2: Write the failing config parsing test**

```java
@Test
void getFinanceAutoFlowModeFallsBackToApprovedPaymentWhenUnset() {
    when(systemConfigMapper.selectOne(any())).thenReturn(null);

    FinanceAutoFlowMode mode = service.getFinanceAutoFlowMode();

    assertThat(mode).isEqualTo(FinanceAutoFlowMode.AR_AP_WITH_APPROVED_PAYMENT);
}

@Test
void getFinanceAutoFlowModeRejectsUnknownValue() {
    SystemConfig config = new SystemConfig();
    config.setConfigValue("INVALID_MODE");
    when(systemConfigMapper.selectOne(any())).thenReturn(config);

    assertThatThrownBy(() -> service.getFinanceAutoFlowMode())
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("erp.finance.auto-flow.mode");
}
```

- [ ] **Step 3: Run the targeted test and verify it fails**

Run:

```cmd
mvnw.cmd -q -Dtest=TenantSettingFinanceAutoFlowModeTest test
```

Expected: FAIL because `FinanceAutoFlowMode` and the new service method do not exist yet.

- [ ] **Step 4: Add the migration and minimal config parsing implementation**

```sql
INSERT INTO app_system_config (
    config_key, config_value, value_type, description, is_public, created_at, updated_at, tenant_id
)
SELECT
    'erp.finance.auto-flow.mode',
    'AR_AP_WITH_APPROVED_PAYMENT',
    'string',
    '审核后财务自动联动模式',
    false,
    NOW(),
    NOW(),
    t.id
FROM tenant t
WHERE NOT EXISTS (
    SELECT 1
    FROM app_system_config c
    WHERE c.tenant_id = t.id
      AND c.config_key = 'erp.finance.auto-flow.mode'
);
```

```java
public FinanceAutoFlowMode getFinanceAutoFlowMode() {
    SystemConfig config = systemConfigMapper.selectOne(new QueryWrapper<SystemConfig>()
        .eq("tenant_id", TenantContext.requireTenantId())
        .eq("config_key", "erp.finance.auto-flow.mode"));
    if (config == null || config.getConfigValue() == null || config.getConfigValue().isBlank()) {
        return FinanceAutoFlowMode.AR_AP_WITH_APPROVED_PAYMENT;
    }
    return FinanceAutoFlowMode.fromValue(config.getConfigValue());
}
```

- [ ] **Step 5: Run the targeted test and verify it passes**

Run:

```cmd
mvnw.cmd -q -Dtest=TenantSettingFinanceAutoFlowModeTest test
```

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add wms-backend/src/main/resources/db/migration/V113__erp_finance_auto_flow_mode.sql wms-backend/src/main/java/com/example/wms/service/impl/TenantSettingServiceImpl.java wms-backend/src/test/java/com/example/wms/TenantSettingFinanceAutoFlowModeTest.java
git commit -m "feat: add finance auto flow mode config"
```

## Task 2: Build the shared orchestration and guard layer with tests first

**Files:**
- Create: `wms-backend/src/main/java/com/example/wms/service/erp/support/FinanceAutoFlowMode.java`
- Create: `wms-backend/src/main/java/com/example/wms/service/erp/support/AutoFlowDocumentType.java`
- Create: `wms-backend/src/main/java/com/example/wms/service/erp/support/AutoFlowCommand.java`
- Create: `wms-backend/src/main/java/com/example/wms/service/erp/support/AutoFlowBlockResult.java`
- Create: `wms-backend/src/main/java/com/example/wms/service/erp/support/ErpFinanceAutoFlowService.java`
- Create: `wms-backend/src/main/java/com/example/wms/service/erp/support/ErpFinanceAutoFlowGuardService.java`
- Test: `wms-backend/src/test/java/com/example/wms/ErpFinanceAutoFlowServiceTest.java`
- Test: `wms-backend/src/test/java/com/example/wms/ErpFinanceAutoFlowGuardServiceTest.java`

- [ ] **Step 1: Write the failing orchestration test for three modes**

```java
@Test
void saleOrderCommandCreatesReceivableOnlyInArApOnlyMode() {
    AutoFlowCommand command = AutoFlowCommand.saleOrder(1L, 99L, 8L, new BigDecimal("100"), new BigDecimal("30"));

    service.handle(command, FinanceAutoFlowMode.AR_AP_ONLY);

    verify(autoArApService).upsertFor(command);
    verify(autoReceiptPaymentService, never()).upsertFor(command, true);
    verify(autoReceiptPaymentService, never()).upsertFor(command, false);
}

@Test
void saleOrderCommandCreatesDraftReceiptInDraftMode() {
    AutoFlowCommand command = AutoFlowCommand.saleOrder(1L, 99L, 8L, new BigDecimal("100"), new BigDecimal("30"));

    service.handle(command, FinanceAutoFlowMode.AR_AP_WITH_DRAFT_PAYMENT);

    verify(autoArApService).upsertFor(command);
    verify(autoReceiptPaymentService).upsertFor(command, false);
}

@Test
void saleOrderCommandCreatesApprovedReceiptInApprovedMode() {
    AutoFlowCommand command = AutoFlowCommand.saleOrder(1L, 99L, 8L, new BigDecimal("100"), new BigDecimal("30"));

    service.handle(command, FinanceAutoFlowMode.AR_AP_WITH_APPROVED_PAYMENT);

    verify(autoArApService).upsertFor(command);
    verify(autoReceiptPaymentService).upsertFor(command, true);
}
```

- [ ] **Step 2: Write the failing guard test for manual takeover**

```java
@Test
void editedDraftAutoReceiptIsRestorableButNotSilentlyOverwriteable() {
    AutoFlowBlockResult result = guardService.blockForEditedDraft(
        "RECEIPT",
        12L,
        "RC202605240012",
        "已被人工修改金额和结算方式"
    );

    assertThat(result.canRestoreSystemDefault()).isTrue();
    assertThat(result.suggestedAction()).isEqualTo("RESET_TO_SYSTEM_MANAGED");
    assertThat(result.blockingReasonMessage()).contains("RC202605240012");
}

@Test
void approvedAutoPaymentRequiresRedFlushInsteadOfRestore() {
    AutoFlowBlockResult result = guardService.blockForApprovedPayment(
        "PAYMENT",
        18L,
        "FK202605240008"
    );

    assertThat(result.canRestoreSystemDefault()).isFalse();
    assertThat(result.suggestedAction()).isEqualTo("RED_FLUSH_DOCUMENT");
}
```

- [ ] **Step 3: Run the targeted tests and verify they fail**

Run:

```cmd
mvnw.cmd -q -Dtest=ErpFinanceAutoFlowServiceTest,ErpFinanceAutoFlowGuardServiceTest test
```

Expected: FAIL because the new support classes do not exist yet.

- [ ] **Step 4: Add the minimal support types and implementations**

```java
public enum FinanceAutoFlowMode {
    AR_AP_ONLY,
    AR_AP_WITH_DRAFT_PAYMENT,
    AR_AP_WITH_APPROVED_PAYMENT;

    public static FinanceAutoFlowMode fromValue(String value) {
        return FinanceAutoFlowMode.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
```

```java
public void handle(AutoFlowCommand command, FinanceAutoFlowMode mode) {
    autoArApService.upsertFor(command);
    if (mode == FinanceAutoFlowMode.AR_AP_ONLY || command.cashAmount().compareTo(BigDecimal.ZERO) <= 0) {
        return;
    }
    boolean approveImmediately = mode == FinanceAutoFlowMode.AR_AP_WITH_APPROVED_PAYMENT;
    autoReceiptPaymentService.upsertFor(command, approveImmediately);
}
```

```java
public AutoFlowBlockResult blockForEditedDraft(String type, Long id, String no, String detail) {
    return new AutoFlowBlockResult(type, id, no, "MANUAL_TAKEN_OVER_DRAFT", no + " " + detail, "RESET_TO_SYSTEM_MANAGED", true);
}
```

- [ ] **Step 5: Run the targeted tests and verify they pass**

Run:

```cmd
mvnw.cmd -q -Dtest=ErpFinanceAutoFlowServiceTest,ErpFinanceAutoFlowGuardServiceTest test
```

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add wms-backend/src/main/java/com/example/wms/service/erp/support wms-backend/src/test/java/com/example/wms/ErpFinanceAutoFlowServiceTest.java wms-backend/src/test/java/com/example/wms/ErpFinanceAutoFlowGuardServiceTest.java
git commit -m "feat: add finance auto flow orchestration layer"
```

## Task 3: Rewire the four audit flows to delegate finance auto-flow

**Files:**
- Modify: `wms-backend/src/main/java/com/example/wms/service/erp/impl/ErpSaleOrderServiceImpl.java`
- Modify: `wms-backend/src/main/java/com/example/wms/service/erp/impl/ErpPurchaseOrderServiceImpl.java`
- Modify: `wms-backend/src/main/java/com/example/wms/service/erp/impl/ErpSaleReturnServiceImpl.java`
- Modify: `wms-backend/src/main/java/com/example/wms/service/erp/impl/ErpPurchaseReturnServiceImpl.java`
- Test: `wms-backend/src/test/java/com/example/wms/AuthPermissionIntegrationTests.java`

- [ ] **Step 1: Write the failing audit delegation test**

```java
@Test
void saleOrderApproveDelegatesFinanceAutoFlowAfterStockMutation() {
    ErpSaleOrderServiceImpl service = buildService();
    when(financeAutoFlowService.currentMode()).thenReturn(FinanceAutoFlowMode.AR_AP_WITH_APPROVED_PAYMENT);

    service.approve(9L);

    verify(financeAutoFlowService).handle(argThat(command ->
        command.documentType() == AutoFlowDocumentType.SALE_ORDER
            && command.sourceDocumentId().equals(9L)
    ), eq(FinanceAutoFlowMode.AR_AP_WITH_APPROVED_PAYMENT));
}
```

- [ ] **Step 2: Run the targeted test and verify it fails**

Run:

```cmd
mvnw.cmd -q -Dtest=AuthPermissionIntegrationTests#saleOrderApproveDelegatesFinanceAutoFlowAfterStockMutation test
```

Expected: FAIL because the source service still calls document-specific receipt/payment logic directly.

- [ ] **Step 3: Replace inline finance generation with command delegation**

```java
FinanceAutoFlowMode mode = financeAutoFlowService.currentMode();
financeAutoFlowService.handle(
    AutoFlowCommand.saleOrder(
        tenantId,
        order.getId(),
        order.getCustomerId(),
        approved.getTotalAmountInclTax(),
        approved.getPaidAmount()
    ),
    mode
);
```

Apply the same pattern for:

- purchase orders
- sale returns
- purchase returns

while keeping stock deltas and source-document audit behavior in each service.

- [ ] **Step 4: Run focused tests for the four audit entry points**

Run:

```cmd
mvnw.cmd -q -Dtest=AuthPermissionIntegrationTests test
```

Expected: PASS for the new delegation assertion and existing auth/audit assertions that compile in the current branch.

- [ ] **Step 5: Commit**

```bash
git add wms-backend/src/main/java/com/example/wms/service/erp/impl/ErpSaleOrderServiceImpl.java wms-backend/src/main/java/com/example/wms/service/erp/impl/ErpPurchaseOrderServiceImpl.java wms-backend/src/main/java/com/example/wms/service/erp/impl/ErpSaleReturnServiceImpl.java wms-backend/src/main/java/com/example/wms/service/erp/impl/ErpPurchaseReturnServiceImpl.java wms-backend/src/test/java/com/example/wms/AuthPermissionIntegrationTests.java
git commit -m "refactor: delegate finance auto flow after document approval"
```

## Task 4: Enforce manual-takeover blocking and structured front-end messaging

**Files:**
- Modify: `wms-backend/src/main/java/com/example/wms/service/erp/impl/ErpReceiptServiceImpl.java`
- Modify: `wms-backend/src/main/java/com/example/wms/service/erp/impl/ErpPaymentServiceImpl.java`
- Modify: `auto-parts-wms-vue/src/views/system/TenantSettingManagement.vue`
- Modify: `auto-parts-wms-vue/src/locales/zh.ts`
- Modify: `auto-parts-wms-vue/src/locales/en.ts`

- [ ] **Step 1: Write the failing guard integration test for user-facing messages**

```java
@Test
void approvedManualReceiptReturnsStructuredBlockResult() {
    AutoFlowBlockResult result = receiptService.buildManualTakeoverBlock("RC202605240012", "APPROVED");

    assertThat(result.blockingReasonMessage()).contains("请先红冲该收款单");
    assertThat(result.suggestedAction()).isEqualTo("RED_FLUSH_DOCUMENT");
    assertThat(result.canRestoreSystemDefault()).isFalse();
}
```

- [ ] **Step 2: Run the targeted test and verify it fails**

Run:

```cmd
mvnw.cmd -q -Dtest=ErpFinanceAutoFlowGuardServiceTest test
```

Expected: FAIL because the services still throw free-form strings or do not expose the new structured block result.

- [ ] **Step 3: Implement structured blocking and the system-config selector**

```java
if (manualTakenOver) {
    throw new FinanceAutoFlowBlockedException(
        guardService.blockForApprovedPayment("RECEIPT", receipt.getId(), receipt.getReceiptNo())
    );
}
```

```ts
const financeAutoFlowOptions = [
  { value: 'AR_AP_ONLY', label: t('systemConfig.financeAutoFlowArApOnly') },
  { value: 'AR_AP_WITH_DRAFT_PAYMENT', label: t('systemConfig.financeAutoFlowDraftPayment') },
  { value: 'AR_AP_WITH_APPROVED_PAYMENT', label: t('systemConfig.financeAutoFlowApprovedPayment') },
]
```

- [ ] **Step 4: Run backend and frontend verification**

Run:

```cmd
mvnw.cmd -q -Dmaven.test.skip=true compile
```

```powershell
& 'C:\Users\Administrator\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin\node.exe' .\node_modules\vue-tsc\bin\vue-tsc.js --noEmit
```

Expected: both commands succeed.

- [ ] **Step 5: Commit**

```bash
git add wms-backend/src/main/java/com/example/wms/service/erp/impl/ErpReceiptServiceImpl.java wms-backend/src/main/java/com/example/wms/service/erp/impl/ErpPaymentServiceImpl.java auto-parts-wms-vue/src/views/system/TenantSettingManagement.vue auto-parts-wms-vue/src/locales/zh.ts auto-parts-wms-vue/src/locales/en.ts
git commit -m "feat: add manual takeover blocking for finance auto flow"
```

## Task 5: Phase 2 restore-system-default support

**Files:**
- Modify: `wms-backend/src/main/java/com/example/wms/service/erp/impl/ErpReceiptServiceImpl.java`
- Modify: `wms-backend/src/main/java/com/example/wms/service/erp/impl/ErpPaymentServiceImpl.java`
- Modify: `auto-parts-wms-vue/src/views/system/TenantSettingManagement.vue`
- Test: `wms-backend/src/test/java/com/example/wms/ErpFinanceAutoFlowGuardServiceTest.java`

- [ ] **Step 1: Write the failing restore eligibility test**

```java
@Test
void restoreSystemDefaultIsAllowedOnlyForEditableAutoDraftReceipt() {
    assertThat(guardService.canRestoreSystemDefault(true, "DRAFT", false, false, true)).isTrue();
    assertThat(guardService.canRestoreSystemDefault(true, "APPROVED", false, false, true)).isFalse();
    assertThat(guardService.canRestoreSystemDefault(true, "DRAFT", true, false, true)).isFalse();
}
```

- [ ] **Step 2: Run the targeted test and verify it fails**

Run:

```cmd
mvnw.cmd -q -Dtest=ErpFinanceAutoFlowGuardServiceTest#restoreSystemDefaultIsAllowedOnlyForEditableAutoDraftReceipt test
```

Expected: FAIL because restore eligibility helpers do not exist yet.

- [ ] **Step 3: Implement the restore action and route it through the guard**

```java
public void restoreSystemDefaultForReceipt(Long receiptId) {
    ErpReceipt receipt = loadAutoManagedDraftReceipt(receiptId);
    if (!guardService.canRestoreSystemDefault(true, receipt.getStatus(), hasAllocation(receipt), wasRedFlushed(receipt), hasSourceLink(receipt))) {
        throw new FinanceAutoFlowBlockedException(
            guardService.blockForManualOnly("RECEIPT", receiptId, receipt.getReceiptNo())
        );
    }
    rewriteReceiptFromSource(receipt);
}
```

- [ ] **Step 4: Run focused tests and compile**

Run:

```cmd
mvnw.cmd -q -Dtest=ErpFinanceAutoFlowGuardServiceTest test
```

```cmd
mvnw.cmd -q -Dmaven.test.skip=true compile
```

Expected: tests pass and backend compile succeeds.

- [ ] **Step 5: Commit**

```bash
git add wms-backend/src/main/java/com/example/wms/service/erp/impl/ErpReceiptServiceImpl.java wms-backend/src/main/java/com/example/wms/service/erp/impl/ErpPaymentServiceImpl.java wms-backend/src/test/java/com/example/wms/ErpFinanceAutoFlowGuardServiceTest.java
git commit -m "feat: add restore system default for auto-managed draft finance docs"
```

## Self-Review

- Spec coverage: this plan covers migration/config support, the shared orchestration and guard layer, delegation from four audit flows, structured blocking messages, and the phase-2 restore-system-default capability.
- Placeholder scan: no `TODO` or “similar to task N” placeholders remain; every task has explicit files, commands, and expected outcomes.
- Type consistency: the shared names used across tasks are `FinanceAutoFlowMode`, `AutoFlowCommand`, `AutoFlowBlockResult`, `ErpFinanceAutoFlowService`, and `ErpFinanceAutoFlowGuardService`; later tasks reuse the same names without drift.

User instruction override:

- The normal brainstorming review gate is intentionally skipped because the user explicitly requested “完成设计文档，后直接进入实现计划，不需要再次审阅”.
