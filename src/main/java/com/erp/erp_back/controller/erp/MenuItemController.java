package com.erp.erp_back.controller.erp;

import java.net.URI;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.erp.erp_back.dto.erp.MenuItemRequest;
import com.erp.erp_back.dto.erp.MenuItemResponse;
import com.erp.erp_back.service.erp.MenuItemService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;


@RestController
@RequestMapping("/owner/menu") 
@RequiredArgsConstructor
@Validated
@CrossOrigin(origins = "http://localhost:3000") // 프론트(3000)에서 호출 허용
public class MenuItemController {

    private final MenuItemService menuItemService;

    // 생성: POST /owner/menu
    @PostMapping
    public ResponseEntity<MenuItemResponse> create(@Valid @RequestBody MenuItemRequest req) {
        MenuItemResponse res = menuItemService.createMenu(req /* 인증 미적용: ownerId 생략 */);
        return ResponseEntity
                .created(URI.create("/owner/menu/" + res.getMenuId())) // 201 + Location
                .body(res);
    }

    // 목록: GET /owner/menu?storeId=10&q=ame&page=0&size=20&sort=menuName,asc
    @GetMapping
    public Page<MenuItemResponse> list(@RequestParam Long storeId,
                                       @RequestParam(required = false) String q,
                                       @PageableDefault(size = 20, sort = "menuName") Pageable pageable) {
        return menuItemService.list(storeId, q, pageable);
    }

    // 단건 조회: GET /owner/menu/{menuId}
    @GetMapping("/{menuId}")
    public MenuItemResponse getOne(@PathVariable Long menuId) {
        return menuItemService.findMenuById(menuId);
    }

    // 수정: PATCH /owner/menu/{menuId}
    @PatchMapping("/{menuId}")
    public MenuItemResponse update(@PathVariable Long menuId,
                                   @Valid @RequestBody MenuItemRequest req) {
        return menuItemService.updateMenu(menuId, req);
    }

    // 삭제: DELETE /owner/menu/{menuId}
    @DeleteMapping("/{menuId}")
    public ResponseEntity<Void> delete(@PathVariable Long menuId) {
        menuItemService.deleteMenu(menuId);
        return ResponseEntity.noContent().build(); // 204
    }
}