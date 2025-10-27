package com.erp.erp_back.repository.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.erp.erp_back.entity.user.Employee;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    // 기본적인 CRUD 메소드가 이미 모두 구현되어 있음
}