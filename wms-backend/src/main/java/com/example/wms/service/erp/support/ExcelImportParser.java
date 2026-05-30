package com.example.wms.service.erp.support;

import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
public class ExcelImportParser {

    private final DataFormatter formatter = new DataFormatter();

    public ExcelImportSheet parse(String filename, byte[] content) {
        if (!isSupportedExcel(filename)) {
            throw new IllegalArgumentException("仅支持 .xls 或 .xlsx 文件");
        }
        if (content == null || content.length == 0) {
            throw new IllegalArgumentException("导入文件不能为空");
        }

        try (Workbook workbook = openWorkbook(content)) {
            Sheet sheet = requireFirstSheet(workbook);
            Row headerRow = requireHeaderRow(sheet);

            List<String> headers = readHeaders(headerRow);
            List<Map<String, String>> rows = new ArrayList<>();
            for (int rowIndex = sheet.getFirstRowNum() + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) {
                    continue;
                }

                Map<String, String> values = new LinkedHashMap<>();
                boolean blankRow = true;
                for (int cellIndex = 0; cellIndex < headers.size(); cellIndex++) {
                    String value = formatCell(row, cellIndex);
                    if (!value.isEmpty()) {
                        blankRow = false;
                    }
                    values.put(headers.get(cellIndex), value);
                }

                if (!blankRow) {
                    rows.add(values);
                }
            }
            return new ExcelImportSheet(headers, rows);
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (IOException ex) {
            throw invalidExcel(ex);
        } catch (RuntimeException ex) {
            throw invalidExcel(ex);
        }
    }

    private boolean isSupportedExcel(String filename) {
        if (filename == null) {
            return false;
        }
        String lowerCaseFilename = filename.toLowerCase(Locale.ROOT);
        return lowerCaseFilename.endsWith(".xls") || lowerCaseFilename.endsWith(".xlsx");
    }

    private List<String> readHeaders(Row headerRow) {
        int headerCount = Math.max(headerRow.getLastCellNum(), 0);
        if (headerCount == 0) {
            throw new IllegalArgumentException("Excel 缺少表头");
        }

        List<String> headers = new ArrayList<>(headerCount);
        Set<String> seenHeaders = new HashSet<>();
        for (int cellIndex = 0; cellIndex < headerCount; cellIndex++) {
            String header = formatCell(headerRow, cellIndex);
            if (header.isEmpty()) {
                throw new IllegalArgumentException("Excel 表头不能为空白");
            }
            if (!seenHeaders.add(header)) {
                throw new IllegalArgumentException("Excel 表头存在重复列：" + header);
            }
            headers.add(header);
        }
        return headers;
    }

    private String formatCell(Row row, int cellIndex) {
        return formatter.formatCellValue(row.getCell(cellIndex, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK)).trim();
    }

    private Workbook openWorkbook(byte[] content) throws IOException {
        try {
            return WorkbookFactory.create(new ByteArrayInputStream(content));
        } catch (IllegalArgumentException ex) {
            throw invalidExcel(ex);
        } catch (RuntimeException ex) {
            throw invalidExcel(ex);
        }
    }

    private Sheet requireFirstSheet(Workbook workbook) {
        if (workbook.getNumberOfSheets() <= 0) {
            throw new IllegalArgumentException("Excel 没有可读取的工作表");
        }
        return workbook.getSheetAt(0);
    }

    private Row requireHeaderRow(Sheet sheet) {
        Row headerRow = sheet.getRow(sheet.getFirstRowNum());
        if (headerRow == null) {
            throw new IllegalArgumentException("Excel 缺少表头");
        }
        return headerRow;
    }

    private IllegalArgumentException invalidExcel(Exception cause) {
        return new IllegalArgumentException("Excel 文件已损坏或格式不正确", cause);
    }
}
