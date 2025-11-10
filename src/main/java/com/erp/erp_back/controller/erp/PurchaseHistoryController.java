// src/main/java/com/erp/erp_back/controller/erp/PurchaseHistoryController.java
package com.erp.erp_back.controller.erp;

import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.erp.erp_back.dto.erp.PurchaseHistoryRequest;
import com.erp.erp_back.dto.erp.PurchaseHistoryUpdateRequest;
import com.erp.erp_back.dto.erp.PurchaseHistoryResponse;
import com.erp.erp_back.service.erp.PurchaseHistoryService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/owner/purchases")
@RequiredArgsConstructor
@Validated
@CrossOrigin(origins = "http://localhost:3000")
public class PurchaseHistoryController {

    private final PurchaseHistoryService purchaseHistoryService;

    /* 생성 */
    @PostMapping
    public ResponseEntity<PurchaseHistoryResponse> createPurchaseHistory(
            @Valid @RequestBody PurchaseHistoryRequest req) {
        return ResponseEntity.status(201).body(purchaseHistoryService.createPurchase(req));
    }

    /* 단건 조회 */
    @GetMapping("/{purchaseId}")
    public ResponseEntity<PurchaseHistoryResponse> getPurchaseHistory(@PathVariable @Min(1) Long purchaseId) {
        return ResponseEntity.ok(purchaseHistoryService.getPurchase(purchaseId));
    }

    /* 목록: storeId 필수, itemId/from/to 옵션 */
    @GetMapping
    public ResponseEntity<Page<PurchaseHistoryResponse>> listPurchaseHistories(
            @RequestParam("storeId") @Min(1) Long storeId,
            @RequestParam(value = "itemId", required = false) Long itemId,
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false)   @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @PageableDefault(size = 10, sort = "purchaseDate") Pageable pageable
    ) {
        return ResponseEntity.ok(purchaseHistoryService.listPurchase(storeId, itemId, from, to, pageable));
    }

    /* 수정 (전체 필드 필요: qty, unitPrice, purchaseDate) */
    @PatchMapping("/{purchaseId}")
    public ResponseEntity<PurchaseHistoryResponse> updatePurchaseHistory(
            @PathVariable @Min(1) Long purchaseId,
            @Valid @RequestBody PurchaseHistoryUpdateRequest req
    ) {
        return ResponseEntity.ok(purchaseHistoryService.updatePurchase(purchaseId, req));
    }
}
