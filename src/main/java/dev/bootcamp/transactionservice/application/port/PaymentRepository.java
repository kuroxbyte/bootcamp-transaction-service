package dev.bootcamp.transactionservice.application.port;

import dev.bootcamp.transactionservice.domain.model.payment.Payment;

public interface PaymentRepository {
    Payment save(Payment payment);
}
