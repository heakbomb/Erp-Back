// com.erp.erp_back.dto.erp.MenuItemRequest.java
package com.erp.erp_back.dto.erp;

import java.math.BigDecimal;

import com.erp.erp_back.entity.enums.ActiveStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class MenuItemRequest {

    @NotNull
    private Long storeId;

    @NotBlank
    @Size(max = 100)
    private String menuName;

    @NotNull
    @Positive
    private BigDecimal price;

    private ActiveStatus status;

    // ✅ [추가] 중분류/소분류 (무조건 선택 = NOT NULL)
    private String categoryName;
    private String subCategoryName;
}