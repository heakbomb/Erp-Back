package com.erp.erp_back.config;

import com.erp.erp_back.service.store.StoreService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.method.HandlerMethod;

@Component
@RequiredArgsConstructor
public class ActiveStoreInterceptor implements HandlerInterceptor {

    private final StoreService storeService;

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler
    ) throws Exception {

        // 컨트롤러가 아닌 리소스 요청(css, js 등)은 패스
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        String uri = request.getRequestURI();

        // ✅ 예외 1: 사업장 관리/인증 자체는 항상 열어둔다.
        //    (경로는 프로젝트에 맞게 조정: "/store/**", "/auth/**" 등)
        if (uri.startsWith("/store")
                || uri.startsWith("/phone-verify")
                || uri.startsWith("/business-number")) {
            return true;
        }

        // ✅ 예외 2: storeId가 아예 없는 API 는 제약 안걸기 (ex. 로그인, 프로필 조회 등)
        String storeIdParam = request.getParameter("storeId");
        if (storeIdParam == null || storeIdParam.isBlank()) {
            return true;
        }

        Long storeId;
        try {
            storeId = Long.valueOf(storeIdParam);
        } catch (NumberFormatException e) {
            // storeId 형식 이상하면 그냥 진행 (컨트롤러에서 400 처리하게 둠)
            return true;
        }

        try {
            // ✅ 여기서 비활성 여부 체크 (StoreService 쪽에서 active/status 검사)
            storeService.requireActiveStore(storeId);
            return true; // 통과
        } catch (IllegalStateException ex) {
            // ✅ 여기서 바로 응답 보내고 컨트롤러는 아예 안 타게 막기
            //    프론트 apiClient 인터셉터에서 아래 패턴을 보고 처리하므로 맞춰준다.
            //    - status: 423 (LOCKED)
            //    - body : "INACTIVE_STORE"
            response.setStatus(HttpStatus.LOCKED.value()); // 423
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().write("INACTIVE_STORE");
            return false;
        }
    }
}