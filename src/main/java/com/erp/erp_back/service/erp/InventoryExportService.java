// src/main/java/com/erp/erp_back/service/erp/InventoryExportService.java
package com.erp.erp_back.service.erp;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.erp.erp_back.dto.erp.InventoryResponse;
import com.erp.erp_back.repository.erp.InventoryRepository;
import com.erp.erp_back.service.export.InventoryExcelReportGenerator;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventoryExportService {

    private final InventoryRepository inventoryRepository;
    private final InventoryExcelReportGenerator excelGenerator;

    public byte[] exportExcel(Long storeId) {
        List<InventoryResponse> rows =
                inventoryRepository.findExportRowsByStoreId(storeId);

        return excelGenerator.generate(rows);
    }
}
