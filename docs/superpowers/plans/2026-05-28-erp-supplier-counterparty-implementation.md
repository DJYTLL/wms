# ERP 供应商扩展与往来主体 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为现有 ERP 系统增加供应商类型、供应商扩展导入字段、往来主体归并能力，以及主体维度财务汇总查询能力，并为后续导入功能打通数据承接链路。

**Architecture:** 保留现有客户、供应商、应收、应付主链路不变，在供应商模型上扩展历史字段，并新增“供应商类型”和“往来主体/主体关联”模型作为主数据补充层。财务层继续按客户/供应商分别记账，同时新增主体维度汇总接口和页面。

**Tech Stack:** Spring Boot、MyBatis-Plus、Flyway、PostgreSQL、Vue 3、TypeScript、Element Plus

---

## 文件结构

### 后端数据库与实体

- Create: `wms-backend/src/main/resources/db/migration/V118__erp_supplier_type.sql`
- Create: `wms-backend/src/main/resources/db/migration/V119__erp_supplier_extend_import_fields.sql`
- Create: `wms-backend/src/main/resources/db/migration/V120__erp_counterparty_subject.sql`
- Create: `wms-backend/src/main/resources/db/migration/V121__erp_counterparty_subject_finance_view.sql`
- Create: `wms-backend/src/main/java/com/example/wms/entity/erp/ErpSupplierType.java`
- Create: `wms-backend/src/main/java/com/example/wms/entity/erp/ErpCounterpartySubject.java`
- Create: `wms-backend/src/main/java/com/example/wms/entity/erp/ErpCounterpartySubjectLink.java`
- Modify: `wms-backend/src/main/java/com/example/wms/entity/erp/ErpSupplier.java`

### 后端 DTO / Mapper / Service / Controller

- Create: `wms-backend/src/main/java/com/example/wms/dto/erp/ErpSupplierTypeCreateRequest.java`
- Create: `wms-backend/src/main/java/com/example/wms/dto/erp/ErpSupplierTypeUpdateRequest.java`
- Create: `wms-backend/src/main/java/com/example/wms/dto/erp/ErpCounterpartySubjectCreateRequest.java`
- Create: `wms-backend/src/main/java/com/example/wms/dto/erp/ErpCounterpartySubjectUpdateRequest.java`
- Create: `wms-backend/src/main/java/com/example/wms/dto/erp/ErpCounterpartyFinanceSummaryView.java`
- Modify: `wms-backend/src/main/java/com/example/wms/dto/erp/ErpSupplierCreateRequest.java`
- Modify: `wms-backend/src/main/java/com/example/wms/dto/erp/ErpSupplierUpdateRequest.java`
- Create: `wms-backend/src/main/java/com/example/wms/mapper/erp/ErpSupplierTypeMapper.java`
- Create: `wms-backend/src/main/java/com/example/wms/mapper/erp/ErpCounterpartySubjectMapper.java`
- Create: `wms-backend/src/main/java/com/example/wms/mapper/erp/ErpCounterpartySubjectLinkMapper.java`
- Modify: `wms-backend/src/main/java/com/example/wms/mapper/erp/ErpSupplierMapper.java`
- Create: `wms-backend/src/main/java/com/example/wms/service/erp/ErpSupplierTypeService.java`
- Create: `wms-backend/src/main/java/com/example/wms/service/erp/ErpCounterpartySubjectService.java`
- Create: `wms-backend/src/main/java/com/example/wms/service/erp/impl/ErpSupplierTypeServiceImpl.java`
- Create: `wms-backend/src/main/java/com/example/wms/service/erp/impl/ErpCounterpartySubjectServiceImpl.java`
- Modify: `wms-backend/src/main/java/com/example/wms/service/erp/impl/ErpSupplierServiceImpl.java`
- Create: `wms-backend/src/main/java/com/example/wms/controller/erp/ErpSupplierTypeController.java`
- Create: `wms-backend/src/main/java/com/example/wms/controller/erp/ErpCounterpartySubjectController.java`
- Modify: `wms-backend/src/main/java/com/example/wms/controller/erp/ErpSupplierController.java`
- Modify: `wms-backend/src/main/java/com/example/wms/controller/erp/ErpFinanceController.java`

