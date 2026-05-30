package com.example.wms.service.erp.support;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExcelImportParserTest {

    private final ExcelImportParser parser = new ExcelImportParser();

    @Test
    void parsesXlsWorkbookIntoHeadersAndRows() throws Exception {
        byte[] content = workbookBytes(new HSSFWorkbook());

        ExcelImportSheet sheet = parser.parse("supplier.xls", content);

        assertThat(sheet).isNotNull();
        assertThat(sheet.headers()).containsExactly("编码", "名称", "备注");
        assertThat(sheet.rows()).hasSize(2);
        assertThat(sheet.rows()).containsExactly(
            Map.of("编码", "SUP-001", "名称", "火花塞供应商", "备注", "首行"),
            Map.of("编码", "SUP-002", "名称", "滤芯供应商", "备注", "")
        );
        assertThat(sheet.rows().get(0))
            .containsEntry("编码", "SUP-001")
            .containsEntry("名称", "火花塞供应商")
            .containsEntry("备注", "首行");
        assertThat(sheet.rows().get(1))
            .containsEntry("编码", "SUP-002")
            .containsEntry("名称", "滤芯供应商")
            .containsEntry("备注", "");
    }

    @Test
    void parsesXlsxWorkbookAndSkipsBlankRows() throws Exception {
        byte[] content = workbookBytes(new XSSFWorkbook());

        ExcelImportSheet sheet = parser.parse("supplier.xlsx", content);

        assertThat(sheet.headers()).containsExactly("编码", "名称", "备注");
        assertThat(sheet.rows()).hasSize(2);
        assertThat(sheet.rows().get(1))
            .containsEntry("编码", "SUP-002")
            .containsEntry("名称", "滤芯供应商")
            .containsEntry("备注", "");
    }

    @Test
    void rejectsUnsupportedExtension() {
        assertThatThrownBy(() -> parser.parse("supplier.csv", new byte[] {1, 2, 3}))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining(".xls")
            .hasMessageContaining(".xlsx");
    }

    @Test
    void rejectsEmptyFileContent() {
        assertThatThrownBy(() -> parser.parse("supplier.xlsx", new byte[0]))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("导入文件不能为空")
            .hasNoCause();
    }

    @Test
    void rejectsWorkbookWithoutSheets() throws Exception {
        byte[] content = workbookBytes(new XSSFWorkbook(), workbook -> {
        });

        assertThatThrownBy(() -> parser.parse("supplier.xlsx", content))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Excel 没有可读取的工作表");
    }

    @Test
    void rejectsBlankHeaderNames() throws Exception {
        byte[] content = workbookBytes(new XSSFWorkbook(), workbook -> {
            Row header = workbook.createSheet("Sheet1").createRow(0);
            header.createCell(0).setCellValue("   ");
            header.createCell(1).setCellValue("名称");
        });

        assertThatThrownBy(() -> parser.parse("supplier.xlsx", content))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Excel 表头不能为空白");
    }

    @Test
    void rejectsDuplicateHeaderNames() throws Exception {
        byte[] content = workbookBytes(new XSSFWorkbook(), workbook -> {
            Row header = workbook.createSheet("Sheet1").createRow(0);
            header.createCell(0).setCellValue("编码");
            header.createCell(1).setCellValue("名称");
            header.createCell(2).setCellValue(" 编码 ");
        });

        assertThatThrownBy(() -> parser.parse("supplier.xlsx", content))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Excel 表头存在重复列：编码");
    }

    @Test
    void normalizesCorruptedWorkbookContent() {
        assertThatThrownBy(() -> parser.parse("supplier.xlsx", new byte[] {1, 2, 3, 4}))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Excel 文件已损坏或格式不正确");
    }

    @Test
    void normalizesPoiRuntimeExceptions() throws Exception {
        byte[] content = fakeXlsxBytes();

        assertThatThrownBy(() -> parser.parse("supplier.xlsx", content))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Excel 文件已损坏或格式不正确");
    }

    private byte[] workbookBytes(Workbook workbook) throws IOException {
        return workbookBytes(workbook, this::fillDefaultSheet);
    }

    private byte[] workbookBytes(Workbook workbook, Consumer<Workbook> customizer) throws IOException {
        try (workbook; ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            customizer.accept(workbook);
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private void fillDefaultSheet(Workbook workbook) {
        Row header = workbook.createSheet("Sheet1").createRow(0);
        header.createCell(0).setCellValue(" 编码 ");
        header.createCell(1).setCellValue("名称");
        header.createCell(2).setCellValue("备注");

        Row firstRow = workbook.getSheetAt(0).createRow(1);
        firstRow.createCell(0).setCellValue("SUP-001");
        firstRow.createCell(1).setCellValue("火花塞供应商");
        firstRow.createCell(2).setCellValue("首行");

        workbook.getSheetAt(0).createRow(2);

        Row secondRow = workbook.getSheetAt(0).createRow(3);
        secondRow.createCell(0).setCellValue("SUP-002");
        secondRow.createCell(1).setCellValue("滤芯供应商");
        secondRow.createCell(2).setBlank();
    }

    private byte[] fakeXlsxBytes() throws IOException {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream)) {
            zipOutputStream.putNextEntry(new ZipEntry("[Content_Types].xml"));
            zipOutputStream.write("not-xml".getBytes());
            zipOutputStream.closeEntry();
            zipOutputStream.putNextEntry(new ZipEntry("xl/workbook.xml"));
            zipOutputStream.write("still-not-xml".getBytes());
            zipOutputStream.closeEntry();
            zipOutputStream.finish();
            return outputStream.toByteArray();
        }
    }
}
