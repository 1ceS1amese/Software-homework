package com.csms.common.error;

import com.csms.common.api.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.UUID;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public ResponseEntity<ApiResponse<?>> handleBizException(BizException e) {
        log.warn("Business exception: {}", e.getMessage());
        return ResponseEntity.status(statusOf(e.getErrorCode()))
                .body(ApiResponse.error(e.getErrorCode().getCode(), e.getMessage()));
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<?>> handleValidationException(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        log.warn("Validation error: {}", msg);
        return ResponseEntity.badRequest().body(ApiResponse.error(ErrorCode.PARAM_ERROR.getCode(), msg));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleException(Exception e) {
        String traceId = UUID.randomUUID().toString().replace("-", "");
        log.error("System error, traceId: {}", traceId, e);
        ApiResponse<?> response = ApiResponse.error(ErrorCode.SYSTEM_ERROR.getCode(), ErrorCode.SYSTEM_ERROR.getMessage());
        response.setTraceId(traceId);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .header("X-Trace-Id", traceId)
                .body(response);
    }

    private HttpStatus statusOf(ErrorCode code) {
        return switch (code) {
            case AUTH_BAD_CREDENTIALS, AUTH_LOCKED, AUTH_EXPIRED, AUTH_INVALID_TOKEN -> HttpStatus.UNAUTHORIZED;
            case FORBIDDEN, AUTH_DISABLED, GRADE_UNLOCK_FORBIDDEN -> HttpStatus.FORBIDDEN;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case CONFLICT, DEPT_CODE_EXISTS, DEPT_HAS_RECORDS, MAJOR_CODE_EXISTS,
                    TERM_CODE_EXISTS, COURSE_CODE_EXISTS, SYS_CONFIG_READONLY,
                    TC_EXISTS, TC_STATUS_INVALID, TC_HAS_ENROLLMENTS,
                    ENROLL_DUPLICATE, ENROLL_FULL, ENROLL_TIME_CONFLICT,
                    GRADE_PUBLISHED, USER_EXISTS, USER_EXISTS_DELETED -> HttpStatus.CONFLICT;
            case SYSTEM_BUSY, ENROLL_SYSTEM_BUSY -> HttpStatus.SERVICE_UNAVAILABLE;
            case SYSTEM_ERROR -> HttpStatus.INTERNAL_SERVER_ERROR;
            default -> HttpStatus.BAD_REQUEST;
        };
    }
}
