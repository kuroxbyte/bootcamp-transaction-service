package dev.bootcamp.transactionservice.application.usecase;

import dev.bootcamp.transactionservice.application.port.CreditCardClient;
import dev.bootcamp.transactionservice.application.port.PaymentRepository;
import dev.bootcamp.transactionservice.domain.exception.NotFoundException;
import dev.bootcamp.transactionservice.domain.model.creditcard.CreditCard;
import dev.bootcamp.transactionservice.domain.model.payment.Payment;
import dev.bootcamp.transactionservice.domain.model.payment.PaymentTarget;
import io.reactivex.rxjava3.core.Single;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Implementación de {@link PayCreditCardUseCase}. */
public class PayCreditCardUseCaseImpl implements PayCreditCardUseCase {
    private static final String CREDIT_CARD_NOT_FOUND = "La tarjeta de crédito no existe";
    private static final String CREDIT_CARD_NOT_OWNED =
            "La tarjeta de crédito no pertenece al cliente";

    private final PaymentRepository paymentRepository;
    private final CreditCardClient creditCardClient;

    /** Crea el caso de uso con los adaptadores de sus puertos. */
    public PayCreditCardUseCaseImpl(
            PaymentRepository paymentRepository, CreditCardClient creditCardClient) {
        this.paymentRepository = paymentRepository;
        this.creditCardClient = creditCardClient;
    }

    @Override
    public Single<Payment> pay(
            String customerId, String creditCardId, BigDecimal amount, LocalDate date) {
        return Single.fromCallable(
                        () -> new Payment(PaymentTarget.CREDIT_CARD, creditCardId, amount, date))
                .flatMap(
                        payment ->
                                creditCardClient
                                        .findById(creditCardId)
                                        .switchIfEmpty(
                                                Single.error(
                                                        () ->
                                                                new NotFoundException(
                                                                        CREDIT_CARD_NOT_FOUND)))
                                        .map(creditCard -> ensureBelongsTo(creditCard, customerId))
                                        .flatMap(creditCard -> paymentRepository.save(payment)));
    }

    private static CreditCard ensureBelongsTo(CreditCard creditCard, String customerId) {
        if (!creditCard.belongsTo(customerId)) {
            throw new NotFoundException(CREDIT_CARD_NOT_OWNED);
        }
        return creditCard;
    }
}
