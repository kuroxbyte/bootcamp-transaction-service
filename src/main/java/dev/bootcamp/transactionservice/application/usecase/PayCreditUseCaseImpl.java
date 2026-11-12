package dev.bootcamp.transactionservice.application.usecase;

import dev.bootcamp.transactionservice.application.port.CreditClient;
import dev.bootcamp.transactionservice.application.port.PaymentRepository;
import dev.bootcamp.transactionservice.application.port.TransactionEventPublisher;
import dev.bootcamp.transactionservice.domain.exception.BusinessRuleException;
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
        return register(customerId, creditId, amount, date, true);
    }

    @Override
    public Single<Payment> payThirdParty(
            String customerId,
            String creditId,
            BigDecimal amount,
            LocalDate date) {
        return register(customerId, creditId, amount, date, false);
    }

    private Single<Payment> register(
            String customerId,
            String creditId,
            BigDecimal amount,
            LocalDate date,
            boolean ownCredit) {
        return Single.fromCallable(() ->
                        new Payment(PaymentTarget.CREDIT, creditId, amount, date, customerId))
                .flatMap(payment -> creditClient
                        .findById(creditId)
                        .switchIfEmpty(Single.error(
                                () -> new NotFoundException("El crédito no existe")))
                        .map(credit -> ensureOwnership(credit, customerId, ownCredit))
                        .flatMap(credit -> paymentRepository.save(payment)
                                .flatMap(saved -> transactionEventPublisher
                                        .publishPaymentRegistered(credit.customerId(), saved)
                                        .andThen(Single.just(saved)))));
    }

    private static Credit ensureOwnership(Credit credit, String customerId, boolean ownCredit) {
        if (ownCredit && !credit.belongsTo(customerId)) {
            throw new NotFoundException("El crédito no pertenece al cliente");
        }
        if (!ownCredit && credit.belongsTo(customerId)) {
            throw new BusinessRuleException(
                    "El crédito es del cliente; use el pago de sus propios créditos");
        }
        return credit;
    }
}