### 前端页面与路由

- Create: `auto-parts-wms-vue/src/views/erp/ErpSupplierTypeManagement.vue`
- Create: `auto-parts-wms-vue/src/views/erp/ErpCounterpartySubjectManagement.vue`
- Create: `auto-parts-wms-vue/src/views/erp/ErpCounterpartyFinanceSummary.vue`
- Modify: `auto-parts-wms-vue/src/views/erp/ErpSupplierManagement.vue`
- Modify: `auto-parts-wms-vue/src/router/index.ts`
- Modify: `auto-parts-wms-vue/src/locales/zh.ts`
- Modify: `auto-parts-wms-vue/src/locales/en.ts`
- Modify: `auto-parts-wms-vue/src/views/system/ColumnPermissionManagement.vue`
- Modify: `auto-parts-wms-vue/src/views/system/RoleManagement.vue`

### 测试

- Modify: `wms-backend/src/test/java/com/example/wms/ErpMasterDataGuardTests.java`
- Create: `wms-backend/src/test/java/com/example/wms/ErpSupplierCounterpartyTests.java`
- Create: `auto-parts-wms-vue/src/router/__tests__/erpCounterpartyRouteHygiene.test.mjs`

## Task 1: 落库供应商类型表

**Files:**
- Create: `wms-backend/src/main/resources/db/migration/V118__erp_supplier_type.sql`
- Create: `wms-backend/src/main/java/com/example/wms/entity/erp/ErpSupplierType.java`
- Create: `wms-backend/src/main/java/com/example/wms/mapper/erp/ErpSupplierTypeMapper.java`

- [ ] **Step 1: 写 Flyway 迁移脚本**

```sql
CREATE TABLE erp_supplier_type (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(128) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    sort INTEGER NOT NULL DEFAULT 0,
    remark TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_by VARCHAR(64),
    delete_reason VARCHAR(255),
    deleted_at TIMESTAMPTZ
);

CREATE UNIQUE INDEX uk_erp_supplier_type_code
    ON erp_supplier_type (tenant_id, code, deleted_at);

CREATE UNIQUE INDEX uk_erp_supplier_type_name
    ON erp_supplier_type (tenant_id, name, deleted_at);

CREATE INDEX idx_erp_supplier_type_tenant_deleted_at
    ON erp_supplier_type (tenant_id, deleted_at);
```

- [ ] **Step 2: 新增实体类**

```java
@TableName("erp_supplier_type")
public class ErpSupplierType {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @TableField("tenant_id")
    private Long tenantId;
    @TableField("code")
    private String code;
    @TableField("name")
    private String name;
    @TableField("enabled")
    private Boolean enabled;
    @TableField("sort")
    private Integer sort;
    @TableField("remark")
    private String remark;
    @TableField("created_at")
    private Instant createdAt;
    @TableField("updated_at")
    private Instant updatedAt;
    @TableField("deleted_by")
    private String deletedBy;
    @TableField("delete_reason")
    private String deleteReason;
    @TableLogic(value = "null", delval = "now()")
    @TableField("deleted_at")
    private Instant deletedAt;
}
```

- [ ] **Step 3: 新增 Mapper**

```java
@Mapper
public interface ErpSupplierTypeMapper extends BaseMapper<ErpSupplierType> {
}
```

- [ ] **Step 4: 运行后端编译验证迁移与实体无语法错误**

Run: `mvn -pl wms-backend -DskipTests compile`

Expected: `BUILD SUCCESS`

- [ ] **Step 5: 提交**

```bash
git add wms-backend/src/main/resources/db/migration/V118__erp_supplier_type.sql wms-backend/src/main/java/com/example/wms/entity/erp/ErpSupplierType.java wms-backend/src/main/java/com/example/wms/mapper/erp/ErpSupplierTypeMapper.java
git commit -m "feat: add supplier type schema"
```

## Task 2: 扩展供应商表承接导入字段

**Files:**
- Create: `wms-backend/src/main/resources/db/migration/V119__erp_supplier_extend_import_fields.sql`
- Modify: `wms-backend/src/main/java/com/example/wms/entity/erp/ErpSupplier.java`
- Modify: `wms-backend/src/main/java/com/example/wms/dto/erp/ErpSupplierCreateRequest.java`
- Modify: `wms-backend/src/main/java/com/example/wms/dto/erp/ErpSupplierUpdateRequest.java`

