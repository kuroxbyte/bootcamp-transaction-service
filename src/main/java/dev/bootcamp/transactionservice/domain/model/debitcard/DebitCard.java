package dev.bootcamp.transactionservice.domain.model.debitcard;

/**
 * Copia local de una tarjeta de débito, construida con los eventos de account-service.
 *
 * @param id               id de la tarjeta en account-service
 * @param customerId       cliente dueño
 * @param primaryAccountId cuenta de la que salen los pagos con la tarjeta
 */
public record DebitCard(String id, String customerId, String primaryAccountId) {

    /**
     * Indica si la tarjeta pertenece al cliente.
     */
    public boolean belongsTo(String customerId) {
        return this.customerId.equals(customerId);
    }
}
