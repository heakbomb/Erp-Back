// src/main/java/com/erp/erp_back/repository/hr/PayrollSettingRepository.java
package com.erp.erp_back.repository.hr;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.erp.erp_back.entity.hr.PayrollSetting;

public interface PayrollSettingRepository extends JpaRepository<PayrollSetting, Long> {

    // 사업장 전체 직원 급여 설정
    List<PayrollSetting> findAllByStore_StoreId(Long storeId);

    // 특정 직원 한 명 설정
    Optional<PayrollSetting> findByStore_StoreIdAndEmployee_EmployeeId(Long storeId, Long employeeId);
}