# Stock Init Async Approval Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将期初库存审核改为异步提交，并在后台按 200 行分批事务处理。

**Architecture:** 后端在期初审核入口把单据状态切到 `APPROVING` 后投递后台执行器；后台使用 `TransactionOperations` 按 200 行切块提交，成功后置 `APPROVED`，失败后置 `APPROVE_FAILED`。前端识别新增状态并在审核后轮询刷新。

**Tech Stack:** Spring Boot、MyBatis-Plus、Vue 3、Element Plus、JUnit 5、Mockito、Flyway

---

## File Structure

- Modify: `D:/project/wms-backend/src/main/java/com/example/wms/service/erp/impl/ErpStockCountServiceImpl.java`
- Modify: `D:/project/wms-backend/src/main/java/com/example/wms/controller/erp/ErpStockInitController.java`
- Modify: `D:/project/wms-backend/src/main/java/com/example/wms/entity/erp/ErpStockCount.java`
- Modify: `D:/project/wms-backend/src/test/java/com/example/wms/ErpInventoryWorkflowTests.java`
- Modify: `D:/project/auto-parts-wms-vue/src/views/erp/ErpStockCountManagement.vue`

## Tasks

### Task 1: 锁定异步审核行为

- [ ] 为期初审核补失败测试，验证提交后立即进入 `APPROVING` 且后台任务被排队。
- [ ] 为期初审核补失败测试，验证后台按 200 行分批调用事务执行。
- [ ] 运行单测并确认先红灯。

### Task 2: 实现后端异步审核

- [ ] 为 `INIT` 审核增加 `APPROVING`、`APPROVE_FAILED` 状态流转。
- [ ] 将期初审核提交改成轻事务，仅做校验、改状态、投递后台任务。
- [ ] 后台按 200 行切块处理，每块用 `TransactionOperations` 独立提交。
- [ ] 成功时改为 `APPROVED`，失败时改为 `APPROVE_FAILED`。

### Task 3: 实现前端状态与轮询

- [ ] 列表筛选、状态标签、按钮禁用逻辑支持 `APPROVING`、`APPROVE_FAILED`。
- [ ] 期初审核提交成功后启动轮询，直到无 `APPROVING` 行。

### Task 4: 验证

- [ ] 运行 `ErpInventoryWorkflowTests`。
- [ ] 运行受影响前端测试或最小化源码检查。
