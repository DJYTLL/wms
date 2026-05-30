# ERP Category Default Flag Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a default-category option to ERP product category management.

**Architecture:** Reuse the existing ERP customer category default pattern. Add an `is_default` database column, expose it through DTO/entity/mapper/service, and show it in the category list and edit dialog.

**Tech Stack:** Spring Boot, MyBatis-Plus, Flyway, Vue 3, Element Plus, TypeScript.

---

### Task 1: Backend Default Category Behavior

**Files:**
- Modify: `wms-backend/src/test/java/com/example/wms/ErpMasterDataGuardTests.java`
- Modify: `wms-backend/src/main/java/com/example/wms/dto/erp/ErpCategoryCreateRequest.java`
- Modify: `wms-backend/src/main/java/com/example/wms/dto/erp/ErpCategoryUpdateRequest.java`
- Modify: `wms-backend/src/main/java/com/example/wms/entity/erp/ErpCategory.java`
- Modify: `wms-backend/src/main/java/com/example/wms/mapper/erp/ErpCategoryMapper.java`
- Modify: `wms-backend/src/main/java/com/example/wms/service/erp/impl/ErpCategoryServiceImpl.java`

- [ ] **Step 1: Write failing test**

```java
@Test
void categoryCreateClearsOtherDefaultCategories() {
    ErpCategoryServiceImpl service = new ErpCategoryServiceImpl(categoryMapper, productMapper, orderSequenceMapper, systemConfigMapper);
    when(categoryMapper.findByCode(1L, "CAT-DEFAULT")).thenReturn(null);
    doAnswer(invocation -> {
        ErpCategory category = invocation.getArgument(0);
        category.setId(99L);
        return 1;
    }).when(categoryMapper).insert(any(ErpCategory.class));

    service.create(new ErpCategoryCreateRequest("CAT-DEFAULT", "默认分类", null, 1, 0, true, true, null));

    verify(categoryMapper).insert(argThat(category -> Boolean.TRUE.equals(category.getIsDefault())));
    verify(categoryMapper).clearDefault(1L, 99L);
}
```

- [ ] **Step 2: Verify red**

Run: `mvn -Dtest=ErpMasterDataGuardTests#categoryCreateClearsOtherDefaultCategories test`
Expected: compile failure because `ErpCategoryCreateRequest` and `ErpCategoryMapper` do not yet expose default fields/methods.

- [ ] **Step 3: Implement backend**

Add `Boolean isDefault` to create/update request records; add `isDefault` field/getter/setter to `ErpCategory`; add `clearDefault` and `findDefault` mapper methods; set and clear default in create/update; sort defaults first in list/page.

- [ ] **Step 4: Verify green**

Run: `mvn -Dtest=ErpMasterDataGuardTests#categoryCreateClearsOtherDefaultCategories test`
Expected: test passes.

### Task 2: Migration

**Files:**
- Create: `wms-backend/src/main/resources/db/migration/V129__erp_category_default_flag.sql`

- [ ] **Step 1: Add migration**

```sql
ALTER TABLE erp_category
    ADD COLUMN IF NOT EXISTS is_default BOOLEAN NOT NULL DEFAULT FALSE;

COMMENT ON COLUMN erp_category.is_default IS '是否默认';

WITH ranked AS (
    SELECT id, ROW_NUMBER() OVER (PARTITION BY tenant_id ORDER BY sort_no NULLS LAST, id) AS rn
    FROM erp_category
    WHERE deleted_at IS NULL
)
UPDATE erp_category c
SET is_default = TRUE
FROM ranked r
WHERE c.id = r.id
  AND r.rn = 1
  AND NOT EXISTS (
      SELECT 1
      FROM erp_category existing
      WHERE existing.tenant_id = c.tenant_id
        AND existing.deleted_at IS NULL
        AND existing.is_default = TRUE
  );

CREATE UNIQUE INDEX IF NOT EXISTS uk_erp_category_default
    ON erp_category (tenant_id)
    WHERE is_default = TRUE AND deleted_at IS NULL;
```

- [ ] **Step 2: Validate version sequence**

Run: `Get-ChildItem wms-backend\src\main\resources\db\migration -Filter 'V*.sql' | Sort-Object Name | Select-Object -Last 3`
Expected: `V127`, `V128`, `V129` are the final versions.

### Task 3: Frontend Category Management

**Files:**
- Modify: `auto-parts-wms-vue/src/views/erp/ErpCategoryManagement.vue`

- [ ] **Step 1: Add UI field**

Add `isDefault?: boolean` to `ErpCategory`, add `default` to `defaultColumns`, render a default tag column, add an edit dialog switch, and include/reset/copy `formData.isDefault`.

- [ ] **Step 2: Verify frontend types**

Run: `npm run type-check`
Expected: exit 0.

### Task 4: Final Verification

- [ ] Run backend targeted test.
- [ ] Run frontend type check.
- [ ] Inspect `git diff` to ensure only intended files were changed.
