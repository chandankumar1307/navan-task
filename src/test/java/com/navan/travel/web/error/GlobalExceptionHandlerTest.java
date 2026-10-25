package com.navan.travel.web.error;

import com.navan.travel.domain.TransactionType;
import com.navan.travel.domain.entity.TransactionEntity;
import com.navan.travel.domain.repository.TransactionRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private HttpServletRequest httpServletRequest;

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler(transactionRepository);
    }

    @Test
    @DisplayName("handleBadRequest returns 400 with detail message")
    void testHandleBadRequest() {
        ResponseEntity<Map<String, Object>> res = exceptionHandler.handleBadRequest(new BadRequestException("Invalid input"));

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(res.getBody().get("status")).isEqualTo(400);
        assertThat(res.getBody().get("title")).isEqualTo("Bad Request");
        assertThat(res.getBody().get("detail")).isEqualTo("Invalid input");
    }

    @Test
    @DisplayName("handleNotFound returns 404 with detail message")
    void testHandleNotFound() {
        ResponseEntity<Map<String, Object>> res = exceptionHandler.handleNotFound(new ResourceNotFoundException("Resource missing"));

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(res.getBody().get("status")).isEqualTo(404);
        assertThat(res.getBody().get("title")).isEqualTo("Not Found");
        assertThat(res.getBody().get("detail")).isEqualTo("Resource missing");
    }

    @Test
    @DisplayName("handleConflict returns 409 and includes id at root when present")
    void testHandleConflictWithId() {
        ResponseEntity<Map<String, Object>> res = exceptionHandler.handleConflict(new ConflictException("Conflict detected", "txn_existing_1"));

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(res.getBody().get("status")).isEqualTo(409);
        assertThat(res.getBody().get("title")).isEqualTo("Conflict");
        assertThat(res.getBody().get("detail")).isEqualTo("Conflict detected");
        assertThat(res.getBody().get("id")).isEqualTo("txn_existing_1");
    }

    @Test
    @DisplayName("handleConflict returns 409 without id when id is null")
    void testHandleConflictWithoutId() {
        ResponseEntity<Map<String, Object>> res = exceptionHandler.handleConflict(new ConflictException("Conflict without id"));

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(res.getBody().get("status")).isEqualTo(409);
        assertThat(res.getBody().containsKey("id")).isFalse();
    }

    @Test
    @DisplayName("handleDataIntegrityViolation extracts external_id from request attribute and populates existing id")
    void testHandleDataIntegrityViolationWithAttribute() {
        when(httpServletRequest.getAttribute("external_id")).thenReturn("ext-conflict-123");
        TransactionEntity existing = new TransactionEntity("txn_winner_456", "ext-conflict-123", TransactionType.FLIGHT, "EUR", Instant.now(), new BigDecimal("100.00"), "{}");
        when(transactionRepository.findByExternalId("ext-conflict-123")).thenReturn(Optional.of(existing));

        DataIntegrityViolationException ex = new DataIntegrityViolationException("Unique constraint violation");
        ResponseEntity<Map<String, Object>> res = exceptionHandler.handleDataIntegrityViolation(ex, httpServletRequest);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(res.getBody().get("status")).isEqualTo(409);
        assertThat(res.getBody().get("id")).isEqualTo("txn_winner_456");
    }

    @Test
    @DisplayName("handleIllegalArgument returns 400 Bad Request")
    void testHandleIllegalArgument() {
        ResponseEntity<Map<String, Object>> res = exceptionHandler.handleIllegalArgument(new IllegalArgumentException("Illegal argument"));

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(res.getBody().get("status")).isEqualTo(400);
        assertThat(res.getBody().get("detail")).isEqualTo("Illegal argument");
    }

    @Test
    @DisplayName("handleGeneral returns 500 Internal Server Error")
    void testHandleGeneral() {
        ResponseEntity<Map<String, Object>> res = exceptionHandler.handleGeneral(new RuntimeException("Unexpected error"));

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(res.getBody().get("status")).isEqualTo(500);
        assertThat(res.getBody().get("title")).isEqualTo("Internal Server Error");
    }
}
