package com.erp.erp_back.repository.store;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.erp.erp_back.entity.store.Store;

@Repository
public interface StoreRepository extends JpaRepository<Store, Long> {

    List<Store> findByBusinessNumber_Owner_OwnerId(Long ownerId);

    List<Store> findByStatus(String status);

    @Query("SELECT s FROM Store s JOIN s.businessNumber bn " +
           "WHERE (:status = 'ALL' OR s.status = :status) " +
           "AND (:q = '' OR s.storeName LIKE %:q% OR bn.bizNum LIKE %:q%)")
    Page<Store> findAdminStores(
            @Param("status") String status,
            @Param("q") String q,
            Pageable pageable
    );
}