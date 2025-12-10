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

@Slf4j // 로깅을 위해 추가
@RestControllerAdvice // @ControllerAdvice + @ResponseBody
public class GlobalExceptionHandler {

    /**
     * 클라이언트에게 반환할 에러 응답 포맷
     */
    public record ErrorResponse(String code, String message, Map<String, String> details) {
        public ErrorResponse(String code, String message) {
            this(code, message, null);
        }
    }

    // =================================================================================
    // 1. 파일 관련 예외 (기존 핸들러 개선 - String 반환을 JSON으로 변경)
    // =================================================================================

    @ExceptionHandler(FileStorageException.class)
    public ResponseEntity<ErrorResponse> handleFileStorageException(FileStorageException ex) {
        log.error("File Storage Error: ", ex); // 서버 로그 남기기
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("FILE_UPLOAD_ERROR", "파일 저장 중 오류가 발생했습니다."));
    }

    @ExceptionHandler(FileNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleFileNotFoundException(FileNotFoundException ex) {
        log.error("File Not Found: ", ex);
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("FILE_NOT_FOUND", "요청하신 파일을 찾을 수 없습니다."));
    }

    // =================================================================================
    // 2. 입력값 검증 및 요청 오류 (ApiExceptionHandler 통합)
    // =================================================================================

    // @Valid 검증 실패 시 (DTO 유효성 검사 등)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        log.warn("Validation Failed: {}", ex.getMessage()); // 유효성 검사는 error 레벨보다는 warn이 적절할 수 있음

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        // 첫 번째 에러 메시지를 대표 메시지로 사용
        String primaryMessage = errors.values().stream().findFirst().orElse("잘못된 요청 데이터입니다.");

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("INVALID_INPUT", primaryMessage, errors));
    }

    //비즈니스 예외(BusinessException)를 공통 에러 응답으로 변환해 주는 글로벌 핸들러.
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException ex) {

        ErrorResponse body = new ErrorResponse(
                ex.getCode(),
                ex.getMessage(),
                null);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(body);
    }

    // 잘못된 인자 전달 (기존 두 핸들러 충돌 해결)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        log.error("Illegal Argument: ", ex);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("BAD_REQUEST", ex.getMessage()));
    }

    // 제약 조건 위반 (DB 제약조건 등)
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        log.error("Constraint Violation: ", ex);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("CONSTRAINT_VIOLATION", ex.getMessage()));
    }

    // 잘못된 상태 (ApiExceptionHandler에서 가져옴)
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException ex) {
        log.error("Illegal State: ", ex);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("ILLEGAL_STATE", ex.getMessage()));
    }

    // =================================================================================
    // 3. 데이터베이스 및 리소스 관련 예외
    // =================================================================================

    // 데이터 중복 (ErrorCodes 활용 가능 지점)
    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateKey(DuplicateKeyException ex) {
        log.error("Duplicate Key: ", ex);
        // 필요하다면 ex.getMessage()를 파싱해서 ErrorCodes.DUPLICATE_MENU_NAME 등을 구분할 수도 있음
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(ErrorCodes.DUPLICATE_DATA, "이미 존재하는 데이터입니다."));
    }

    // 데이터 무결성 위반
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex) {
        log.error("Data Integrity Violation: ", ex);
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("DATA_INTEGRITY_VIOLATION", "데이터 무결성 위반 오류입니다. 연관된 데이터를 확인해주세요."));
    }

    // 엔티티 없음 (JPA 표준 예외)
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEntityNotFound(EntityNotFoundException ex) {
        log.error("Entity Not Found: ", ex);
        // ErrorCodes에 정의된 일반적인 '찾을 수 없음' 코드를 사용하거나 동적으로 처리
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("RESOURCE_NOT_FOUND", ex.getMessage()));
    }

    // =================================================================================
    // 4. 그 외 처리되지 않은 모든 예외 (최후의 보루)
    // =================================================================================

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(Exception ex) {
        log.error("Unexpected Error: ", ex); // 스택 트레이스 전체 로깅
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("INTERNAL_SERVER_ERROR", "알 수 없는 서버 오류가 발생했습니다. 관리자에게 문의하세요."));
    }
}