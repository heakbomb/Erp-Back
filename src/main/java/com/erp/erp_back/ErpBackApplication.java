package com.erp.erp_back;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

import jakarta.annotation.PostConstruct;

@SpringBootApplication
@EnableScheduling
@EnableJpaAuditing // ✅ 이 어노테이션이 있어야 @CreatedDate가 작동하여 생성 시간이 자동 기록됩니다.
public class ErpBackApplication {

	// ✅ 애플리케이션의 기본 시간대를 한국(Asia/Seoul)으로 설정하는 코드
    @PostConstruct
    public void started() {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
    }

    public static void main(String[] args) {
        SpringApplication.run(ErpBackApplication.class, args);
    }
}
