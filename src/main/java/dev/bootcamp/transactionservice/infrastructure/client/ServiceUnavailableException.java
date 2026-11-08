package dev.bootcamp.transactionservice.infrastructure.client;

/**
 * Otro microservicio no respondió a tiempo, no está disponible o respondió con un error inesperado.
 * Se traduce a una respuesta 503.
 */
public class ServiceUnavailableException extends RuntimeException {
    public ServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
