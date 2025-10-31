package com.erp.erp_back.controller.erp;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.erp.erp_back.dto.erp.RecipeIngredientRequest;
import com.erp.erp_back.dto.erp.RecipeIngredientResponse;
import com.erp.erp_back.dto.erp.RecipeIngredientUpdateRequest;
import com.erp.erp_back.service.erp.RecipeIngredientService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/owner/menu")
@RequiredArgsConstructor
@Validated
@CrossOrigin(origins = "http://localhost:3000")
public class RecipeIngredientController {

    private final RecipeIngredientService recipeIngredientService;

    // 레시피 목록 조회: GET /owner/menu/{menuId}/recipeIngredients
    @GetMapping("/{menuId}/recipeIngredients")
    public ResponseEntity<List<RecipeIngredientResponse>> listByMenu(
            @PathVariable("menuId") Long menuId) {
        return ResponseEntity.ok(recipeIngredientService.listByMenu(menuId));
    }

    // 레시피 등록: POST /owner/menu/{menuId}/recipeIngredients
    @PostMapping("/{menuId}/recipeIngredients")
    public ResponseEntity<RecipeIngredientResponse> create(
            @PathVariable("menuId") Long menuId,
            @Valid @RequestBody RecipeIngredientRequest req) {

        if (req.getMenuId() == null) {
            req.setMenuId(menuId);
        } else if (!menuId.equals(req.getMenuId())) {
            throw new IllegalArgumentException("MENU_ID_MISMATCH_BETWEEN_PATH_AND_BODY");
        }

        RecipeIngredientResponse saved = recipeIngredientService.createRecipe(req);
        return ResponseEntity.status(201).body(saved);
    }

    // 레시피 수정(소모 수량만): PATCH /owner/menu/recipeIngredients/{recipeId}
    @PatchMapping("/recipeIngredients/{recipeId}")
    public ResponseEntity<RecipeIngredientResponse> update(
            @PathVariable("recipeId") Long recipeId,
            @Valid @RequestBody RecipeIngredientUpdateRequest req) {
        return ResponseEntity.ok(recipeIngredientService.updateRecipe(recipeId, req));
    }

    // 레시피 삭제: DELETE /owner/menu/recipeIngredients/{recipeId}
    @DeleteMapping("/recipeIngredients/{recipeId}")
    public ResponseEntity<Void> delete(
            @PathVariable("recipeId") Long recipeId) {
        recipeIngredientService.deleteRecipe(recipeId);
        return ResponseEntity.noContent().build();
    }
    
}
