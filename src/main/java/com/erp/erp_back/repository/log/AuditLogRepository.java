package com.erp.erp_back.repository.log;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.erp.erp_back.entity.log.AuditLog;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    /** (Admin) 최근 감사 로그 조회 (페이징)
     * (findAll(Pageable pageable)을 써도 되지만 명시적으로 선언)
     */
    Page<AuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
