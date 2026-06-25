package com.enterprise.framework.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

/**
 * Apache POI-based Excel reader for data-driven testing.
 *
 * <p><b>Convention:</b> The first row of each sheet is treated as a header row.
 * Data rows start at row index 1. Column headers are used as map keys, making
 * test data self-documenting and resilient to column reordering.
 *
 * <p><b>Usage:</b>
 * <pre>
 *   List&lt;Map&lt;String, String&gt;&gt; users = ExcelUtils.readSheet("testdata/users.xlsx", "LoginData");
 *   users.forEach(row -&gt; System.out.println(row.get("username")));
 * </pre>
 *
 * <p><b>File location:</b> Files are resolved relative to {@code src/test/resources/}.
 */
public final class ExcelUtils {

    private static final Logger log = LogManager.getLogger(ExcelUtils.class);

    private ExcelUtils() { }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Reads all data rows from a named sheet.
     *
     * @param filePath  path to .xlsx file (relative to classpath root or absolute)
     * @param sheetName name of the sheet to read
     * @return list of row maps: key = column header, value = cell value as String
     */
    public static List<Map<String, String>> readSheet(String filePath, String sheetName) {
        log.info("Reading Excel file: {} | Sheet: {}", filePath, sheetName);
        List<Map<String, String>> data = new ArrayList<>();

        try (FileInputStream fis = openFile(filePath);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheet(sheetName);
            if (sheet == null) {
                throw new IllegalArgumentException(
                    "Sheet '" + sheetName + "' not found in: " + filePath);
            }

            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                log.warn("Sheet '{}' is empty — no headers found.", sheetName);
                return data;
            }

            List<String> headers = extractHeaders(headerRow);
            log.debug("Headers found: {}", headers);

            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null || isEmptyRow(row)) continue;

                Map<String, String> rowData = new LinkedHashMap<>();
                for (int c = 0; c < headers.size(); c++) {
                    Cell cell = row.getCell(c, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                    rowData.put(headers.get(c), getCellValueAsString(cell));
                }
                data.add(rowData);
            }

            log.info("Read {} data rows from sheet '{}'", data.size(), sheetName);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read Excel file: " + filePath, e);
        }

        return data;
    }

    /**
     * Reads all data rows from the first sheet.
     */
    public static List<Map<String, String>> readFirstSheet(String filePath) {
        try (FileInputStream fis = openFile(filePath);
             Workbook workbook = new XSSFWorkbook(fis)) {
            Sheet firstSheet = workbook.getSheetAt(0);
            return readSheet(filePath, firstSheet.getSheetName());
        } catch (IOException e) {
            throw new RuntimeException("Failed to open Excel file: " + filePath, e);
        }
    }

    /**
     * Converts an Excel sheet to a 2D Object array for use with TestNG @DataProvider.
     *
     * <p>Column 0 of the returned array is excluded (assumed to be a row label).
     *
     * <p><b>Usage:</b>
     * <pre>
     *   @DataProvider
     *   public Object[][] loginData() {
     *       return ExcelUtils.toTestNgDataProvider("testdata/login.xlsx", "Login");
     *   }
     * </pre>
     */
    public static Object[][] toTestNgDataProvider(String filePath, String sheetName) {
        List<Map<String, String>> rows = readSheet(filePath, sheetName);
        Object[][] data = new Object[rows.size()][1];
        for (int i = 0; i < rows.size(); i++) {
            data[i][0] = rows.get(i);
        }
        return data;
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private static List<String> extractHeaders(Row headerRow) {
        List<String> headers = new ArrayList<>();
        for (Cell cell : headerRow) {
            String header = getCellValueAsString(cell).trim();
            if (!header.isEmpty()) {
                headers.add(header);
            }
        }
        return headers;
    }

    private static boolean isEmptyRow(Row row) {
        for (Cell cell : row) {
            if (cell.getCellType() != CellType.BLANK && !getCellValueAsString(cell).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING   -> cell.getStringCellValue();
            case NUMERIC  -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield cell.getLocalDateTimeCellValue().toString();
                }
                double d = cell.getNumericCellValue();
                // Return integer representation for whole numbers
                yield (d == Math.floor(d)) ? String.valueOf((long) d) : String.valueOf(d);
            }
            case BOOLEAN  -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA  -> cell.getCachedFormulaResultType() == CellType.NUMERIC
                ? String.valueOf(cell.getNumericCellValue())
                : cell.getStringCellValue();
            default       -> "";
        };
    }

    private static FileInputStream openFile(String filePath) throws IOException {
        // Try classpath first, then absolute path
        var classloaderPath = ExcelUtils.class.getClassLoader().getResource(filePath);
        String resolvedPath = (classloaderPath != null) ? classloaderPath.getPath() : filePath;
        return new FileInputStream(resolvedPath);
    }
}