- [ ] **Step 1: 写供应商扩展字段迁移**

```sql
ALTER TABLE erp_supplier
    ADD COLUMN supplier_type_id BIGINT,
    ADD COLUMN region VARCHAR(128),
    ADD COLUMN wechat VARCHAR(128),
    ADD COLUMN purchaser VARCHAR(128),
    ADD COLUMN contact_info VARCHAR(255),
    ADD COLUMN source_created_at TIMESTAMPTZ,
    ADD COLUMN source_created_by VARCHAR(64),
    ADD COLUMN business_scope VARCHAR(32) NOT NULL DEFAULT 'SUPPLIER',
    ADD COLUMN counterparty_subject_id BIGINT;

CREATE INDEX idx_erp_supplier_supplier_type ON erp_supplier (tenant_id, supplier_type_id);
CREATE INDEX idx_erp_supplier_counterparty_subject ON erp_supplier (tenant_id, counterparty_subject_id);
CREATE INDEX idx_erp_supplier_business_scope ON erp_supplier (tenant_id, business_scope);
```

- [ ] **Step 2: 扩展供应商实体**

```java
@TableField("supplier_type_id")
private Long supplierTypeId;
@TableField("region")
private String region;
@TableField("wechat")
private String wechat;
@TableField("purchaser")
private String purchaser;
@TableField("contact_info")
private String contactInfo;
@TableField("source_created_at")
private Instant sourceCreatedAt;
@TableField("source_created_by")
private String sourceCreatedBy;
@TableField("business_scope")
private String businessScope;
@TableField("counterparty_subject_id")
private Long counterpartySubjectId;
```

- [ ] **Step 3: 扩展新增/更新 DTO**

```java
Long supplierTypeId,
String region,
String wechat,
String purchaser,
String contactInfo,
Instant sourceCreatedAt,
String sourceCreatedBy,
String businessScope,
Long counterpartySubjectId,
```

- [ ] **Step 4: 编译验证 DTO 与实体同步**

Run: `mvn -pl wms-backend -DskipTests compile`

Expected: `BUILD SUCCESS`

- [ ] **Step 5: 提交**

```bash
git add wms-backend/src/main/resources/db/migration/V119__erp_supplier_extend_import_fields.sql wms-backend/src/main/java/com/example/wms/entity/erp/ErpSupplier.java wms-backend/src/main/java/com/example/wms/dto/erp/ErpSupplierCreateRequest.java wms-backend/src/main/java/com/example/wms/dto/erp/ErpSupplierUpdateRequest.java
git commit -m "feat: extend supplier import fields"
```

## Task 3: 落库往来主体与关联表

**Files:**
- Create: `wms-backend/src/main/resources/db/migration/V120__erp_counterparty_subject.sql`
- Create: `wms-backend/src/main/java/com/example/wms/entity/erp/ErpCounterpartySubject.java`
- Create: `wms-backend/src/main/java/com/example/wms/entity/erp/ErpCounterpartySubjectLink.java`
- Create: `wms-backend/src/main/java/com/example/wms/mapper/erp/ErpCounterpartySubjectMapper.java`
- Create: `wms-backend/src/main/java/com/example/wms/mapper/erp/ErpCounterpartySubjectLinkMapper.java`

- [ ] **Step 1: 写主体与关联表迁移**

```sql
CREATE TABLE erp_counterparty_subject (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    name VARCHAR(128) NOT NULL,
    region VARCHAR(128),
    unified_credit_code VARCHAR(64),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    remark TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_by VARCHAR(64),
    delete_reason VARCHAR(255),
    deleted_at TIMESTAMPTZ
);

CREATE TABLE erp_counterparty_subject_link (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    subject_id BIGINT NOT NULL,
    target_type VARCHAR(32) NOT NULL,
    target_id BIGINT NOT NULL,
    role_type VARCHAR(32) NOT NULL,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    remark VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_counterparty_subject_tenant_deleted
    ON erp_counterparty_subject (tenant_id, deleted_at);
CREATE INDEX idx_counterparty_subject_link_subject
    ON erp_counterparty_subject_link (tenant_id, subject_id);
CREATE UNIQUE INDEX uk_counterparty_subject_link_target
    ON erp_counterparty_subject_link (tenant_id, target_type, target_id, role_type);
```

