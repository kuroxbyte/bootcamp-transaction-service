package dev.bootcamp.transactionservice.domain.model.yanki;

/**
 * Resultado de un cargo o abono que yanki-service pidió sobre una cuenta bancaria. Se guarda para
 * responder lo mismo si el pedido llega otra vez.
 *
 * @param operationId id de la operación en yanki-service
 * @param completed   true si se registró el movimiento
 * @param reason      motivo del rechazo (null si se completó)
 */
public record YankiOperation(String operationId, boolean completed, String reason) {

    /**
     * Operación completada.
     */
    public static YankiOperation completed(String operationId) {
        return new YankiOperation(operationId, true, null);
    }

    /**
     * Operación rechazada con su motivo.
     */
    public static YankiOperation rejected(String operationId, String reason) {
        return new YankiOperation(operationId, false, reason);
    }
}
