package com.csms.common.error;

import com.csms.common.api.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void businessErrorsUseDocumentedHttpStatuses() {
        assertStatus(ErrorCode.AUTH_BAD_CREDENTIALS, HttpStatus.UNAUTHORIZED);
        assertStatus(ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        assertStatus(ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND);
        assertStatus(ErrorCode.ENROLL_TIME_CONFLICT, HttpStatus.CONFLICT);
        assertStatus(ErrorCode.ENROLL_OUT_OF_TIME, HttpStatus.BAD_REQUEST);
        assertStatus(ErrorCode.ENROLL_SYSTEM_BUSY, HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    void unexpectedErrorIncludesTraceIdInHeaderAndBody() {
        ResponseEntity<ApiResponse<?>> response = handler.handleException(new IllegalStateException("test"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(ErrorCode.SYSTEM_ERROR.getCode(), response.getBody().getCode());
        assertNotNull(response.getBody().getTraceId());
        assertEquals(response.getBody().getTraceId(), response.getHeaders().getFirst("X-Trace-Id"));
    }

    private void assertStatus(ErrorCode code, HttpStatus status) {
        ResponseEntity<ApiResponse<?>> response = handler.handleBizException(new BizException(code));
        assertEquals(status, response.getStatusCode(), code.name());
        assertNotNull(response.getBody());
        assertEquals(code.getCode(), response.getBody().getCode());
        assertEquals(code.getMessage(), response.getBody().getMessage());
    }
}
