// src/main/java/com/erp/erp_back/controller/erp/RecipeIngredientController.java
package com.erp.erp_back.controller.erp;

import com.erp.erp_back.dto.erp.RecipeIngredientRequest;
import com.erp.erp_back.dto.erp.RecipeIngredientResponse;
import com.erp.erp_back.dto.erp.RecipeIngredientUpdateRequest;
import com.erp.erp_back.service.erp.RecipeIngredientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/owner/menu")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
public class RecipeIngredientController {

    private final RecipeIngredientService recipeIngredientService;

    /** 레시피 목록: GET /owner/menu/{menuId}/recipeIngredients */
    @GetMapping("/{menuId}/recipeIngredients")
    public ResponseEntity<List<RecipeIngredientResponse>> listByMenu(@PathVariable Long menuId) {
        return ResponseEntity.ok(recipeIngredientService.listByMenu(menuId));
    }

    /** 레시피 등록: POST /owner/menu/{menuId}/recipeIngredients */
    @PostMapping("/{menuId}/recipeIngredients")
    public ResponseEntity<RecipeIngredientResponse> create(
            @PathVariable Long menuId,
            @Valid @RequestBody RecipeIngredientRequest req
    ) {
        // path의 menuId를 신뢰(프론트가 body에 안 넣어도 동작)
        req.setMenuId(menuId);
        RecipeIngredientResponse created = recipeIngredientService.createRecipe(req);
        URI location = URI.create(String.format("/owner/menu/%d/recipeIngredients", menuId));
        return ResponseEntity.created(location).body(created);
    }

    /** 레시피 수정(소모수량): PATCH /owner/menu/recipeIngredients/{recipeId} */
    @PatchMapping("/recipeIngredients/{recipeId}")
    public ResponseEntity<RecipeIngredientResponse> updateQty(
            @PathVariable Long recipeId,
            @Valid @RequestBody RecipeIngredientUpdateRequest req
    ) {
        return ResponseEntity.ok(recipeIngredientService.updateRecipe(recipeId, req));
    }

    /** 레시피 삭제: DELETE /owner/menu/recipeIngredients/{recipeId} */
    @DeleteMapping("/recipeIngredients/{recipeId}")
    public ResponseEntity<Void> delete(@PathVariable Long recipeId) {
        recipeIngredientService.deleteRecipe(recipeId);
        return ResponseEntity.noContent().build();
    }
}
