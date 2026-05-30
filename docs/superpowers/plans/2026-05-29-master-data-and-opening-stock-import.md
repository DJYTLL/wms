# Master Data And Opening Stock Import Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为供应商、客户、配件新增 Excel 直传导入，并在库存余额页新增“导入期初库存”，支持 `.xls/.xlsx`，同时保证库存导入不反写商品主数据。

**Architecture:** 采用“共享 Excel 解析底座 + 各业务服务独立导入规则”的结构。供应商沿用现有导入批次体系并把入口切换成 Excel 上传；客户、配件、库存各自补导入接口和页面入口，库存仅导入库存事实字段，商品重复字段只记 warning。

**Tech Stack:** Spring Boot 3.3、MyBatis-Plus、Flyway、Vue 3、Element Plus、Node `node:test`、Maven、Apache POI（如缺失则补充）

---

## File Structure

### Shared Backend Import Infrastructure

- Modify: `D:/project/wms-backend/pom.xml`
  - 增加 Excel 解析依赖（如 Apache POI）
- Create: `D:/project/wms-backend/src/main/java/com/example/wms/service/erp/support/ExcelImportSheet.java`
  - 承载“表头 + 行数据”解析结果
- Create: `D:/project/wms-backend/src/main/java/com/example/wms/service/erp/support/ExcelImportParser.java`
  - 统一解析 `.xls/.xlsx` 第一张工作表
- Create: `D:/project/wms-backend/src/test/java/com/example/wms/service/erp/support/ExcelImportParserTest.java`
  - 覆盖 `.xls/.xlsx`、空行、表头读取

### Supplier Import

- Modify: `D:/project/wms-backend/src/main/java/com/example/wms/controller/erp/ErpSupplierController.java`
- Modify: `D:/project/wms-backend/src/main/java/com/example/wms/service/erp/ErpSupplierService.java`
- Modify: `D:/project/wms-backend/src/main/java/com/example/wms/service/erp/impl/ErpSupplierServiceImpl.java`
- Modify: `D:/project/wms-backend/src/test/java/com/example/wms/ErpFinanceWorkflowTests.java`
- Modify: `D:/project/auto-parts-wms-vue/src/views/erp/ErpSupplierManagement.vue`
- Modify: `D:/project/auto-parts-wms-vue/src/views/erp/__tests__/erpSupplierDialogRedesign.test.mjs`

### Customer Import

- Modify: `D:/project/wms-backend/src/main/java/com/example/wms/controller/erp/ErpCustomerController.java`
- Modify: `D:/project/wms-backend/src/main/java/com/example/wms/service/erp/ErpCustomerService.java`
- Modify: `D:/project/wms-backend/src/main/java/com/example/wms/service/erp/impl/ErpCustomerServiceImpl.java`
- Create: `D:/project/wms-backend/src/main/java/com/example/wms/dto/erp/ErpCustomerImportResult.java`
- Create: `D:/project/wms-backend/src/test/java/com/example/wms/ErpCustomerImportTests.java`
- Modify: `D:/project/auto-parts-wms-vue/src/views/erp/ErpCustomerManagement.vue`
- Create: `D:/project/auto-parts-wms-vue/src/views/erp/__tests__/erpCustomerImportEntry.test.mjs`

### Product Import

- Modify: `D:/project/wms-backend/src/main/java/com/example/wms/controller/erp/ErpProductController.java`
- Modify: `D:/project/wms-backend/src/main/java/com/example/wms/service/erp/ErpProductService.java`
- Modify: `D:/project/wms-backend/src/main/java/com/example/wms/service/erp/impl/ErpProductServiceImpl.java`
- Create: `D:/project/wms-backend/src/main/java/com/example/wms/dto/erp/ErpProductImportResult.java`
- Create: `D:/project/wms-backend/src/test/java/com/example/wms/ErpProductImportTests.java`
- Modify: `D:/project/auto-parts-wms-vue/src/views/erp/ErpProductManagement.vue`
- Create: `D:/project/auto-parts-wms-vue/src/views/erp/__tests__/erpProductImportEntry.test.mjs`

