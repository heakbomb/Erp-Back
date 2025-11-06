package com.erp.erp_back.dto.log;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@NoArgsConstructor
public class AttendancePunchQrRequest {

    @NotNull
    private Long employeeId;   // 로그인 붙이기 전까지

    @NotBlank
    private String qrToken;    // QR에서 읽은 문자열

    private Double latitude;   // 모바일에서 보낸 위도
    private Double longitude;  // 모바일에서 보낸 경도

    @NotBlank
    private String recordType; // "IN" or "OUT"
}