- [ ] **Step 2: 新增主体实体与关联实体**

```java
@TableName("erp_counterparty_subject")
public class ErpCounterpartySubject { ... }

@TableName("erp_counterparty_subject_link")
public class ErpCounterpartySubjectLink { ... }
```

- [ ] **Step 3: 新增两个 Mapper**

```java
@Mapper
public interface ErpCounterpartySubjectMapper extends BaseMapper<ErpCounterpartySubject> {
}

@Mapper
public interface ErpCounterpartySubjectLinkMapper extends BaseMapper<ErpCounterpartySubjectLink> {
}
```

- [ ] **Step 4: 编译验证**

Run: `mvn -pl wms-backend -DskipTests compile`

Expected: `BUILD SUCCESS`

- [ ] **Step 5: 提交**

```bash
git add wms-backend/src/main/resources/db/migration/V120__erp_counterparty_subject.sql wms-backend/src/main/java/com/example/wms/entity/erp/ErpCounterpartySubject.java wms-backend/src/main/java/com/example/wms/entity/erp/ErpCounterpartySubjectLink.java wms-backend/src/main/java/com/example/wms/mapper/erp/ErpCounterpartySubjectMapper.java wms-backend/src/main/java/com/example/wms/mapper/erp/ErpCounterpartySubjectLinkMapper.java
git commit -m "feat: add counterparty subject schema"
```

## Task 4: 实现供应商类型后端接口

**Files:**
- Create: `wms-backend/src/main/java/com/example/wms/dto/erp/ErpSupplierTypeCreateRequest.java`
- Create: `wms-backend/src/main/java/com/example/wms/dto/erp/ErpSupplierTypeUpdateRequest.java`
- Create: `wms-backend/src/main/java/com/example/wms/service/erp/ErpSupplierTypeService.java`
- Create: `wms-backend/src/main/java/com/example/wms/service/erp/impl/ErpSupplierTypeServiceImpl.java`
- Create: `wms-backend/src/main/java/com/example/wms/controller/erp/ErpSupplierTypeController.java`
- Modify: `wms-backend/src/test/java/com/example/wms/ErpMasterDataGuardTests.java`

- [ ] **Step 1: 先写服务与控制器测试**

```java
@Test
void supplierTypeCrudShouldWork() {
    // create -> list -> update -> disable -> delete guard
    assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
}
```

- [ ] **Step 2: 运行单测确认失败**

Run: `mvn -pl wms-backend -Dtest=ErpMasterDataGuardTests test`

Expected: FAIL with missing controller/service or endpoint

- [ ] **Step 3: 实现 DTO / Service / Controller**

```java
public interface ErpSupplierTypeService {
    List<ErpSupplierType> listAll(String keyword, Boolean enabled);
    ErpSupplierType create(ErpSupplierTypeCreateRequest request);
    ErpSupplierType update(Long id, ErpSupplierTypeUpdateRequest request);
    void delete(Long id);
}
```

```java
@RestController
@RequestMapping("/api/erp/supplier-types")
public class ErpSupplierTypeController {
    @GetMapping
    public ApiResponse<List<ErpSupplierType>> list(...) { ... }
    @PostMapping
    public ApiResponse<ErpSupplierType> create(...) { ... }
    @PutMapping("/{id}")
    public ApiResponse<ErpSupplierType> update(...) { ... }
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(...) { ... }
}
```

- [ ] **Step 4: 运行单测确认通过**

Run: `mvn -pl wms-backend -Dtest=ErpMasterDataGuardTests test`

Expected: PASS

- [ ] **Step 5: 提交**

```bash
git add wms-backend/src/main/java/com/example/wms/dto/erp/ErpSupplierTypeCreateRequest.java wms-backend/src/main/java/com/example/wms/dto/erp/ErpSupplierTypeUpdateRequest.java wms-backend/src/main/java/com/example/wms/service/erp/ErpSupplierTypeService.java wms-backend/src/main/java/com/example/wms/service/erp/impl/ErpSupplierTypeServiceImpl.java wms-backend/src/main/java/com/example/wms/controller/erp/ErpSupplierTypeController.java wms-backend/src/test/java/com/example/wms/ErpMasterDataGuardTests.java
git commit -m "feat: add supplier type endpoints"
```

