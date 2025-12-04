package com.erp.erp_back.entity.erp;

import java.io.Serializable;
import java.time.LocalDate;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InventorySnapshotId implements Serializable {
    private Long snapshotId;
    private LocalDate snapshotDate;
}