package com.erp.erp_back.service.auth;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.erp.erp_back.entity.user.Employee;
import com.erp.erp_back.infra.auth.oauth.userinfo.OAuth2UserInfo;
import com.erp.erp_back.repository.user.EmployeeRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmployeeSocialAuthService {

    private final EmployeeRepository employeeRepository;

    @Transactional
    public Employee upsertEmployee(OAuth2UserInfo userInfo) {
        String provider = safeLower(userInfo.getProvider());
        String providerId = trimToNull(userInfo.getProviderId());

        if (provider == null || provider.isBlank() || providerId == null) {
            throw new IllegalArgumentException("Invalid OAuth2 user info (provider/providerId missing)");
        }

        try {
            Employee employee = employeeRepository.findByProviderAndProviderId(provider, providerId)
                    .orElseGet(Employee::new);

            employee.setProvider(provider);
            employee.setProviderId(providerId);

            // name: 빈값은 덮어쓰지 않되, 신규 & null이면 fallback 제공
            String name = trimToNull(userInfo.getName());
            if (name != null) {
                employee.setName(name);
            } else if (employee.getName() == null) {
                employee.setName(provider + "_" + providerId); // ✅ 식별 가능한 기본값
            }

            // email/phone: 있으면 갱신, 없으면 유지 (nullable 전제)
            String email = trimToNull(userInfo.getEmail());
            if (email != null) employee.setEmail(email);

            String phone = trimToNull(userInfo.getPhone());
            if (phone != null) employee.setPhone(phone);

            return employeeRepository.save(employee);

        } catch (DataIntegrityViolationException e) {
            // ✅ 동시성으로 unique 충돌 시 재조회해서 반환
            return employeeRepository.findByProviderAndProviderId(provider, providerId)
                    .orElseThrow(() -> e);
        }
    }

    private String safeLower(String s) {
        if (s == null) return null;
        return s.trim().toLowerCase();
    }

    private String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}