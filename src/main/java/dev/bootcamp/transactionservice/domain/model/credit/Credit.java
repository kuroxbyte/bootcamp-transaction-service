package dev.bootcamp.transactionservice.domain.model.credit;

/** Vista local de un crédito personal o empresarial, obtenida de credit-service. */
public record Credit(String id, String customerId) {

    /** Indica si el crédito pertenece al cliente. */
    public boolean belongsTo(String customerId) {
        return this.customerId.equals(customerId);
    }
}
