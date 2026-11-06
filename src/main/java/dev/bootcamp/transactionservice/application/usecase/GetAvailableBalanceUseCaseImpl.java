package dev.bootcamp.transactionservice.application.usecase;

import dev.bootcamp.transactionservice.application.port.AccountClient;
import dev.bootcamp.transactionservice.application.port.ChargeRepository;
import dev.bootcamp.transactionservice.application.port.CreditCardClient;
import dev.bootcamp.transactionservice.application.port.MovementRepository;
import dev.bootcamp.transactionservice.domain.exception.BusinessRuleException;
import dev.bootcamp.transactionservice.domain.model.account.Account;
import dev.bootcamp.transactionservice.domain.model.creditcard.CreditCard;
import java.math.BigDecimal;

public class GetAvailableBalanceUseCaseImpl implements GetAvailableBalanceUseCase {
    private final AccountClient accountClient;
    private final MovementRepository movementRepository;
    private final CreditCardClient creditCardClient;
    private final ChargeRepository chargeRepository;

    public GetAvailableBalanceUseCaseImpl(AccountClient accountClient, MovementRepository movementRepository,
                                          CreditCardClient creditCardClient, ChargeRepository chargeRepository) {
        this.accountClient = accountClient;
        this.movementRepository = movementRepository;
        this.creditCardClient = creditCardClient;
        this.chargeRepository = chargeRepository;
    }

    @Override
    public BigDecimal getAccountBalance(Long customerId, Long accountId) {
        Account account = accountClient.findById(accountId)
                .orElseThrow(() -> new BusinessRuleException("La cuenta no existe"));
        if (!account.belongsTo(customerId)) {
            throw new BusinessRuleException("La cuenta no pertenece al cliente");
        }
        return movementRepository.getBalance(accountId);
    }

    @Override
    public BigDecimal getCreditCardAvailableCredit(Long customerId, Long creditCardId) {
        CreditCard creditCard = creditCardClient.findById(creditCardId)
                .orElseThrow(() -> new BusinessRuleException("La tarjeta de crédito no existe"));
        if (!creditCard.belongsTo(customerId)) {
            throw new BusinessRuleException("La tarjeta de crédito no pertenece al cliente");
        }
        return creditCard.availableCredit(chargeRepository.getDebt(creditCardId));
    }
}
