package com.erp.erp_back.exception;

import java.util.HashMap;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.erp.erp_back.common.ErrorCodes;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    public record ErrorResponse(String code, String message, Map<String, String> details) {
        public ErrorResponse(String code, String message) {
            this(code, message, null);
        }
    }

    // =========================
    // 1) Validation
    // =========================
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        log.warn("Validation Failed: {}", ex.getMessage());

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            if (error instanceof FieldError fe) {
                errors.put(fe.getField(), fe.getDefaultMessage());
            } else {
                errors.put(error.getObjectName(), error.getDefaultMessage());
            }
        });

        String primaryMessage = errors.values().stream().findFirst().orElse("잘못된 요청 데이터입니다.");
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .<ErrorResponse>body(new ErrorResponse("INVALID_INPUT", primaryMessage, errors));
    }

    // =========================
    // 2) BusinessException  ✅ 중복은 409로
    // =========================
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException ex) {

        // 여기서 ex.getCode()는 "문자열 코드"라고 가정 (네 기존 코드가 그 구조)
        String code = ex.getCode();

        HttpStatus status = HttpStatus.BAD_REQUEST;

        // ✅ ErrorCodes가 enum이 아니라 String 상수면, 그냥 equals로 비교해야 함
        if (ErrorCodes.DUPLICATE_DATA.equals(code)
                || ErrorCodes.DUPLICATE_MENU_NAME.equals(code)
                || ErrorCodes.DUPLICATE_INVENTORY_NAME.equals(code)) {
            status = HttpStatus.CONFLICT; // 409
        }

        return ResponseEntity
                .status(status)
                .<ErrorResponse>body(new ErrorResponse(code, ex.getMessage(), null));
    }

    // =========================
    // 3) DB duplicate (레이스컨디션/유니크 제약)
    // =========================
    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateKey(DuplicateKeyException ex) {
        log.warn("Duplicate Key: ", ex);

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .<ErrorResponse>body(new ErrorResponse(ErrorCodes.DUPLICATE_DATA, "이미 존재하는 데이터입니다."));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex) {
        log.warn("Data Integrity Violation: ", ex);

        // ✅ 여기서도 프론트가 처리 가능한 코드로 통일 (409)
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .<ErrorResponse>body(new ErrorResponse(ErrorCodes.DUPLICATE_DATA, "이미 존재하는 데이터입니다."));
    }

    // =========================
    // 4) Common
    // =========================
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        log.error("Illegal Argument: ", ex);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .<ErrorResponse>body(new ErrorResponse("BAD_REQUEST", ex.getMessage()));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        log.error("Constraint Violation: ", ex);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .<ErrorResponse>body(new ErrorResponse("CONSTRAINT_VIOLATION", ex.getMessage()));
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEntityNotFound(EntityNotFoundException ex) {
        log.error("Entity Not Found: ", ex);
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .<ErrorResponse>body(new ErrorResponse("RESOURCE_NOT_FOUND", ex.getMessage()));
    }

    // =========================
    // 5) Fallback
    // =========================
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(Exception ex) {
        log.error("Unexpected Error: ", ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .<ErrorResponse>body(new ErrorResponse("INTERNAL_SERVER_ERROR", "알 수 없는 서버 오류가 발생했습니다. 관리자에게 문의하세요."));
    }
}
