package dev.bootcamp.transactionservice.application.port;

import dev.bootcamp.transactionservice.domain.model.payment.Payment;
import dev.bootcamp.transactionservice.domain.model.payment.PaymentTarget;
import java.util.List;

public interface PaymentRepository {
    /** Pagos hechos al producto de crédito indicado. */
    List<Payment> findByTargetAndProductId(PaymentTarget target, Long productId);

    Payment save(Payment payment);
}
