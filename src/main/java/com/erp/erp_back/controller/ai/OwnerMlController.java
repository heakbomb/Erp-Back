package com.erp.erp_back.controller.ai;


import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.erp.erp_back.dto.ai.ProfitForecastResponse;
import com.erp.erp_back.service.ai.OwnerMlForecastService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/owner/ml")
public class OwnerMlController {

    private final OwnerMlForecastService service;

    @GetMapping("/profit-forecast")
    public ResponseEntity<ProfitForecastResponse> profitForecast(
            @RequestParam Long storeId,
            @RequestParam int year,
            @RequestParam int month
    ) {
        return ResponseEntity.ok(service.getOrCreateProfitForecast(storeId, year, month));
    }
}