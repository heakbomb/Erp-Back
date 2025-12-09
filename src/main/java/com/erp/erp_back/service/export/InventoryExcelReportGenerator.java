// src/main/java/com/erp/erp_back/service/erp/export/InventoryExcelReportGenerator.java
package com.erp.erp_back.service.export;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import com.erp.erp_back.dto.erp.InventoryResponse;
import com.erp.erp_back.entity.enums.ActiveStatus;

@Component
public class InventoryExcelReportGenerator {

    private static final String SHEET_NAME = "재고현황";

    public byte[] generate(List<InventoryResponse> rows) {

        // 활성 재고만
        List<InventoryResponse> activeRows = rows.stream()
                .filter(r -> r.getStatus() == ActiveStatus.ACTIVE)
                .toList();
        
        try (Workbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream bos = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet(SHEET_NAME);

            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle numberStyle = createNumberStyle(workbook);

            int rowIdx = 0;

            // ===========================
            // 1) 헤더
            // ===========================
            Row header = sheet.createRow(rowIdx++);
            String[] headers = {
                    "품목ID", "품목명", "품목 유형", "재고 유형",
                    "재고 수량", "안전 재고", "상태", "최신 단가"
            };

            for (int i = 0; i < headers.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // ===========================
            // 2) 실제 데이터 rows
            // ===========================
            for (InventoryResponse rowData : activeRows) {
                Row row = sheet.createRow(rowIdx++);
                int col = 0;

                // 품목ID
                createNumericCell(row, col++, rowData.getItemId(), numberStyle);

                // 품목명
                createStringCell(row, col++, rowData.getItemName());

                // ⭐ 품목 유형 (한글 라벨)
                createStringCell(row, col++,
                        rowData.getItemType() != null ? rowData.getItemType().getLabelKo() : "");
                // 재고 유형
                createStringCell(row, col++, rowData.getStockType());

                // 재고 수량
                createBigDecimalCell(row, col++, rowData.getStockQty(), numberStyle);

                // 안전 재고
                createBigDecimalCell(row, col++, rowData.getSafetyQty(), numberStyle);

                // 상태
                createStringCell(row, col++,
                        rowData.getStatus() != null
                                ? rowData.getStatus().name()
                                : "");

                // 최신 단가
                createBigDecimalCell(row, col++, rowData.getLastUnitCost(), numberStyle);
            }

            // ===========================
            // 3) 컬럼 자동 너비 조정
            // ===========================
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(bos);
            return bos.toByteArray();

        } catch (IOException e) {
            throw new IllegalStateException("재고 엑셀 리포트 생성 중 오류", e);
        }
    }

    // ====== 스타일/셀 유틸 ======

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private CellStyle createNumberStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("#,##0.###"));
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private void createStringCell(Row row, int col, String value) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value != null ? value : "");
    }

    private void createNumericCell(Row row, int col, Long value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value != null ? value : 0);
        cell.setCellStyle(style);
    }

    private void createBigDecimalCell(Row row, int col, BigDecimal value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value != null ? value.doubleValue() : 0d);
        cell.setCellStyle(style);
    }
}
