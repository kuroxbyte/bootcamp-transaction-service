package dev.bootcamp.transactionservice.domain.exception;

/** Se incumple una regla de negocio; se traduce a una respuesta 422. */
public class BusinessRuleException extends RuntimeException {
    /** Crea la excepción con el mensaje indicado. */
    public BusinessRuleException(String message) {
        super(message);
    }
}
