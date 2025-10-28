package com.erp.erp_back.controller;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.erp.erp_back.dto.erp.MenuItemRequest;
import com.erp.erp_back.dto.erp.MenuItemResponse;
import com.erp.erp_back.service.MenuItemService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/menus") // POST /menus, GET /menus/{id}
@RequiredArgsConstructor
@Validated
public class MenuItemController {

    private final MenuItemService menuItemService;

    // 생성
    @PostMapping
    public ResponseEntity<MenuItemResponse> create(@Valid @RequestBody MenuItemRequest req) {
        MenuItemResponse res = menuItemService.createMenu(req /*, 로그인 미구현: ownerId 생략 */);
        return ResponseEntity
                .created(URI.create("/menus/" + res.getMenuId())) // 201 + Location
                .body(res);
    }

    @GetMapping("/{menuId}")
    public MenuItemResponse getOne(@PathVariable Long menuId) {
        // 서비스에 findById가 있으면 사용, 없으면 레포 직접 호출도 가능
        // 여기선 간단히 레포를 서비스가 감싸고 있다고 가정
        return menuItemService.findMenuById(menuId);
    }

    // ===== 수정 =====
    @PatchMapping("/{menuId}")
    public MenuItemResponse update(@PathVariable Long menuId,
                                   @RequestBody @jakarta.validation.Valid MenuItemRequest req) {
        return menuItemService.updateMenu(menuId, req);
    }

    // ===== 삭제 =====
    @DeleteMapping("/{menuId}")
    public org.springframework.http.ResponseEntity<Void> delete(@PathVariable Long menuId) {
        menuItemService.deleteMenu(menuId);
        return org.springframework.http.ResponseEntity.noContent().build(); // 204
    }

}
