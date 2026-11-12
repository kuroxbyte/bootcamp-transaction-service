package dev.bootcamp.transactionservice.application.port;

import dev.bootcamp.transactionservice.domain.model.charge.Charge;
import dev.bootcamp.transactionservice.domain.model.payment.Payment;
import io.reactivex.rxjava3.core.Completable;

/**
 * Publicación de los pagos y consumos de productos de crédito para otros microservicios (por
 * ejemplo, debt-service calcula con ellos la deuda vencida).
 */
public interface TransactionEventPublisher {

    /**
     * Publica un pago registrado a un crédito o a una tarjeta de crédito, hecho por su dueño o por
     * un tercero ({@link Payment#payerCustomerId()}).
     *
     * @param customerId cliente dueño del producto pagado (no necesariamente quien pagó)
     * @return termina cuando se publicó, o sin error si no se pudo (el adaptador lo registra)
     */
    Completable publishPaymentRegistered(String customerId, Payment payment);

    /**
     * Publica un consumo registrado en una tarjeta de crédito.
     *
     * @param customerId cliente dueño de la tarjeta
     * @return termina cuando se publicó, o sin error si no se pudo (el adaptador lo registra)
     */
    Completable publishChargeRegistered(String customerId, Charge charge);
}