## Task 5: 扩展供应商服务与接口

**Files:**
- Modify: `wms-backend/src/main/java/com/example/wms/service/erp/impl/ErpSupplierServiceImpl.java`
- Modify: `wms-backend/src/main/java/com/example/wms/controller/erp/ErpSupplierController.java`
- Modify: `wms-backend/src/main/java/com/example/wms/mapper/erp/ErpSupplierMapper.java`
- Create: `wms-backend/src/test/java/com/example/wms/ErpSupplierCounterpartyTests.java`

- [ ] **Step 1: 先写供应商扩展字段保存与查询测试**

```java
@Test
void supplierShouldPersistExtendedFields() {
    // create supplier with supplierTypeId, region, wechat, purchaser, contactInfo, businessScope
    // assert returned payload and database row both contain expected values
}
```

- [ ] **Step 2: 运行单测确认失败**

Run: `mvn -pl wms-backend -Dtest=ErpSupplierCounterpartyTests test`

Expected: FAIL with missing fields mapping or JSON mismatch

- [ ] **Step 3: 在服务层补充字段赋值与校验**

```java
supplier.setSupplierTypeId(request.supplierTypeId());
supplier.setRegion(request.region());
supplier.setWechat(request.wechat());
supplier.setPurchaser(request.purchaser());
supplier.setContactInfo(request.contactInfo());
supplier.setSourceCreatedAt(request.sourceCreatedAt());
supplier.setSourceCreatedBy(request.sourceCreatedBy());
supplier.setBusinessScope(
    request.businessScope() == null || request.businessScope().isBlank()
        ? "SUPPLIER"
        : request.businessScope().trim()
);
supplier.setCounterpartySubjectId(request.counterpartySubjectId());
```

- [ ] **Step 4: 运行单测确认通过**

Run: `mvn -pl wms-backend -Dtest=ErpSupplierCounterpartyTests test`

Expected: PASS

- [ ] **Step 5: 提交**

```bash
git add wms-backend/src/main/java/com/example/wms/service/erp/impl/ErpSupplierServiceImpl.java wms-backend/src/main/java/com/example/wms/controller/erp/ErpSupplierController.java wms-backend/src/main/java/com/example/wms/mapper/erp/ErpSupplierMapper.java wms-backend/src/test/java/com/example/wms/ErpSupplierCounterpartyTests.java
git commit -m "feat: support supplier counterparty fields"
```

## Task 6: 实现往来主体后端接口

**Files:**
- Create: `wms-backend/src/main/java/com/example/wms/dto/erp/ErpCounterpartySubjectCreateRequest.java`
- Create: `wms-backend/src/main/java/com/example/wms/dto/erp/ErpCounterpartySubjectUpdateRequest.java`
- Create: `wms-backend/src/main/java/com/example/wms/service/erp/ErpCounterpartySubjectService.java`
- Create: `wms-backend/src/main/java/com/example/wms/service/erp/impl/ErpCounterpartySubjectServiceImpl.java`
- Create: `wms-backend/src/main/java/com/example/wms/controller/erp/ErpCounterpartySubjectController.java`
- Modify: `wms-backend/src/test/java/com/example/wms/ErpSupplierCounterpartyTests.java`

- [ ] **Step 1: 先写主体与主体关联测试**

```java
@Test
void counterpartySubjectShouldLinkMultipleCustomersAndSuppliers() {
    // create subject
    // link supplier A, supplier B, customer A
    // assert query returns 2 supplier links and 1 customer link
}
```

- [ ] **Step 2: 运行单测确认失败**

Run: `mvn -pl wms-backend -Dtest=ErpSupplierCounterpartyTests test`

Expected: FAIL with missing subject endpoints or service

- [ ] **Step 3: 实现主体服务和控制器**

