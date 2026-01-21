package com.erp.erp_back.controller.erp;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.erp.erp_back.dto.erp.PriceCompetitivenessResponse;
import com.erp.erp_back.service.erp.OwnerPriceCompetitivenessService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/owner/area")
public class PriceCompetitivenessController {

    private final OwnerPriceCompetitivenessService service;

    @GetMapping("/price-competitiveness")
    public ResponseEntity<PriceCompetitivenessResponse> analyze(
            @RequestParam("storeId") Long storeId,
            @RequestParam(value = "radiusM", defaultValue = "2000") int radiusM,
            @RequestParam(value = "onlyActive", defaultValue = "true") boolean onlyActive
    ) {
        return ResponseEntity.ok(service.analyze(storeId, radiusM, onlyActive));
    }
}
