package dev.bootcamp.transactionservice.domain.model.credit;

/**
 * Vista local del producto de crédito, obtenida de credit-service.
 */
public record Credit(Long id, Long customerId) {

    /**
     * Indica si el crédito pertenece al cliente.
     */
    public boolean belongsTo(Long customerId) {
        return this.customerId.equals(customerId);
    }
}
