package com.erp.erp_back.controller.erp;

import java.net.URI;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.erp.erp_back.dto.erp.InventoryAdjustRequest;
import com.erp.erp_back.dto.erp.InventoryRequest;
import com.erp.erp_back.dto.erp.InventoryResponse;
import com.erp.erp_back.service.erp.InventoryService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/owner/inventory") // ▶ http://localhost:8080/owner/inventory
@RequiredArgsConstructor
@Validated
@CrossOrigin(origins = "http://localhost:3000")
public class InventoryController {

    private final InventoryService inventoryService;

    /** =============================
     *  재고 등록 (Create)
     *  POST /owner/inventory
     *  Body: InventoryRequest { storeId, itemName, itemType, stockType, stockQty, safetyQty, expiryDate? }
     *  응답: 201 + Location 헤더, InventoryResponse
     * ============================== */
    @PostMapping
    public ResponseEntity<InventoryResponse> create(@Valid @RequestBody InventoryRequest req) {
        InventoryResponse res = inventoryService.createInventory(req /*, ownerId 생략 */);
        return ResponseEntity
                .created(URI.create("/owner/inventory/" + res.getItemId()))
                .body(res);
    }

    /** ========== (옵션) 목록 조회 ==========
     * GET /owner/inventory?storeId=10&q=bean&page=0&size=20&sort=itemName,asc
     */
    @GetMapping
    public Page<InventoryResponse> list(@RequestParam Long storeId,
                                        @RequestParam(required = false) String q,
                                        @PageableDefault(size = 10, sort = "itemName") Pageable pageable) {
        return inventoryService.list(storeId, q, pageable);
    }

    /** ========== (옵션) 단건 조회 ==========
     * GET /owner/inventory/{itemId}
     */
    @GetMapping("/{itemId}")
    public InventoryResponse getOne(@PathVariable Long itemId) {
        return inventoryService.findInventoryById(itemId);
    }

    /** ========== (옵션) 수정 ==========
     * PATCH /owner/inventory/{itemId}
     * Body: InventoryRequest (storeId는 동일 매장만 허용)
     */
    @PatchMapping("/{itemId}")
    public InventoryResponse update(@PathVariable Long itemId,
                                    @Valid @RequestBody InventoryRequest req) {
        return inventoryService.updateInventory(itemId, req);
    }

    /** ========== (옵션) 수량 증감(입고/차감) ==========
     * PATCH /owner/inventory/{itemId}/adjust
     * Body: InventoryAdjustRequest { deltaQty }
     */
    @PatchMapping("/{itemId}/adjust")
    public InventoryResponse adjust(@PathVariable Long itemId,
                                    @Valid @RequestBody InventoryAdjustRequest req) {
        return inventoryService.adjustQty(itemId, req);
    }

    /** ========== (옵션) 삭제 ==========
     * DELETE /owner/inventory/{itemId}
     */
    @DeleteMapping("/{itemId}")
    public ResponseEntity<Void> delete(@PathVariable Long itemId) {
        inventoryService.deleteInventory(itemId);
        return ResponseEntity.noContent().build();
    }

    /** ========== (옵션) 다건 삭제 ==========
     * DELETE /owner/inventory?ids=1,2,3
     */
    @DeleteMapping(params = "ids")
    public ResponseEntity<Void> deleteBulk(@RequestParam java.util.List<Long> ids) {
        inventoryService.deleteBulk(ids);
        return ResponseEntity.noContent().build();
    }
}
