package dev.bootcamp.transactionservice.application.port;

import dev.bootcamp.transactionservice.domain.model.payment.Payment;
import dev.bootcamp.transactionservice.domain.model.payment.PaymentTarget;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;

public interface PaymentRepository {

    /** Pagos hechos al producto de crédito indicado. */
    Flowable<Payment> findByTargetAndProductId(PaymentTarget target, String productId);

    Single<Payment> save(Payment payment);
}
