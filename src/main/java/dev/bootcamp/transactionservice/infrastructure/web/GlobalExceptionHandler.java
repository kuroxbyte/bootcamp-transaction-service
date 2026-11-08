package dev.bootcamp.transactionservice.infrastructure.web;

import dev.bootcamp.transactionservice.domain.exception.BusinessRuleException;
import dev.bootcamp.transactionservice.domain.exception.NotFoundException;
import dev.bootcamp.transactionservice.infrastructure.client.ServiceUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.reactive.result.method.annotation.ResponseEntityExceptionHandler;
import reactor.core.publisher.Mono;

/**
 * Traduce las excepciones a respuestas Problem Details (RFC 7807).
 *
 * <ul>
 *   <li>400: datos de entrada inválidos o falta un header; lo resuelve {@link
 *       ResponseEntityExceptionHandler}.
 *   <li>404: {@link NotFoundException}.
 *   <li>422: {@link BusinessRuleException}.
 *   <li>503: {@link ServiceUnavailableException}; el detalle ya lo registra el cliente HTTP.
 *   <li>500: cualquier otro error inesperado.
 * </ul>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    /** Responde 404 cuando el recurso no existe o no pertenece al cliente. */
    @ExceptionHandler(NotFoundException.class)
    public Mono<ResponseEntity<ProblemDetail>> handleNotFound(NotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        return problem(HttpStatus.NOT_FOUND, "Recurso no encontrado", ex.getMessage());
    }

    /** Responde 422 cuando se incumple una regla de negocio. */
    @ExceptionHandler(BusinessRuleException.class)
    public Mono<ResponseEntity<ProblemDetail>> handleBusinessRule(BusinessRuleException ex) {
        log.warn("Business rule violated: {}", ex.getMessage());
        return problem(
                HttpStatus.UNPROCESSABLE_CONTENT, "Regla de negocio incumplida", ex.getMessage());
    }

    /** Responde 503 cuando otro microservicio no está disponible. */
    @ExceptionHandler(ServiceUnavailableException.class)
    public Mono<ResponseEntity<ProblemDetail>> handleServiceUnavailable(
            ServiceUnavailableException ex) {
        return problem(HttpStatus.SERVICE_UNAVAILABLE, "Servicio no disponible", ex.getMessage());
    }

    /** Responde 500 ante cualquier otro error, sin exponer el detalle interno. */
    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<ProblemDetail>> handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        return problem(
                HttpStatus.INTERNAL_SERVER_ERROR, "Error interno", "Ocurrió un error inesperado");
    }

    private static Mono<ResponseEntity<ProblemDetail>> problem(
            HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return Mono.just(ResponseEntity.status(status).body(problem));
    }
}
