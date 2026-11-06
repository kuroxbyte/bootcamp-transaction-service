package dev.bootcamp.transactionservice.application.usecase;

import dev.bootcamp.transactionservice.application.port.ChargeRepository;
import dev.bootcamp.transactionservice.application.port.CreditCardClient;
import dev.bootcamp.transactionservice.domain.exception.BusinessRuleException;
import dev.bootcamp.transactionservice.domain.model.charge.Charge;
import dev.bootcamp.transactionservice.domain.model.creditcard.CreditCard;
import java.math.BigDecimal;
import java.time.LocalDate;

public class ChargeCreditCardUseCaseImpl implements ChargeCreditCardUseCase {
    private final ChargeRepository chargeRepository;
    private final CreditCardClient creditCardClient;

    public ChargeCreditCardUseCaseImpl(ChargeRepository chargeRepository, CreditCardClient creditCardClient) {
        this.chargeRepository = chargeRepository;
        this.creditCardClient = creditCardClient;
    }

    @Override
    public Charge charge(Long customerId, Long creditCardId, BigDecimal amount, LocalDate date) {
        Charge charge = new Charge(creditCardId, amount, date);
        CreditCard creditCard = creditCardClient.findById(creditCardId)
                .orElseThrow(() -> new BusinessRuleException("La tarjeta de crédito no existe"));
        if (!creditCard.belongsTo(customerId)) {
            throw new BusinessRuleException("La tarjeta de crédito no pertenece al cliente");
        }
        creditCard.validateCharge(charge, chargeRepository.getDebt(creditCardId));
        return chargeRepository.save(charge);
    }
}
