package dev.bootcamp.transactionservice.domain.model.credit;

/**
 * Vista local de un crédito personal o empresarial, obtenida de credit-service.
 */
public record Credit(Long id, Long customerId) {

    /**
     * Indica si el crédito pertenece al cliente.
     */
    public boolean belongsTo(Long customerId) {
        return this.customerId.equals(customerId);
    }
}
