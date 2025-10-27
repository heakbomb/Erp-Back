package com.erp.erp_back;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import jakarta.annotation.PostConstruct;

@SpringBootApplication
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
