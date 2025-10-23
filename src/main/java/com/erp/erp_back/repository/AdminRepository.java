package com.erp.erp_back.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.erp.erp_back.entity.Admin;

@Repository
public interface AdminRepository extends JpaRepository<Admin, Long> {
    // 기본적인 CRUD 메소드가 이미 모두 구현되어 있음
}