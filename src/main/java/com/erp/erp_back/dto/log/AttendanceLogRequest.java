package com.erp.erp_back.dto.log;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AttendanceLogRequest {

    @NotNull
    private Long employeeId;

    @NotNull
    private Long storeId;

    // 이건 서비스에서 지금 무시하고 서버 시간이 들어가니까 굳이 안 보내도 되는데
    // 기존 필드라 남겨둠
    @NotNull
    private LocalDateTime recordTime;

    @NotBlank
    @Size(max = 20)
    private String recordType;  // IN / OUT / CLOCK_IN ...

    // ✅ 추가: 직원이 찍은 QR 문자열
    @Size(max = 200)
    private String qrCode;

    // ✅ (선택) 위치도 같이 보낼 거면 미리 받아두기
    private Double latitude;
    private Double longitude;
}