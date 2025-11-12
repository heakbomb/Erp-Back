package com.erp.erp_back.repository.store;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.erp.erp_back.entity.store.Store;

@Repository
public interface StoreRepository extends JpaRepository<Store, Long>,JpaSpecificationExecutor<Store> {
    // ✅ 오너가 가진 사업장 목록 (store -> businessNumber -> owner 조인)
    @Query("""
           select s
           from Store s
             join s.businessNumber bn
             join bn.owner o
           where o.ownerId = :ownerId
           order by s.storeId asc
           """)
    List<Store> findAllByOwnerId(@Param("ownerId") Long ownerId);

    long countByStatus(String status);
}