### Opening Stock Import

- Modify: `D:/project/wms-backend/src/main/java/com/example/wms/controller/erp/ErpStockController.java`
- Modify: `D:/project/wms-backend/src/main/java/com/example/wms/service/erp/ErpStockService.java`
- Modify: `D:/project/wms-backend/src/main/java/com/example/wms/service/erp/impl/ErpStockServiceImpl.java`
- Create: `D:/project/wms-backend/src/main/java/com/example/wms/dto/erp/ErpOpeningStockImportResult.java`
- Create: `D:/project/wms-backend/src/test/java/com/example/wms/ErpOpeningStockImportTests.java`
- Modify: `D:/project/auto-parts-wms-vue/src/views/erp/ErpStockManagement.vue`
- Create: `D:/project/auto-parts-wms-vue/src/views/erp/__tests__/erpOpeningStockImportEntry.test.mjs`

### Optional Migration Files

- Create if needed only after rescanning:
  - `D:/project/wms-backend/src/main/resources/db/migration/V126__...sql`
  - `D:/project/wms-backend/src/main/resources/db/migration/V127__...sql`

---

### Task 1: Build Shared Excel Import Infrastructure

**Files:**
- Modify: `D:/project/wms-backend/pom.xml`
- Create: `D:/project/wms-backend/src/main/java/com/example/wms/service/erp/support/ExcelImportSheet.java`
- Create: `D:/project/wms-backend/src/main/java/com/example/wms/service/erp/support/ExcelImportParser.java`
- Create: `D:/project/wms-backend/src/test/java/com/example/wms/service/erp/support/ExcelImportParserTest.java`

- [ ] **Step 1: Write the failing parser tests**

```java
package com.example.wms.service.erp.support;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ExcelImportParserTest {

    private final ExcelImportParser parser = new ExcelImportParser();

    @Test
    void parsesXlsFirstSheetIntoHeadersAndRows() throws Exception {
        byte[] content = Files.readAllBytes(Path.of("C:/Users/Administrator/Downloads/供应商档案表.xls"));

        ExcelImportSheet sheet = parser.parse("supplier.xls", content);

        assertEquals("编码", sheet.headers().get(0));
        assertEquals("名称", sheet.headers().get(1));
        assertEquals("20260401153247", sheet.rows().get(0).get("编码"));
    }

    @Test
    void rejectsUnsupportedExtension() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse("supplier.csv", new byte[] {1, 2, 3}));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -Dtest=ExcelImportParserTest test`

Expected: FAIL，提示 `ExcelImportParser` 或 `ExcelImportSheet` 不存在。

- [ ] **Step 3: Write the minimal parser implementation**

```java
package com.example.wms.service.erp.support;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record ExcelImportSheet(List<String> headers, List<Map<String, String>> rows) {
}

@Component
public class ExcelImportParser {
    private final DataFormatter formatter = new DataFormatter();

    public ExcelImportSheet parse(String filename, byte[] content) {
        if (filename == null || (!filename.endsWith(".xls") && !filename.endsWith(".xlsx"))) {
            throw new IllegalArgumentException("仅支持 .xls 或 .xlsx 文件");
        }
        if (content == null || content.length == 0) {
            throw new IllegalArgumentException("导入文件不能为空");
        }
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(content))) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null || sheet.getPhysicalNumberOfRows() == 0) {
                throw new IllegalArgumentException("Excel 没有可读取的数据");
            }
            List<String> headers = readHeaders(sheet.getRow(sheet.getFirstRowNum()));
            List<Map<String, String>> rows = new ArrayList<>();
            for (int rowIndex = sheet.getFirstRowNum() + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) {
                    continue;
                }
                Map<String, String> values = new LinkedHashMap<>();
                boolean blank = true;
                for (int cellIndex = 0; cellIndex < headers.size(); cellIndex++) {
                    String header = headers.get(cellIndex);
                    String value = formatter.formatCellValue(row.getCell(cellIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL)).trim();
                    if (!value.isEmpty()) {
                        blank = false;
                    }
                    values.put(header, value);
                }
                if (!blank) {
                    rows.add(values);
                }
            }
            return new ExcelImportSheet(headers, rows);
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException("Excel 解析失败", ex);
        }
    }

    private List<String> readHeaders(Row headerRow) {
        if (headerRow == null) {
            throw new IllegalArgumentException("Excel 缺少表头");
        }
        List<String> headers = new ArrayList<>();
        for (int cellIndex = 0; cellIndex < headerRow.getLastCellNum(); cellIndex++) {
            Cell cell = headerRow.getCell(cellIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
            headers.add(cell == null ? "" : formatter.formatCellValue(cell).trim());
        }
        return headers;
    }
}
```

