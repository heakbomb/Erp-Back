package com.erp.erp_back.repository.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.erp.erp_back.entity.user.Employee;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    @Query("SELECT e FROM Employee e " +
           "WHERE (:q = '' OR e.name LIKE %:q% OR e.email LIKE %:q% OR e.phone LIKE %:q%)")
    Page<Employee> findAdminEmployees(
            @Param("q") String q,
            Pageable pageable
    );
}