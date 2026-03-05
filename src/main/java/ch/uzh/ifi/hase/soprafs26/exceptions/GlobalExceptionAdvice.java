package ch.uzh.ifi.hase.soprafs26.exceptions;

import ch.uzh.ifi.hase.soprafs26.rest.dto.ErrorResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@ControllerAdvice(annotations = RestController.class)
public class GlobalExceptionAdvice {

  private final Logger log = LoggerFactory.getLogger(GlobalExceptionAdvice.class);

  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<ErrorResponseDTO> handleResponseStatusException(
      ResponseStatusException ex, HttpServletRequest request) {

    String reason = ex.getReason() != null ? ex.getReason() : "Unexpected error";

    if (ex.getStatusCode().is5xxServerError()) {
      log.error("Request: {} failed", request.getRequestURL(), ex);
    } else {
      log.warn("Request: {} rejected: {}", request.getRequestURL(), reason);
    }

    return ResponseEntity.status(ex.getStatusCode()).body(new ErrorResponseDTO(reason));
  }

  @ExceptionHandler(TransactionSystemException.class)
  public ResponseEntity<ErrorResponseDTO> handleTransactionSystemException(
      TransactionSystemException ex, HttpServletRequest request) {

    log.error("Request: {} transaction failed", request.getRequestURL(), ex);
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(new ErrorResponseDTO("Database constraint violation"));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponseDTO> handleAny(
      Exception ex, HttpServletRequest request) {

    log.error("Request: {} crashed", request.getRequestURL(), ex);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(new ErrorResponseDTO("Internal server error"));
  }
}