package dev.bootcamp.transactionservice.application.usecase;

import dev.bootcamp.transactionservice.application.port.CreditCardClient;
import dev.bootcamp.transactionservice.application.port.PaymentRepository;
import dev.bootcamp.transactionservice.domain.exception.BusinessRuleException;
import dev.bootcamp.transactionservice.domain.model.creditcard.CreditCard;
import dev.bootcamp.transactionservice.domain.model.payment.Payment;
import dev.bootcamp.transactionservice.domain.model.payment.PaymentTarget;
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
    public Payment pay(Long customerId, Long creditCardId, BigDecimal amount, LocalDate date) {
        Payment payment = new Payment(PaymentTarget.CREDIT_CARD, creditCardId, amount, date);
        CreditCard creditCard = creditCardClient.findById(creditCardId)
                .orElseThrow(() -> new BusinessRuleException("La tarjeta de crédito no existe"));
        if (!creditCard.belongsTo(customerId)) {
            throw new BusinessRuleException("La tarjeta de crédito no pertenece al cliente");
        }
        return paymentRepository.save(payment);
    }
}
