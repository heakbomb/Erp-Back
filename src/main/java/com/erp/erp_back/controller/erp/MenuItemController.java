package com.erp.erp_back.controller.erp;

import com.erp.erp_back.dto.erp.MenuItemRequest;
import com.erp.erp_back.dto.erp.MenuItemResponse;
import com.erp.erp_back.dto.erp.MenuStatsResponse;
import com.erp.erp_back.entity.enums.ActiveStatus;
import com.erp.erp_back.service.erp.MenuItemService;
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
@RequestMapping("/owner/menu")
@RequiredArgsConstructor
@Validated
@CrossOrigin(origins = "http://localhost:3000")
public class MenuItemController {

    private final MenuItemService menuItemService;

    /** 목록: /owner/menu?storeId=11&q=아메리카노&status=ACTIVE&page=0&size=20&sort=menuName,asc */
    @GetMapping
    public ResponseEntity<Page<MenuItemResponse>> getMenuPage(
            @RequestParam Long storeId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) ActiveStatus status,
            @PageableDefault(size = 20, sort = "menuName") Pageable pageable
    ) {
        Page<MenuItemResponse> page = menuItemService.getMenuPage(storeId, q, status, pageable);
        return ResponseEntity.ok(page);
    }

    /** 단건 조회: /owner/menu/{menuId}?storeId=11 */
    @GetMapping("/{menuId}")
    public ResponseEntity<MenuItemResponse> getMenu(
            @PathVariable Long menuId,
            @RequestParam Long storeId
    ) {
        return ResponseEntity.ok(menuItemService.getMenu(storeId, menuId));
    }

    /** 생성: POST /owner/menu */
    @PostMapping
    public ResponseEntity<MenuItemResponse> createMenu(
            @Valid @RequestBody MenuItemRequest req
    ) {
        MenuItemResponse created = menuItemService.createMenu(req);
        URI location = URI.create(String.format("/owner/menu/%d?storeId=%d",
                created.getMenuId(), created.getStoreId()));
        return ResponseEntity.created(location).body(created);
    }

    /** 수정: PATCH /owner/menu/{menuId}?storeId=11 */
    @PatchMapping("/{menuId}")
    public ResponseEntity<MenuItemResponse> updateMenu(
            @PathVariable Long menuId,
            @RequestParam Long storeId,
            @Valid @RequestBody MenuItemRequest req
    ) {
        MenuItemResponse updated = menuItemService.updateMenu(storeId, menuId, req);
        return ResponseEntity.ok(updated);
    }

    /** 비활성화: POST /owner/menu/{menuId}/deactivate?storeId=11 */
    @PostMapping("/{menuId}/deactivate")
    public ResponseEntity<Void> deactivate(
            @PathVariable Long menuId,
            @RequestParam Long storeId
    ) {
        menuItemService.deactivate(storeId, menuId);
        return ResponseEntity.noContent().build();
    }

    /** 비활성화 해제: POST /owner/menu/{menuId}/reactivate?storeId=11 */
    @PostMapping("/{menuId}/reactivate")
    public ResponseEntity<Void> reactivate(
            @PathVariable Long menuId,
            @RequestParam Long storeId
    ) {
        menuItemService.reactivate(storeId, menuId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/stats")
    public ResponseEntity<MenuStatsResponse> getMenuStats(
            @RequestParam Long storeId
    ) {
        return ResponseEntity.ok(menuItemService.getMenuStats(storeId));
    }
    
}
