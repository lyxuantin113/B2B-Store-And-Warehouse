package com.b2b.warehouse.common.exception;

import com.b2b.warehouse.common.dto.ApiResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("Should map BusinessException to exact HTTP status and ErrorCode")
    void shouldHandleBusinessException() {
        BusinessException ex = new BusinessException(ErrorCode.INSUFFICIENT_INVENTORY, "Branch HCMC-01 only has 3 boxes left");

        ResponseEntity<ApiResponse<Void>> response = handler.handleBusinessException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getCode()).isEqualTo("ERR_INSUFFICIENT_INVENTORY");
        assertThat(response.getBody().getMessage()).isEqualTo("Branch HCMC-01 only has 3 boxes left");
    }

    @Test
    @DisplayName("Should map OptimisticLockingFailureException to 409 CONFLICT")
    void shouldHandleOptimisticLockingFailure() {
        OptimisticLockingFailureException ex = new OptimisticLockingFailureException("Row was updated by another transaction");

        ResponseEntity<ApiResponse<Void>> response = handler.handleOptimisticLockingException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(ErrorCode.CONCURRENT_MODIFICATION.getCode());
    }

    @Test
    @DisplayName("Should map AccessDeniedException to 403 FORBIDDEN")
    void shouldHandleAccessDeniedException() {
        AccessDeniedException ex = new AccessDeniedException("Access is denied");

        ResponseEntity<ApiResponse<Void>> response = handler.handleAccessDeniedException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(ErrorCode.FORBIDDEN.getCode());
    }
}
