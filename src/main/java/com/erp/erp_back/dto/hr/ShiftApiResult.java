package com.erp.erp_back.dto.hr;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShiftApiResult<T> {
    private boolean success;
    private String code;
    private String message;
    private T data;

    public static <T> ShiftApiResult<T> ok(T data) {
        return ShiftApiResult.<T>builder()
                .success(true)
                .code("OK")
                .message("OK")
                .data(data)
                .build();
    }

    public static <T> ShiftApiResult<T> duplicate(String message) {
        return ShiftApiResult.<T>builder()
                .success(false)
                .code("DUPLICATE_SHIFT")
                .message(message)
                .data(null)
                .build();
    }
}