package com.erp.erp_back.repository.hr;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.erp.erp_back.entity.hr.EmployeeDocument;

@Repository
public interface EmployeeDocumentRepository extends JpaRepository<EmployeeDocument, Long> {
    // --- 1. (유지) 검색어가 없을 때 사용할 기존 메서드 ---
    Page<EmployeeDocument> findAllByStore_StoreId(Long storeId, Pageable pageable);
    Page<EmployeeDocument> findAllByStore_StoreIdAndRetentionEndDateBefore(Long storeId, LocalDate today, Pageable pageable);
    Page<EmployeeDocument> findAllByStore_StoreIdAndRetentionEndDateGreaterThanEqual(Long storeId, LocalDate today, Pageable pageable);

    // --- 2. [수정] 검색어가 있을 때 사용할 @Query 메서드 (docType 검색 제거) ---

    /** (ALL + Search) */
    @Query("SELECT d FROM EmployeeDocument d " +
           "WHERE d.store.storeId = :storeId " +
           "AND d.originalFilename LIKE :search") // [수정] docType 검색 제거
    Page<EmployeeDocument> findByStoreIdAndSearch(
        @Param("storeId") Long storeId, 
        @Param("search") String search, 
        Pageable pageable
    );

    /** (EXPIRED + Search) */
    @Query("SELECT d FROM EmployeeDocument d " +
           "WHERE d.store.storeId = :storeId AND d.retentionEndDate < :today " +
           "AND d.originalFilename LIKE :search") // [수정] docType 검색 제거
    Page<EmployeeDocument> findByStoreIdAndExpiredAndSearch(
        @Param("storeId") Long storeId, 
        @Param("today") LocalDate today, 
        @Param("search") String search, 
        Pageable pageable
    );

    /** (ACTIVE + Search) */
    @Query("SELECT d FROM EmployeeDocument d " +
           "WHERE d.store.storeId = :storeId AND d.retentionEndDate >= :today " +
           "AND d.originalFilename LIKE :search") // [수정] docType 검색 제거
    Page<EmployeeDocument> findByStoreIdAndActiveAndSearch(
        @Param("storeId") Long storeId, 
        @Param("today") LocalDate today, 
        @Param("search") String search, 
        Pageable pageable
    );
    // [신규] 특정 날짜(today) 이전에 만료된 모든 문서를 찾는 쿼리
    List<EmployeeDocument> findAllByRetentionEndDateBefore(LocalDate today);
}