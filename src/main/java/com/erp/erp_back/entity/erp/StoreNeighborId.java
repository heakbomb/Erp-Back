package com.erp.erp_back.entity.erp;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Embeddable
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class StoreNeighborId implements Serializable {

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "neighbor_store_id", nullable = false)
    private Long neighborStoreId;

    @Column(name = "radius_m", nullable = false)
    private Integer radiusM;
}