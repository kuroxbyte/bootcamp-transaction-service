package dev.bootcamp.transactionservice.infrastructure.persistence;

import dev.bootcamp.transactionservice.domain.model.charge.Charge;
import dev.bootcamp.transactionservice.domain.model.movement.Movement;
import dev.bootcamp.transactionservice.domain.model.movement.MovementType;
import dev.bootcamp.transactionservice.domain.model.payment.Payment;
import dev.bootcamp.transactionservice.domain.model.payment.PaymentTarget;

/**
 * Conversión entre el dominio y los documentos de MongoDB.
 */
final class TransactionPersistenceMapper {
    private TransactionPersistenceMapper() {}

    static MovementDocument toDocument(Movement movement) {
        return new MovementDocument(
                null,
                movement.accountId(),
                movement.type().name(),
                movement.amount(),
                movement.date(),
                movement.commission());
    }

    static PaymentDocument toDocument(Payment payment) {
        return new PaymentDocument(
                null,
                payment.target().name(),
                payment.productId(),
                payment.amount(),
                payment.date(),
                payment.payerCustomerId());
    }

    static ChargeDocument toDocument(Charge charge) {
        return new ChargeDocument(null, charge.creditCardId(), charge.amount(), charge.date());
    }

    static Movement toDomain(MovementDocument document) {
        return new Movement(
                document.getAccountId(),
                MovementType.valueOf(document.getType()),
                document.getAmount(),
                document.getDate(),
                document.getCommission());
    }

    static Payment toDomain(PaymentDocument document) {
        return new Payment(
                PaymentTarget.valueOf(document.getTarget()),
                document.getProductId(),
                document.getAmount(),
                document.getDate(),
                document.getPayerCustomerId());
    }

    static Charge toDomain(ChargeDocument document) {
        return new Charge(document.getCreditCardId(), document.getAmount(), document.getDate());
    }
}
