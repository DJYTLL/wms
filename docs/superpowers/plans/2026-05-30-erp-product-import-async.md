# ERP Product Import Async Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将商品/配件导入从同步阻塞请求改为异步批次任务，前端提交后自动轮询并可查看历史明细。

**Architecture:** 后端新增商品导入批次表与明细表，上传时先创建批次并立即返回，再由后台线程消费文件字节并更新批次状态。前端复用供应商导入历史的交互模型，增加“导入中”自动轮询与结果展示，避免 10 秒请求超时。

**Tech Stack:** Spring Boot, MyBatis-Plus, Flyway, Vue 3, Element Plus, Node test, JUnit 5

---

### Task 1: 先锁住接口和页面行为

**Files:**
- Modify: `D:/project/wms-backend/src/test/java/com/example/wms/ErpMasterDataGuardTests.java`
- Create: `D:/project/auto-parts-wms-vue/src/views/erp/__tests__/erpProductImportAsync.test.mjs`

- [ ] 为后端补失败测试，要求商品导入控制器暴露批次查询接口，导入返回类型不再是旧的 `ErpExcelImportResult`
- [ ] 运行后端单测确认先失败
- [ ] 为前端补失败测试，要求商品页具备导入历史、自动轮询、批次明细相关源码结构
- [ ] 运行前端单测确认先失败

### Task 2: 建立商品导入批次持久化模型

**Files:**
- Create: `D:/project/wms-backend/src/main/resources/db/migration/V127__erp_product_import_batch.sql`
- Create: `D:/project/wms-backend/src/main/java/com/example/wms/entity/erp/ErpProductImportBatch.java`
- Create: `D:/project/wms-backend/src/main/java/com/example/wms/entity/erp/ErpProductImportItem.java`
- Create: `D:/project/wms-backend/src/main/java/com/example/wms/mapper/erp/ErpProductImportBatchMapper.java`
- Create: `D:/project/wms-backend/src/main/java/com/example/wms/mapper/erp/ErpProductImportItemMapper.java`
- Create: `D:/project/wms-backend/src/main/java/com/example/wms/dto/erp/ErpProductImportResult.java`
- Create: `D:/project/wms-backend/src/main/java/com/example/wms/dto/erp/ErpProductImportBatchSummary.java`
- Create: `D:/project/wms-backend/src/main/java/com/example/wms/dto/erp/ErpProductImportItemView.java`

- [ ] 新增 V127 迁移，建立商品导入批次/明细表和索引
- [ ] 建立对应实体、Mapper、DTO

### Task 3: 后端改为异步导入任务

**Files:**
- Modify: `D:/project/wms-backend/src/main/java/com/example/wms/service/erp/ErpProductService.java`
- Modify: `D:/project/wms-backend/src/main/java/com/example/wms/service/erp/impl/ErpProductServiceImpl.java`
- Modify: `D:/project/wms-backend/src/main/java/com/example/wms/controller/erp/ErpProductController.java`

- [ ] 上传时立即创建批次并返回
- [ ] 后台线程解析 Excel、逐行写入明细和汇总状态
- [ ] 新增批次列表和批次明细查询接口
- [ ] 跑后端相关单测并修到通过

### Task 4: 前端改为任务化体验

**Files:**
- Modify: `D:/project/auto-parts-wms-vue/src/views/erp/ErpProductManagement.vue`

- [ ] 新增导入弹窗、导入历史抽屉、批次明细表
- [ ] 上传后展示“任务已创建”，自动轮询直到批次结束
- [ ] 完成后刷新列表并展示成功/失败汇总
- [ ] 跑前端相关单测并修到通过

### Task 5: 完整验证

**Files:**
- Modify: `D:/project/docs/superpowers/plans/2026-05-30-erp-product-import-async.md`

- [ ] 运行后端相关测试
- [ ] 运行前端相关测试
- [ ] 运行必要类型检查
- [ ] 回填计划状态并整理交付说明
