package dev.bootcamp.transactionservice.application.usecase;

import dev.bootcamp.transactionservice.application.port.CreditClient;
import dev.bootcamp.transactionservice.application.port.PaymentRepository;
import dev.bootcamp.transactionservice.domain.exception.BusinessRuleException;
import dev.bootcamp.transactionservice.domain.model.credit.Credit;
import dev.bootcamp.transactionservice.domain.model.payment.Payment;
import dev.bootcamp.transactionservice.domain.model.payment.PaymentTarget;
import java.math.BigDecimal;
import java.time.LocalDate;

public class PayCreditUseCaseImpl implements PayCreditUseCase {
    private final PaymentRepository paymentRepository;
    private final CreditClient creditClient;

    public PayCreditUseCaseImpl(PaymentRepository paymentRepository, CreditClient creditClient) {
        this.paymentRepository = paymentRepository;
        this.creditClient = creditClient;
    }

    @Override
    public Payment pay(String customerId, String creditId, BigDecimal amount, LocalDate date) {
        Payment payment = new Payment(PaymentTarget.CREDIT, creditId, amount, date);
        Credit credit = creditClient.findById(creditId)
                .orElseThrow(() -> new BusinessRuleException("El crédito no existe"));
        if (!credit.belongsTo(customerId)) {
            throw new BusinessRuleException("El crédito no pertenece al cliente");
        }
        return paymentRepository.save(payment);
    }
}
