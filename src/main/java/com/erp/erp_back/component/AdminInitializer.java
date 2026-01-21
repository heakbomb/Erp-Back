package com.erp.erp_back.component;

import com.erp.erp_back.entity.user.Admin;
import com.erp.erp_back.repository.user.AdminRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AdminInitializer implements CommandLineRunner {

    private final AdminRepository adminRepository;
    // private final PasswordEncoder passwordEncoder; // 제거

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        String adminUsername = "admin";

        // 이미 존재하면 생성하지 않음
        if (adminRepository.findByUsername(adminUsername).isPresent()) {
            return;
        }

        System.out.println(">> Admin 계정 생성 중 (암호화 미적용)...");

        Admin admin = new Admin();
        admin.setUsername(adminUsername);
        admin.setSalt(UUID.randomUUID().toString().substring(0, 16));
        
        // ✅ 암호화 없이 "1234" 그대로 저장
        admin.setPassword("1234"); 

        adminRepository.save(admin);
        
        System.out.println(">> Admin 계정 생성 완료: ID=admin / PW=1234");
    }
}