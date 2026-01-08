package com.erp.erp_back.repository.erp;

import com.erp.erp_back.entity.erp.StoreNeighbor;
import com.erp.erp_back.entity.erp.StoreNeighborId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StoreNeighborRepository extends JpaRepository<StoreNeighbor, StoreNeighborId> {

    @Query("""
                select sn.id.neighborStoreId
                from StoreNeighbor sn
                where sn.id.storeId = :storeId
                  and sn.id.radiusM = :radiusM
                order by sn.distanceM asc
            """)
    List<Long> findNeighborStoreIds(@Param("storeId") Long storeId, @Param("radiusM") Integer radiusM);

    List<StoreNeighbor> findByIdStoreIdAndIdRadiusM(Long storeId, int radiusM);
}