```java
public interface ErpCounterpartySubjectService {
    List<ErpCounterpartySubject> listAll(String keyword, Boolean enabled);
    ErpCounterpartySubject create(ErpCounterpartySubjectCreateRequest request);
    ErpCounterpartySubject update(Long id, ErpCounterpartySubjectUpdateRequest request);
    void bindSupplier(Long subjectId, Long supplierId, boolean primary);
    void bindCustomer(Long subjectId, Long customerId, boolean primary);
}
```

- [ ] **Step 4: 运行单测确认通过**

Run: `mvn -pl wms-backend -Dtest=ErpSupplierCounterpartyTests test`

Expected: PASS

- [ ] **Step 5: 提交**

```bash
git add wms-backend/src/main/java/com/example/wms/dto/erp/ErpCounterpartySubjectCreateRequest.java wms-backend/src/main/java/com/example/wms/dto/erp/ErpCounterpartySubjectUpdateRequest.java wms-backend/src/main/java/com/example/wms/service/erp/ErpCounterpartySubjectService.java wms-backend/src/main/java/com/example/wms/service/erp/impl/ErpCounterpartySubjectServiceImpl.java wms-backend/src/main/java/com/example/wms/controller/erp/ErpCounterpartySubjectController.java wms-backend/src/test/java/com/example/wms/ErpSupplierCounterpartyTests.java
git commit -m "feat: add counterparty subject endpoints"
```

## Task 7: 实现主体财务汇总后端接口

**Files:**
- Create: `wms-backend/src/main/resources/db/migration/V121__erp_counterparty_subject_finance_view.sql`
- Create: `wms-backend/src/main/java/com/example/wms/dto/erp/ErpCounterpartyFinanceSummaryView.java`
- Modify: `wms-backend/src/main/java/com/example/wms/controller/erp/ErpFinanceController.java`
- Modify: `wms-backend/src/main/java/com/example/wms/service/erp/impl/ErpFinanceServiceImpl.java`
- Modify: `wms-backend/src/main/java/com/example/wms/service/erp/ErpFinanceService.java`
- Modify: `wms-backend/src/test/java/com/example/wms/ErpFinanceWorkflowTests.java`

- [ ] **Step 1: 写主体汇总视图迁移**

```sql
CREATE VIEW erp_counterparty_subject_finance_v AS
SELECT
    s.id AS subject_id,
    s.tenant_id,
    s.name AS subject_name,
    COALESCE(ar.total_ar, 0) AS receivable_total,
    COALESCE(ap.total_ap, 0) AS payable_total,
    COALESCE(ar.total_ar, 0) - COALESCE(ap.total_ap, 0) AS net_amount
FROM erp_counterparty_subject s
LEFT JOIN (...) ar ON ar.subject_id = s.id
LEFT JOIN (...) ap ON ap.subject_id = s.id
WHERE s.deleted_at IS NULL;
```

- [ ] **Step 2: 先写财务汇总接口测试**

```java
@Test
void financeSummaryShouldAggregateByCounterpartySubject() {
    // create customer/supplier links and receivable/payable data
    // assert subject totals, counts and net amount
}
```

- [ ] **Step 3: 运行单测确认失败**

Run: `mvn -pl wms-backend -Dtest=ErpFinanceWorkflowTests test`

Expected: FAIL with missing DTO or endpoint

- [ ] **Step 4: 实现接口**

```java
@GetMapping("/counterparty-subjects/summary")
public ApiResponse<List<ErpCounterpartyFinanceSummaryView>> counterpartySummary() {
    return ApiResponse.success(erpFinanceService.getCounterpartySummary());
}
```

- [ ] **Step 5: 运行单测确认通过**

Run: `mvn -pl wms-backend -Dtest=ErpFinanceWorkflowTests test`

Expected: PASS

- [ ] **Step 6: 提交**

```bash
git add wms-backend/src/main/resources/db/migration/V121__erp_counterparty_subject_finance_view.sql wms-backend/src/main/java/com/example/wms/dto/erp/ErpCounterpartyFinanceSummaryView.java wms-backend/src/main/java/com/example/wms/controller/erp/ErpFinanceController.java wms-backend/src/main/java/com/example/wms/service/erp/ErpFinanceService.java wms-backend/src/main/java/com/example/wms/service/erp/impl/ErpFinanceServiceImpl.java wms-backend/src/test/java/com/example/wms/ErpFinanceWorkflowTests.java
git commit -m "feat: add counterparty finance summary"
```

