package dev.bootcamp.transactionservice.application.usecase;

import dev.bootcamp.transactionservice.application.port.CreditCardClient;
import dev.bootcamp.transactionservice.application.port.PaymentRepository;
import dev.bootcamp.transactionservice.application.port.TransactionEventPublisher;
import dev.bootcamp.transactionservice.domain.exception.BusinessRuleException;
import dev.bootcamp.transactionservice.domain.exception.NotFoundException;
import dev.bootcamp.transactionservice.domain.model.creditcard.CreditCard;
import dev.bootcamp.transactionservice.domain.model.payment.Payment;
import dev.bootcamp.transactionservice.domain.model.payment.PaymentTarget;
import io.reactivex.rxjava3.core.Single;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Implementación de {@link PayCreditCardUseCase}.
 */
public class PayCreditCardUseCaseImpl implements PayCreditCardUseCase {
    private static final String CREDIT_CARD_NOT_FOUND = "La tarjeta de crédito no existe";
    private static final String CREDIT_CARD_NOT_OWNED =
            "La tarjeta de crédito no pertenece al cliente";
    private static final String CREDIT_CARD_IS_OWN =
            "La tarjeta es del cliente; use el pago de sus propias tarjetas";

    private final PaymentRepository paymentRepository;
    private final CreditCardClient creditCardClient;
    private final TransactionEventPublisher transactionEventPublisher;

    /**
     * Crea el caso de uso con los adaptadores de sus puertos.
     */
    public PayCreditCardUseCaseImpl(
            PaymentRepository paymentRepository,
            CreditCardClient creditCardClient,
            TransactionEventPublisher transactionEventPublisher) {
        this.paymentRepository = paymentRepository;
        this.creditCardClient = creditCardClient;
        this.transactionEventPublisher = transactionEventPublisher;
    }

    @Override
    public Single<Payment> pay(
            String customerId,
            String creditCardId,
            BigDecimal amount,
            LocalDate date) {
        return register(customerId, creditCardId, amount, date, true);
    }

    @Override
    public Single<Payment> payThirdParty(
            String customerId,
            String creditCardId,
            BigDecimal amount,
            LocalDate date) {
        return register(customerId, creditCardId, amount, date, false);
    }

    private Single<Payment> register(
            String customerId,
            String creditCardId,
            BigDecimal amount,
            LocalDate date,
            boolean ownCard) {
        return Single.fromCallable(() -> new Payment(
                        PaymentTarget.CREDIT_CARD,
                        creditCardId,
                        amount,
                        date,
                        customerId))
                .flatMap(payment -> creditCardClient
                        .findById(creditCardId)
                        .switchIfEmpty(Single.error(
                                () -> new NotFoundException(CREDIT_CARD_NOT_FOUND)))
                        .map(creditCard -> ensureOwnership(creditCard, customerId, ownCard))
                        .flatMap(creditCard -> paymentRepository.save(payment)
                                .flatMap(saved -> transactionEventPublisher
                                        .publishPaymentRegistered(creditCard.customerId(), saved)
                                        .andThen(Single.just(saved)))));
    }

    private static CreditCard ensureOwnership(
            CreditCard creditCard,
            String customerId,
            boolean ownCard) {
        if (ownCard && !creditCard.belongsTo(customerId)) {
            throw new NotFoundException(CREDIT_CARD_NOT_OWNED);
        }
        if (!ownCard && creditCard.belongsTo(customerId)) {
            throw new BusinessRuleException(CREDIT_CARD_IS_OWN);
        }
        return creditCard;
    }
}
