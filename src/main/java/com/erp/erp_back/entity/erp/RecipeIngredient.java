package com.erp.erp_back.entity.erp;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "RecipeIngredient",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_recipeingredient_menu_item",
            columnNames = {"menu_id", "item_id"}
        )
    },
    indexes = {
        @Index(name = "ix_recipeingredient_menu", columnList = "menu_id"),
        @Index(name = "ix_recipeingredient_item", columnList = "item_id")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecipeIngredient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "recipe_id")
    private Long recipeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_id", nullable = false)
    private MenuItem menuItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private Inventory inventory;

    @Column(name = "consumption_qty", nullable = false, precision = 10, scale = 3)
    private BigDecimal consumptionQty;
}