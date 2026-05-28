# ERP Supplier Create Dialog Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rebuild the supplier create/edit dialog in `ErpSupplierManagement.vue` into a grouped two-column modal with collapsed optional sections while preserving existing save behavior.

**Architecture:** Keep all current supplier CRUD data flow intact and confine the redesign to the frontend dialog template, local state, localized copy, and dialog-specific CSS. Protect the new structure with source-based tests that fail before implementation and pass after the layout and interaction hooks are added.

**Tech Stack:** Vue 3, TypeScript, Element Plus, Node test runner, existing project locale files and table styles.

---

### Task 1: Lock the redesigned dialog structure with failing tests

**Files:**
- Create: `auto-parts-wms-vue/src/views/erp/__tests__/erpSupplierDialogRedesign.test.mjs`
- Read: `auto-parts-wms-vue/src/views/erp/ErpSupplierManagement.vue`
- Read: `auto-parts-wms-vue/src/styles/table.css`

- [ ] **Step 1: Write the failing test**

Add assertions for:

- dialog root class `supplier-dialog`
- intro strip class `supplier-dialog__intro`
- grouped sections `基础识别` / `联系人与沟通` / `业务归属`
- optional sections controlled by `optionalSectionsExpanded`
- sticky footer class `supplier-dialog__footer`

- [ ] **Step 2: Run test to verify it fails**

Run: `node --test auto-parts-wms-vue/src/views/erp/__tests__/erpSupplierDialogRedesign.test.mjs`

Expected: FAIL because the current dialog is still a flat single-column form and the new classes/state do not exist.

### Task 2: Add localized copy for the redesigned dialog

**Files:**
- Modify: `auto-parts-wms-vue/src/locales/zh.ts`
- Modify: `auto-parts-wms-vue/src/locales/en.ts`

- [ ] **Step 1: Add new locale keys**

Add copy for:

- supplier dialog subtitle
- intro tags
- section titles
- counterparty subject hint
- financial/history optional section titles
- save button variants

- [ ] **Step 2: Keep key names aligned across zh/en**

Confirm both locale files define the same keys so `vue-i18n` lookups stay stable.

### Task 3: Rebuild the supplier dialog template and state

**Files:**
- Modify: `auto-parts-wms-vue/src/views/erp/ErpSupplierManagement.vue`

- [ ] **Step 1: Add dialog-local UI state**

Introduce:

- `optionalSectionsExpanded`
- `supplierNameInputRef`

Reset optional sections on add/edit open and on dialog close.

- [ ] **Step 2: Replace the flat form with grouped sections**

Implement:

- wider dialog shell
- intro strip
- grouped two-column section cards
- collapsed optional sections
- sticky footer with expand/collapse button and save button text by mode

- [ ] **Step 3: Preserve existing behavior**

Keep:

- existing `formData`
- existing fetch/save/edit/reset flow
- current payload mapping
- current validation entry point

Add post-open focus to supplier name on create.

### Task 4: Add dialog-specific CSS without breaking current list page layout

**Files:**
- Modify: `auto-parts-wms-vue/src/styles/table.css`

- [ ] **Step 1: Add supplier dialog styles**

Create scoped-ish utility classes for:

- dialog shell/body
- intro strip
- section cards and headers
- two-column grid
- responsive single-column fallback
- sticky footer

- [ ] **Step 2: Preserve existing supplier toolbar behavior**

Do not remove or weaken the current `erp-basic-toolbar--inline-wrap` support used by the list page toolbar test.

### Task 5: Verify tests and type safety

**Files:**
- Test: `auto-parts-wms-vue/src/views/erp/__tests__/erpSupplierDialogRedesign.test.mjs`
- Test: `auto-parts-wms-vue/src/views/erp/__tests__/erpSupplierToolbarLayout.test.mjs`
- Verify: `auto-parts-wms-vue/src/views/erp/ErpSupplierManagement.vue`
- Verify: `auto-parts-wms-vue/src/styles/table.css`

- [ ] **Step 1: Run dialog redesign test**

Run: `node --test auto-parts-wms-vue/src/views/erp/__tests__/erpSupplierDialogRedesign.test.mjs`

Expected: PASS

- [ ] **Step 2: Run existing supplier toolbar regression test**

Run: `node --test auto-parts-wms-vue/src/views/erp/__tests__/erpSupplierToolbarLayout.test.mjs`

Expected: PASS

- [ ] **Step 3: Run frontend type check**

Run: `npm run type-check`

Workdir: `D:\project\auto-parts-wms-vue`

Expected: exit code 0

- [ ] **Step 4: Review final diff**

Confirm only the intended dialog redesign files changed for this task:

- `auto-parts-wms-vue/src/views/erp/ErpSupplierManagement.vue`
- `auto-parts-wms-vue/src/styles/table.css`
- `auto-parts-wms-vue/src/locales/zh.ts`
- `auto-parts-wms-vue/src/locales/en.ts`
- `auto-parts-wms-vue/src/views/erp/__tests__/erpSupplierDialogRedesign.test.mjs`
- `docs/superpowers/specs/2026-05-29-erp-supplier-create-dialog-redesign.md`
- `docs/superpowers/plans/2026-05-29-erp-supplier-create-dialog-redesign-implementation.md`
