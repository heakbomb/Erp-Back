package com.erp.erp_back.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD) // 메소드 위에만 붙일 수 있음
@Retention(RetentionPolicy.RUNTIME) // 실행 중에도 정보를 읽을 수 있음
public @interface LogAudit {
    String action();      // 예: "STORE_UPDATE"
    String target();      // 예: "Store"
    int idIndex() default 0;
}