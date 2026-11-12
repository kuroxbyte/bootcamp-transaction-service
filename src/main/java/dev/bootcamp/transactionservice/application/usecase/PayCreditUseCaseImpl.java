package dev.bootcamp.transactionservice.application.usecase;

import dev.bootcamp.transactionservice.application.port.CreditClient;
import dev.bootcamp.transactionservice.application.port.PaymentRepository;
import dev.bootcamp.transactionservice.application.port.TransactionEventPublisher;
import dev.bootcamp.transactionservice.domain.exception.NotFoundException;
import dev.bootcamp.transactionservice.domain.model.credit.Credit;
import dev.bootcamp.transactionservice.domain.model.payment.Payment;
import dev.bootcamp.transactionservice.domain.model.payment.PaymentTarget;
import io.reactivex.rxjava3.core.Single;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Implementación de {@link PayCreditUseCase}.
 */
public class PayCreditUseCaseImpl implements PayCreditUseCase {
    private final PaymentRepository paymentRepository;
    private final CreditClient creditClient;
    private final TransactionEventPublisher transactionEventPublisher;

    /**
     * Crea el caso de uso con los adaptadores de sus puertos.
     */
    public PayCreditUseCaseImpl(
            PaymentRepository paymentRepository,
            CreditClient creditClient,
            TransactionEventPublisher transactionEventPublisher) {
        this.paymentRepository = paymentRepository;
        this.creditClient = creditClient;
        this.transactionEventPublisher = transactionEventPublisher;
    }

    @Override
    public Single<Payment> pay(
            String customerId,
            String creditId,
            BigDecimal amount,
            LocalDate date) {
        return Single.fromCallable(() -> new Payment(PaymentTarget.CREDIT, creditId, amount, date))
                .flatMap(
                        payment ->
                                creditClient
                                        .findById(creditId)
                                        .switchIfEmpty(
                                                Single.error(
                                                        () ->
                                                                new NotFoundException(
                                                                        "El crédito no existe")))
                                        .map(credit -> ensureBelongsTo(credit, customerId))
                                        .flatMap(credit -> paymentRepository.save(payment)))
                .flatMap(saved -> transactionEventPublisher
                        .publishPaymentRegistered(customerId, saved)
                        .andThen(Single.just(saved)));
    }

    private static Credit ensureBelongsTo(Credit credit, String customerId) {
        if (!credit.belongsTo(customerId)) {
            throw new NotFoundException("El crédito no pertenece al cliente");
        }
        return credit;
    }
}