## Task 8: 新增供应商类型前端页面

**Files:**
- Create: `auto-parts-wms-vue/src/views/erp/ErpSupplierTypeManagement.vue`
- Modify: `auto-parts-wms-vue/src/router/index.ts`
- Modify: `auto-parts-wms-vue/src/locales/zh.ts`
- Modify: `auto-parts-wms-vue/src/locales/en.ts`

- [ ] **Step 1: 先写路由与文案测试**

```js
test('erp counterparty routes should include supplier type management', () => {
  const routerSource = readSource('router/index.ts');
  assert.match(routerSource, /path: 'supplier-types'/);
  assert.match(routerSource, /ErpSupplierTypeManagement\.vue/);
});
```

- [ ] **Step 2: 运行前端测试确认失败**

Run: `node --test auto-parts-wms-vue/src/router/__tests__/erpCounterpartyRouteHygiene.test.mjs`

Expected: FAIL with route not found

- [ ] **Step 3: 实现页面与路由**

```ts
{
  path: 'supplier-types',
  name: 'ErpSupplierTypeManagement',
  component: () => import('../views/erp/ErpSupplierTypeManagement.vue'),
  meta: { title: '供应商类型', permission: 'erp-supplier-type:view' }
}
```

- [ ] **Step 4: 运行前端测试确认通过**

Run: `node --test auto-parts-wms-vue/src/router/__tests__/erpCounterpartyRouteHygiene.test.mjs`

Expected: PASS

- [ ] **Step 5: 提交**

```bash
git add auto-parts-wms-vue/src/views/erp/ErpSupplierTypeManagement.vue auto-parts-wms-vue/src/router/index.ts auto-parts-wms-vue/src/locales/zh.ts auto-parts-wms-vue/src/locales/en.ts auto-parts-wms-vue/src/router/__tests__/erpCounterpartyRouteHygiene.test.mjs
git commit -m "feat: add supplier type management page"
```

## Task 9: 改造供应商前端页面

**Files:**
- Modify: `auto-parts-wms-vue/src/views/erp/ErpSupplierManagement.vue`
- Modify: `auto-parts-wms-vue/src/views/system/ColumnPermissionManagement.vue`
- Modify: `auto-parts-wms-vue/src/views/system/RoleManagement.vue`

- [ ] **Step 1: 先写页面字段可见性测试**

```js
test('supplier management should expose counterparty fields', () => {
  const source = readSource('views/erp/ErpSupplierManagement.vue');
  assert.match(source, /supplierTypeId/);
  assert.match(source, /counterpartySubjectId/);
  assert.match(source, /businessScope/);
  assert.match(source, /wechat/);
  assert.match(source, /purchaser/);
});
```

- [ ] **Step 2: 运行前端测试确认失败**

Run: `node --test auto-parts-wms-vue/src/router/__tests__/erpCounterpartyRouteHygiene.test.mjs`

Expected: FAIL with missing form fields

- [ ] **Step 3: 实现表单、列表列、搜索条件**

```ts
const formData = reactive({
  ...,
  supplierTypeId: undefined as number | undefined,
  region: '',
  wechat: '',
  purchaser: '',
  contactInfo: '',
  businessScope: 'SUPPLIER',
  counterpartySubjectId: undefined as number | undefined
});
```

```vue
<el-form-item label="供应商类型">
  <el-select v-model="formData.supplierTypeId" clearable>...</el-select>
</el-form-item>
<el-form-item label="往来主体">
  <el-select v-model="formData.counterpartySubjectId" clearable filterable>...</el-select>
</el-form-item>
```

- [ ] **Step 4: 运行前端测试确认通过**

Run: `node --test auto-parts-wms-vue/src/router/__tests__/erpCounterpartyRouteHygiene.test.mjs`

Expected: PASS

- [ ] **Step 5: 提交**

```bash
git add auto-parts-wms-vue/src/views/erp/ErpSupplierManagement.vue auto-parts-wms-vue/src/views/system/ColumnPermissionManagement.vue auto-parts-wms-vue/src/views/system/RoleManagement.vue auto-parts-wms-vue/src/router/__tests__/erpCounterpartyRouteHygiene.test.mjs
git commit -m "feat: extend supplier management ui"
```

