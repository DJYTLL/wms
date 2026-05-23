# List Page Refresh Button Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a manual refresh button to list-page toolbars so the current tab can be reloaded after upstream data changes.

**Architecture:** Reuse the existing `MainLayout.vue` keep-alive key versioning and inject a single refresh button into the active list page's toolbar area. Detect eligible routes conservatively from the current path and only target the header-level `.table-actions` container so form pages stay untouched.

**Tech Stack:** Vue 3, Vue Router, Element Plus, existing keep-alive tab layout

---

### Task 1: Add global list-page refresh injection

**Files:**
- Modify: `auto-parts-wms-vue/src/layouts/MainLayout.vue`

- [ ] **Step 1: Add route eligibility and toolbar target sync logic**

- [ ] **Step 2: Render a teleported icon-only refresh button into the active list toolbar**

- [ ] **Step 3: Reuse `viewKeyVersions` to remount only the current tab page on click**

- [ ] **Step 4: Keep button ordering before primary actions and hide it when no eligible toolbar exists**

### Task 2: Verify no regression in the frontend shell

**Files:**
- Verify: `auto-parts-wms-vue/src/layouts/MainLayout.vue`

- [ ] **Step 1: Run `npm run build` in `auto-parts-wms-vue`**

- [ ] **Step 2: Confirm the Vite build and type-check complete without errors**
