// src/main/java/com/erp/erp_back/entity/sales/StoreNeighbor.java
package com.erp.erp_back.entity.erp;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "store_neighbor")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreNeighbor {

    @EmbeddedId
    private StoreNeighborId id;

    @Column(name = "distance_m", nullable = false)
    private Integer distanceM;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}