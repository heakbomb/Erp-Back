package com.erp.erp_back.controller.erp;

import com.erp.erp_back.service.erp.StoreNeighborBuildService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/store-neighbors")
public class StoreNeighborAdminController {

    private final StoreNeighborBuildService buildService;

    @PostMapping("/rebuild-all")
    public ResponseEntity<Map<String, Object>> rebuildAll(
            @RequestParam(defaultValue = "2000") int radiusM
    ) {
        long affected = buildService.rebuildAll(radiusM);
        return ResponseEntity.ok(Map.of(
                "radiusM", radiusM,
                "affectedRows", affected
        ));
    }

    @PostMapping("/rebuild")
    public ResponseEntity<Map<String, Object>> rebuildOne(
            @RequestParam Long storeId,
            @RequestParam(defaultValue = "2000") int radiusM,
            @RequestParam(defaultValue = "false") boolean deleteBefore
    ) {
        int affected = buildService.rebuildForStore(storeId, radiusM, deleteBefore);
        return ResponseEntity.ok(Map.of(
                "storeId", storeId,
                "radiusM", radiusM,
                "deleteBefore", deleteBefore,
                "affectedRows", affected
        ));
    }
}
