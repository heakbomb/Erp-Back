package com.erp.erp_back.controller.erp;

import com.erp.erp_back.dto.erp.InventoryRequest;
import com.erp.erp_back.dto.erp.InventoryResponse;
import com.erp.erp_back.entity.enums.ActiveStatus;
import com.erp.erp_back.service.erp.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/owner/inventory")
@RequiredArgsConstructor
@Validated
@CrossOrigin(origins = "http://localhost:3000")
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping
    public ResponseEntity<Page<InventoryResponse>> getInventoryPage(
            @RequestParam Long storeId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) ActiveStatus status,
            @PageableDefault(size = 10, sort = "itemName") Pageable pageable
    ) {
        Page<InventoryResponse> page = inventoryService.getInventoryPage(storeId, q, status, pageable);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{itemId}")
    public ResponseEntity<InventoryResponse> getInventory(
            @PathVariable Long itemId,
            @RequestParam Long storeId
    ) {
        return ResponseEntity.ok(inventoryService.getInventory(storeId, itemId));
    }

    @PostMapping
    public ResponseEntity<InventoryResponse> createInventory(
            @Valid @RequestBody InventoryRequest req
    ) {
        InventoryResponse created = inventoryService.createInventory(req);
        URI location = URI.create(String.format("/owner/inventory/%d?storeId=%d",
                created.getItemId(), created.getStoreId()));
        return ResponseEntity.created(location).body(created);
    }

    @PatchMapping("/{itemId}")
    public ResponseEntity<InventoryResponse> updateInventory(
            @PathVariable Long itemId,
            @RequestParam Long storeId,
            @Valid @RequestBody InventoryRequest req
    ) {
        InventoryResponse updated = inventoryService.updateInventory(storeId, itemId, req);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{itemId}/deactivate")
    public ResponseEntity<Void> deactivate(
            @PathVariable Long itemId,
            @RequestParam Long storeId
    ) {
        inventoryService.deactivate(storeId, itemId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{itemId}/reactivate")
    public ResponseEntity<Void> reactivate(
            @PathVariable Long itemId,
            @RequestParam Long storeId
    ) {
        inventoryService.reactivate(storeId, itemId);
        return ResponseEntity.noContent().build();
    }
}
