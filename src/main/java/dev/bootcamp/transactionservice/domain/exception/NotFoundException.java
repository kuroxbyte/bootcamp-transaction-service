package dev.bootcamp.transactionservice.domain.exception;

/**
 * El recurso no existe o no pertenece al cliente que lo solicita. Se distingue de {@link
 * BusinessRuleException}, que indica una regla de negocio incumplida.
 */
public class NotFoundException extends RuntimeException {
    /**
     * Crea la excepción con el mensaje indicado.
     */
    public NotFoundException(String message) {
        super(message);
    }
}