- [ ] **Step 4: Add Excel dependency**

```xml
<dependency>
    <groupId>org.apache.poi</groupId>
    <artifactId>poi-ooxml</artifactId>
    <version>5.3.0</version>
</dependency>
```

- [ ] **Step 5: Run test to verify it passes**

Run: `mvn -Dtest=ExcelImportParserTest test`

Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add D:/project/wms-backend/pom.xml D:/project/wms-backend/src/main/java/com/example/wms/service/erp/support/ExcelImportSheet.java D:/project/wms-backend/src/main/java/com/example/wms/service/erp/support/ExcelImportParser.java D:/project/wms-backend/src/test/java/com/example/wms/service/erp/support/ExcelImportParserTest.java
git commit -m "feat: add shared excel import parser"
```

### Task 2: Re-scan Migration Directory And Decide Whether Import Tables Need New Migrations

**Files:**
- Inspect: `D:/project/wms-backend/src/main/resources/db/migration`
- Create if needed: `D:/project/wms-backend/src/main/resources/db/migration/V126__*.sql`
- Modify if needed: corresponding entity/mapper files

- [ ] **Step 1: Re-scan migration directory before any DB schema change**

Run: `Get-ChildItem D:\project\wms-backend\src\main\resources\db\migration | Select-Object -ExpandProperty Name`

Expected: 记录当前最高版本号；若仍为 `V125__seed_erp_supplier_type_uncategorized.sql`，下一号从 `V126` 开始。

- [ ] **Step 2: Write the failing schema coverage test or assertion first**

```java
@Test
void customerAndProductImportCanPersistBatchDetails() {
    assertDoesNotThrow(() -> repositoryLayerCanSaveImportBatch("CUSTOMER"));
}
```

- [ ] **Step 3: Run the test or inspect current mappings to verify the gap**

Run: `mvn -Dtest=ErpCustomerImportTests,ErpProductImportTests test`

Expected: FAIL，如果现有结构无法持久化批次/明细，则确认需要新增 migration。

- [ ] **Step 4: Add minimal migration only if the failing test proves it is required**

```sql
-- Example only if needed after re-scan
CREATE TABLE erp_customer_import_batch (...);
CREATE TABLE erp_customer_import_item (...);
CREATE INDEX idx_erp_customer_import_batch_tenant_created_at ON erp_customer_import_batch(tenant_id, created_at DESC);
```

- [ ] **Step 5: Re-run the targeted tests**

Run: `mvn -Dtest=ErpCustomerImportTests,ErpProductImportTests test`

Expected: PASS 或至少从“缺表/缺字段”错误推进到下一层业务失败。

- [ ] **Step 6: Commit**

```bash
git add D:/project/wms-backend/src/main/resources/db/migration
git commit -m "feat: add import batch schema if required"
```

### Task 3: Switch Supplier Import From Markdown Paste To Excel Upload

**Files:**
- Modify: `D:/project/wms-backend/src/main/java/com/example/wms/controller/erp/ErpSupplierController.java`
- Modify: `D:/project/wms-backend/src/main/java/com/example/wms/service/erp/ErpSupplierService.java`
- Modify: `D:/project/wms-backend/src/main/java/com/example/wms/service/erp/impl/ErpSupplierServiceImpl.java`
- Modify: `D:/project/wms-backend/src/test/java/com/example/wms/ErpFinanceWorkflowTests.java`
- Modify: `D:/project/auto-parts-wms-vue/src/views/erp/ErpSupplierManagement.vue`
- Modify: `D:/project/auto-parts-wms-vue/src/views/erp/__tests__/erpSupplierDialogRedesign.test.mjs`

- [ ] **Step 1: Write the failing frontend test for Excel upload UI**

```js
test('supplier management uses excel upload instead of markdown paste import', () => {
  const pageSource = readView('ErpSupplierManagement.vue');

  assert.match(pageSource, /accept="\.xls,\.xlsx"/);
  assert.match(pageSource, /new FormData\(\)/);
  assert.doesNotMatch(pageSource, /Markdown 表格/);
  assert.doesNotMatch(pageSource, /rawTable/);
});
```

- [ ] **Step 2: Run the frontend test to verify it fails**

Run: `node --test D:/project/auto-parts-wms-vue/src/views/erp/__tests__/erpSupplierDialogRedesign.test.mjs`

Expected: FAIL，因为现有页面仍使用 `Markdown 表格` 和 `rawTable`。

- [ ] **Step 3: Write the failing backend supplier import test for multipart Excel**

```java
@Test
void importSuppliersAcceptsExcelWorkbook() throws Exception {
    byte[] content = Files.readAllBytes(Path.of("C:/Users/Administrator/Downloads/供应商档案表.xls"));

    var result = service.importSuppliers("供应商档案表", "供应商档案表.xls", content);

    assertEquals(1, result.successCount() > 0 ? 1 : 0);
}
```

- [ ] **Step 4: Run the backend test to verify it fails**

Run: `mvn -Dtest=ErpFinanceWorkflowTests test`

Expected: FAIL，因为服务签名和控制器仍是 JSON/`rawTable` 口径。

- [ ] **Step 5: Write the minimal backend implementation**

```java
@PostMapping("/import")
@PreAuthorize("hasAuthority('PERM_erp-supplier:import')")
public ResponseEntity<ApiResponse<ErpSupplierImportResult>> importSuppliers(
    @RequestParam(required = false) String sourceName,
    @RequestParam("file") MultipartFile file
) throws IOException {
    return ResponseEntity.ok(ApiResponse.ok(
        erpSupplierService.importSuppliers(sourceName, file.getOriginalFilename(), file.getBytes())
    ));
}
```

```java
ErpSupplierImportResult importSuppliers(String sourceName, String filename, byte[] fileContent);
```

- [ ] **Step 6: Reuse the shared parser in supplier service**

```java
ExcelImportSheet sheet = excelImportParser.parse(filename, fileContent);
validateRequiredHeaders(sheet.headers(), List.of("编码", "名称"));
for (Map<String, String> rowMap : sheet.rows()) {
    // reuse current supplier matching / settlement / upsert logic
}
```

- [ ] **Step 7: Update the supplier page to upload FormData**

```ts
const formData = new FormData();
formData.append('file', importFile.value);
if (importSourceName.value.trim()) formData.append('sourceName', importSourceName.value.trim());
await request.post('/erp/suppliers/import', formData, {
  headers: { 'Content-Type': 'multipart/form-data' }
});
```

- [ ] **Step 8: Re-run frontend and backend tests**

Run: `node --test D:/project/auto-parts-wms-vue/src/views/erp/__tests__/erpSupplierDialogRedesign.test.mjs`

Expected: PASS

Run: `mvn -Dtest=ErpFinanceWorkflowTests test`

Expected: PASS

- [ ] **Step 9: Commit**

```bash
git add D:/project/wms-backend/src/main/java/com/example/wms/controller/erp/ErpSupplierController.java D:/project/wms-backend/src/main/java/com/example/wms/service/erp/ErpSupplierService.java D:/project/wms-backend/src/main/java/com/example/wms/service/erp/impl/ErpSupplierServiceImpl.java D:/project/wms-backend/src/test/java/com/example/wms/ErpFinanceWorkflowTests.java D:/project/auto-parts-wms-vue/src/views/erp/ErpSupplierManagement.vue D:/project/auto-parts-wms-vue/src/views/erp/__tests__/erpSupplierDialogRedesign.test.mjs
git commit -m "feat: switch supplier import to excel upload"
```

### Task 4: Add Customer Excel Import

**Files:**
- Modify: `D:/project/wms-backend/src/main/java/com/example/wms/controller/erp/ErpCustomerController.java`
- Modify: `D:/project/wms-backend/src/main/java/com/example/wms/service/erp/ErpCustomerService.java`
- Modify: `D:/project/wms-backend/src/main/java/com/example/wms/service/erp/impl/ErpCustomerServiceImpl.java`
- Create: `D:/project/wms-backend/src/main/java/com/example/wms/dto/erp/ErpCustomerImportResult.java`
- Create: `D:/project/wms-backend/src/test/java/com/example/wms/ErpCustomerImportTests.java`
- Modify: `D:/project/auto-parts-wms-vue/src/views/erp/ErpCustomerManagement.vue`
- Create: `D:/project/auto-parts-wms-vue/src/views/erp/__tests__/erpCustomerImportEntry.test.mjs`

- [ ] **Step 1: Write the failing frontend customer import entry test**

```js
test('customer management exposes excel import entry', () => {
  const source = readFileSync('D:/project/auto-parts-wms-vue/src/views/erp/ErpCustomerManagement.vue', 'utf8');
  assert.match(source, /v-permission="'erp-customer:import'"/);
  assert.match(source, /accept="\.xls,\.xlsx"/);
  assert.match(source, /request\.post\('\/erp\/customers\/import'/);
});
```

- [ ] **Step 2: Run the frontend test to verify it fails**

Run: `node --test D:/project/auto-parts-wms-vue/src/views/erp/__tests__/erpCustomerImportEntry.test.mjs`

Expected: FAIL，因为当前页面还没有导入入口。

- [ ] **Step 3: Write the failing backend customer import test**

```java
@Test
void importsCustomersFromExcel() throws Exception {
    byte[] content = Files.readAllBytes(Path.of("C:/Users/Administrator/Downloads/客户档案表.xls"));

    ErpCustomerImportResult result = service.importCustomers("客户档案表", "客户档案表.xls", content);

    assertTrue(result.successCount() > 0);
    assertEquals(0, result.failedCount());
}
```

- [ ] **Step 4: Run the backend test to verify it fails**

Run: `mvn -Dtest=ErpCustomerImportTests test`

Expected: FAIL，因为服务与控制器尚未提供客户导入。

- [ ] **Step 5: Write the minimal customer import implementation**

```java
@PostMapping("/import")
@PreAuthorize("hasAuthority('PERM_erp-customer:import')")
public ResponseEntity<ApiResponse<ErpCustomerImportResult>> importCustomers(
    @RequestParam(required = false) String sourceName,
    @RequestParam("file") MultipartFile file
) throws IOException {
    return ResponseEntity.ok(ApiResponse.ok(
        erpCustomerService.importCustomers(sourceName, file.getOriginalFilename(), file.getBytes())
    ));
}
```

```java
validateRequiredHeaders(sheet.headers(), List.of("编码", "名称"));
String code = trimToNull(row.get("编码"));
String name = trimToNull(row.get("名称"));
String settlementName = trimToNull(row.get("默认结算方式"));
String enabledText = trimToNull(row.get("启用/停用状态"));
```

- [ ] **Step 6: Add the customer page upload dialog**

```ts
<el-button v-permission="'erp-customer:import'" @click="openImportDialog">{{ $t('action.import') }}</el-button>
```

```ts
const formData = new FormData();
formData.append('file', importFile.value);
await request.post('/erp/customers/import', formData, { headers: { 'Content-Type': 'multipart/form-data' } });
```

- [ ] **Step 7: Re-run the tests**

Run: `node --test D:/project/auto-parts-wms-vue/src/views/erp/__tests__/erpCustomerImportEntry.test.mjs`

Expected: PASS

Run: `mvn -Dtest=ErpCustomerImportTests test`

Expected: PASS

- [ ] **Step 8: Commit**

```bash
git add D:/project/wms-backend/src/main/java/com/example/wms/controller/erp/ErpCustomerController.java D:/project/wms-backend/src/main/java/com/example/wms/service/erp/ErpCustomerService.java D:/project/wms-backend/src/main/java/com/example/wms/service/erp/impl/ErpCustomerServiceImpl.java D:/project/wms-backend/src/main/java/com/example/wms/dto/erp/ErpCustomerImportResult.java D:/project/wms-backend/src/test/java/com/example/wms/ErpCustomerImportTests.java D:/project/auto-parts-wms-vue/src/views/erp/ErpCustomerManagement.vue D:/project/auto-parts-wms-vue/src/views/erp/__tests__/erpCustomerImportEntry.test.mjs
git commit -m "feat: add customer excel import"
```

### Task 5: Add Product Excel Import

**Files:**
- Modify: `D:/project/wms-backend/src/main/java/com/example/wms/controller/erp/ErpProductController.java`
- Modify: `D:/project/wms-backend/src/main/java/com/example/wms/service/erp/ErpProductService.java`
- Modify: `D:/project/wms-backend/src/main/java/com/example/wms/service/erp/impl/ErpProductServiceImpl.java`
- Create: `D:/project/wms-backend/src/main/java/com/example/wms/dto/erp/ErpProductImportResult.java`
- Create: `D:/project/wms-backend/src/test/java/com/example/wms/ErpProductImportTests.java`
- Modify: `D:/project/auto-parts-wms-vue/src/views/erp/ErpProductManagement.vue`
- Create: `D:/project/auto-parts-wms-vue/src/views/erp/__tests__/erpProductImportEntry.test.mjs`

- [ ] **Step 1: Write the failing product import tests first**

```js
test('product management exposes excel import entry', () => {
  const source = readFileSync('D:/project/auto-parts-wms-vue/src/views/erp/ErpProductManagement.vue', 'utf8');
  assert.match(source, /v-permission="'erp-product:import'"/);
  assert.match(source, /request\.post\('\/erp\/products\/import'/);
});
```

```java
@Test
void importsProductsFromExcel() throws Exception {
    byte[] content = Files.readAllBytes(Path.of("C:/Users/Administrator/Downloads/配件档案列表.xls"));

    ErpProductImportResult result = service.importProducts("配件档案列表", "配件档案列表.xls", content);

    assertTrue(result.successCount() > 0);
}
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `node --test D:/project/auto-parts-wms-vue/src/views/erp/__tests__/erpProductImportEntry.test.mjs`

Expected: FAIL

Run: `mvn -Dtest=ErpProductImportTests test`

Expected: FAIL

- [ ] **Step 3: Write the minimal product import implementation**

```java
validateRequiredHeaders(sheet.headers(), List.of("编码", "配件名称", "单位"));
String code = trimToNull(row.get("编码"));
String name = trimToNull(row.get("配件名称"));
String unitName = trimToNull(row.get("单位"));
String categoryName = trimToNull(row.get("类别"));
String supplierName = trimToNull(row.get("供应商名称"));
```

```java
if (defaultWarehouse == null && trimToNull(row.get("默认仓库")) != null) {
    warnings.add("默认仓库未匹配，已忽略");
}
if (sourceSupplier == null && trimToNull(row.get("供应商名称")) != null) {
    warnings.add("来源供应商未匹配，已忽略");
}
```

- [ ] **Step 4: Add the product page upload dialog**

```ts
<el-button v-permission="'erp-product:import'" @click="openImportDialog">{{ $t('action.import') }}</el-button>
```

- [ ] **Step 5: Re-run the tests**

Run: `node --test D:/project/auto-parts-wms-vue/src/views/erp/__tests__/erpProductImportEntry.test.mjs`

Expected: PASS

Run: `mvn -Dtest=ErpProductImportTests test`

Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add D:/project/wms-backend/src/main/java/com/example/wms/controller/erp/ErpProductController.java D:/project/wms-backend/src/main/java/com/example/wms/service/erp/ErpProductService.java D:/project/wms-backend/src/main/java/com/example/wms/service/erp/impl/ErpProductServiceImpl.java D:/project/wms-backend/src/main/java/com/example/wms/dto/erp/ErpProductImportResult.java D:/project/wms-backend/src/test/java/com/example/wms/ErpProductImportTests.java D:/project/auto-parts-wms-vue/src/views/erp/ErpProductManagement.vue D:/project/auto-parts-wms-vue/src/views/erp/__tests__/erpProductImportEntry.test.mjs
git commit -m "feat: add product excel import"
```

### Task 6: Add Opening Stock Import Without Back-Writing Product Master Data

**Files:**
- Modify: `D:/project/wms-backend/src/main/java/com/example/wms/controller/erp/ErpStockController.java`
- Modify: `D:/project/wms-backend/src/main/java/com/example/wms/service/erp/ErpStockService.java`
- Modify: `D:/project/wms-backend/src/main/java/com/example/wms/service/erp/impl/ErpStockServiceImpl.java`
- Create: `D:/project/wms-backend/src/main/java/com/example/wms/dto/erp/ErpOpeningStockImportResult.java`
- Create: `D:/project/wms-backend/src/test/java/com/example/wms/ErpOpeningStockImportTests.java`
- Modify: `D:/project/auto-parts-wms-vue/src/views/erp/ErpStockManagement.vue`
- Create: `D:/project/auto-parts-wms-vue/src/views/erp/__tests__/erpOpeningStockImportEntry.test.mjs`

- [ ] **Step 1: Write the failing opening stock tests**

```java
@Test
void openingStockImportFailsWhenProductCodeDoesNotExist() throws Exception {
    byte[] content = Files.readAllBytes(Path.of("C:/Users/Administrator/Downloads/库存明细浏览表.xls"));

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
        () -> service.importOpeningStock("库存明细浏览表", "库存明细浏览表.xls", content));

    assertTrue(ex.getMessage().contains("配件编码不存在"));
}

@Test
void openingStockImportAddsWarningWhenProductFieldsDiffer() throws Exception {
    ErpOpeningStockImportResult result = service.importOpeningStock("库存明细浏览表", "库存明细浏览表.xls", adjustedContentWithMismatchedName);
    assertTrue(result.items().stream().anyMatch(item -> item.warningMessage() != null && item.warningMessage().contains("商品名称不一致")));
}
```

```js
test('stock management exposes opening stock excel import entry', () => {
  const source = readFileSync('D:/project/auto-parts-wms-vue/src/views/erp/ErpStockManagement.vue', 'utf8');
  assert.match(source, /导入期初库存|导入库存余额/);
  assert.match(source, /request\.post\('\/erp\/stock\/opening-import'/);
});
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `node --test D:/project/auto-parts-wms-vue/src/views/erp/__tests__/erpOpeningStockImportEntry.test.mjs`

Expected: FAIL

Run: `mvn -Dtest=ErpOpeningStockImportTests test`

Expected: FAIL

- [ ] **Step 3: Write the minimal stock import implementation**

```java
@PostMapping("/opening-import")
@PreAuthorize("hasAuthority('PERM_erp-stock:edit')")
public ResponseEntity<ApiResponse<ErpOpeningStockImportResult>> importOpeningStock(
    @RequestParam(required = false) String sourceName,
    @RequestParam("file") MultipartFile file
) throws IOException {
    return ResponseEntity.ok(ApiResponse.ok(
        erpStockService.importOpeningStock(sourceName, file.getOriginalFilename(), file.getBytes())
    ));
}
```

```java
validateRequiredHeaders(sheet.headers(), List.of("仓库", "编码", "库存数"));
ErpProduct product = requireProductByCode(code);
String warning = compareProductSnapshotFields(product, row);
Long warehouseId = resolveWarehouseIdOrNull(trimToNull(row.get("仓库")));
BigDecimal qty = parseDecimal(row.get("库存数"));
BigDecimal unitCost = parseDecimal(row.get("库存成本价"));
// write/update stock balance only; do not mutate product master data
```

- [ ] **Step 4: Add stock page upload dialog**

```ts
<el-button v-permission="'erp-stock:edit'" @click="openOpeningImportDialog">导入期初库存</el-button>
```

- [ ] **Step 5: Re-run the tests**

Run: `node --test D:/project/auto-parts-wms-vue/src/views/erp/__tests__/erpOpeningStockImportEntry.test.mjs`

Expected: PASS

Run: `mvn -Dtest=ErpOpeningStockImportTests test`

Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add D:/project/wms-backend/src/main/java/com/example/wms/controller/erp/ErpStockController.java D:/project/wms-backend/src/main/java/com/example/wms/service/erp/ErpStockService.java D:/project/wms-backend/src/main/java/com/example/wms/service/erp/impl/ErpStockServiceImpl.java D:/project/wms-backend/src/main/java/com/example/wms/dto/erp/ErpOpeningStockImportResult.java D:/project/wms-backend/src/test/java/com/example/wms/ErpOpeningStockImportTests.java D:/project/auto-parts-wms-vue/src/views/erp/ErpStockManagement.vue D:/project/auto-parts-wms-vue/src/views/erp/__tests__/erpOpeningStockImportEntry.test.mjs
git commit -m "feat: add opening stock excel import"
```

### Task 7: Run End-to-End Verification And Update Specs If Migration Decisions Changed

**Files:**
- Modify if needed: `D:/project/docs/superpowers/specs/2026-05-29-master-data-and-opening-stock-import-design.md`
- Inspect: backend and frontend changed files

- [ ] **Step 1: Run frontend targeted tests**

Run:

```bash
node --test D:/project/auto-parts-wms-vue/src/views/erp/__tests__/erpSupplierDialogRedesign.test.mjs
node --test D:/project/auto-parts-wms-vue/src/views/erp/__tests__/erpCustomerImportEntry.test.mjs
node --test D:/project/auto-parts-wms-vue/src/views/erp/__tests__/erpProductImportEntry.test.mjs
node --test D:/project/auto-parts-wms-vue/src/views/erp/__tests__/erpOpeningStockImportEntry.test.mjs
```

Expected: PASS

- [ ] **Step 2: Run frontend type check**

Run: `npm run type-check`

Expected: PASS

- [ ] **Step 3: Run backend targeted tests**

Run:

```bash
mvn -Dtest=ExcelImportParserTest,ErpFinanceWorkflowTests,ErpCustomerImportTests,ErpProductImportTests,ErpOpeningStockImportTests test
```

Expected: PASS

- [ ] **Step 4: Run backend package or full verification**

Run: `mvn test`

Expected: PASS，或记录与本次改动无关的已知失败项。

- [ ] **Step 5: Sync the spec if actual schema decisions changed during implementation**

```md
- 若新增了 migration，在 spec 的“数据库与迁移结论”中补充最终版本号和文件名
- 若客户/配件/库存实际未落批次表，而改为轻量结果返回，也同步修正文档
```

- [ ] **Step 6: Commit**

```bash
git add D:/project/docs/superpowers/specs/2026-05-29-master-data-and-opening-stock-import-design.md
git commit -m "docs: sync import design with implementation"
```

## Self-Review

### Spec coverage

- 供应商 Excel 上传：Task 3
- 客户 Excel 上传：Task 4
- 配件 Excel 上传：Task 5
- 库存期初导入：Task 6
- 共享 `.xls/.xlsx` 解析：Task 1
- migration 顺序检查与是否新增：Task 2
- 前端入口、测试、类型检查、后端测试：Task 3-7

### Placeholder scan

- 没有使用 `TODO`、`TBD` 或“后续补充”式占位。
- 所有变更步骤都给出了代码片段、测试命令和预期结果。

### Type consistency

- 统一使用 `importSuppliers/importCustomers/importProducts/importOpeningStock`
- 前端统一使用 `multipart/form-data`
- 库存导入明确限定为库存服务处理，不回写商品服务

