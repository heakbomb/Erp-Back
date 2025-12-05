// com.erp.erp_back.dto.erp.InventoryRequest.java
package com.erp.erp_back.dto.erp;

import java.math.BigDecimal;

import com.erp.erp_back.entity.enums.ActiveStatus;
import com.erp.erp_back.entity.enums.IngredientCategory;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class InventoryRequest {

    @NotNull(message = "storeId는 필수입니다.")
    private Long storeId;

    @NotBlank(message = "품목명은 필수입니다.")
    @Size(max = 100)
    private String itemName;

    @NotNull(message = "품목 타입은 필수입니다.")
    private IngredientCategory itemType;

    @NotBlank(message = "수량 타입은 필수입니다.")
    @Size(max = 20)
    private String stockType;

    @NotNull(message = "현재 수량은 필수입니다.")
    @PositiveOrZero(message = "현재 수량은 0 이상이어야 합니다.")
    private BigDecimal stockQty;

    @NotNull(message = "안전 재고량은 필수입니다.")
    @PositiveOrZero(message = "안전 재고량은 0 이상이어야 합니다.")
    private BigDecimal safetyQty;

    /** 선택: 미전달 시 서비스에서 ACTIVE로 기본 처리 */
    private ActiveStatus status;
}