## Task 10: 新增往来主体页面与财务汇总页面

**Files:**
- Create: `auto-parts-wms-vue/src/views/erp/ErpCounterpartySubjectManagement.vue`
- Create: `auto-parts-wms-vue/src/views/erp/ErpCounterpartyFinanceSummary.vue`
- Modify: `auto-parts-wms-vue/src/router/index.ts`
- Modify: `auto-parts-wms-vue/src/locales/zh.ts`
- Modify: `auto-parts-wms-vue/src/locales/en.ts`

- [ ] **Step 1: 先写路由与页面挂载测试**

```js
test('erp routes should include counterparty subject management and finance summary', () => {
  const routerSource = readSource('router/index.ts');
  assert.match(routerSource, /counterparty-subjects/);
  assert.match(routerSource, /ErpCounterpartySubjectManagement\.vue/);
  assert.match(routerSource, /ErpCounterpartyFinanceSummary\.vue/);
});
```

- [ ] **Step 2: 运行前端测试确认失败**

Run: `node --test auto-parts-wms-vue/src/router/__tests__/erpCounterpartyRouteHygiene.test.mjs`

Expected: FAIL with route not found

- [ ] **Step 3: 实现两个页面**

```vue
<template>
  <div class="page-shell page-shell--system">
    <div class="page-header">
      <div class="page-title">往来主体管理</div>
    </div>
  </div>
</template>
```

```vue
<template>
  <div class="page-shell page-shell--system">
    <div class="page-header">
      <div class="page-title">往来主体财务汇总</div>
    </div>
  </div>
</template>
```

- [ ] **Step 4: 运行前端测试确认通过**

Run: `node --test auto-parts-wms-vue/src/router/__tests__/erpCounterpartyRouteHygiene.test.mjs`

Expected: PASS

- [ ] **Step 5: 提交**

```bash
git add auto-parts-wms-vue/src/views/erp/ErpCounterpartySubjectManagement.vue auto-parts-wms-vue/src/views/erp/ErpCounterpartyFinanceSummary.vue auto-parts-wms-vue/src/router/index.ts auto-parts-wms-vue/src/locales/zh.ts auto-parts-wms-vue/src/locales/en.ts auto-parts-wms-vue/src/router/__tests__/erpCounterpartyRouteHygiene.test.mjs
git commit -m "feat: add counterparty subject pages"
```

## Task 11: 回归验证

**Files:**
- Modify: `docs/superpowers/specs/2026-05-28-erp-supplier-counterparty-design.md`
- Modify: `docs/superpowers/plans/2026-05-28-erp-supplier-counterparty-implementation.md`

- [ ] **Step 1: 运行后端核心测试**

Run: `mvn -pl wms-backend -Dtest=ErpMasterDataGuardTests,ErpSupplierCounterpartyTests,ErpFinanceWorkflowTests test`

Expected: all PASS

- [ ] **Step 2: 运行前端路由测试**

Run: `node --test auto-parts-wms-vue/src/router/__tests__/erpCounterpartyRouteHygiene.test.mjs`

Expected: PASS

- [ ] **Step 3: 更新文档中的实际迁移与接口信息**

```md
- 已实现迁移：V118 / V119 / V120 / V121
- 已实现页面：供应商类型、往来主体管理、往来主体财务汇总
```

- [ ] **Step 4: 查看工作区变更确认仅包含计划内文件**

Run: `git status --short`

Expected: only planned files are modified

- [ ] **Step 5: 最终提交**

```bash
git add docs/superpowers/specs/2026-05-28-erp-supplier-counterparty-design.md docs/superpowers/plans/2026-05-28-erp-supplier-counterparty-implementation.md
git commit -m "docs: finalize supplier counterparty implementation plan"
```

## 自检

- 规格覆盖：计划覆盖了设计文档中的供应商类型、供应商扩展字段、往来主体、主体财务汇总、页面改造、导入承接和测试要求。
- 占位扫描：计划中未使用 `TODO`、`TBD`、`后续补充` 等占位语句。
- 一致性检查：迁移版本统一使用 `V118` 至 `V121`，后端字段命名统一使用 `supplierTypeId / businessScope / counterpartySubjectId`，与设计文档一致。
