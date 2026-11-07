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

public class PayCreditCardUseCaseImpl implements PayCreditCardUseCase {
    private final PaymentRepository paymentRepository;
    private final CreditCardClient creditCardClient;

    public PayCreditCardUseCaseImpl(PaymentRepository paymentRepository, CreditCardClient creditCardClient) {
        this.paymentRepository = paymentRepository;
        this.creditCardClient = creditCardClient;
    }

    @Override
    public Single<Payment> pay(String customerId, String creditCardId, BigDecimal amount, LocalDate date) {
        return Single.fromCallable(() -> new Payment(PaymentTarget.CREDIT_CARD, creditCardId, amount, date))
                .flatMap(payment -> creditCardClient.findById(creditCardId)
                        .switchIfEmpty(Single.error(() -> new NotFoundException("La tarjeta de crédito no existe")))
                        .map(creditCard -> ensureBelongsTo(creditCard, customerId))
                        .flatMap(creditCard -> paymentRepository.save(payment)));
    }

    private static CreditCard ensureBelongsTo(CreditCard creditCard, String customerId) {
        if (!creditCard.belongsTo(customerId)) {
            throw new NotFoundException("La tarjeta de crédito no pertenece al cliente");
        }
        return creditCard;
    }
}
