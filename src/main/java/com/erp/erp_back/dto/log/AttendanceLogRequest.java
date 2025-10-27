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

    @NotNull
    private LocalDateTime recordTime; 

    @NotBlank
    @Size(max = 20)
    private String recordType; // (예: "CLOCK_IN", "CLOCK_OUT")

    @Size(max = 40)
    private String clientIp; 
}
