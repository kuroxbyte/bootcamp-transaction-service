package dev.bootcamp.transactionservice.application.usecase;

import dev.bootcamp.transactionservice.application.port.AccountClient;
import dev.bootcamp.transactionservice.application.port.ChargeRepository;
import dev.bootcamp.transactionservice.application.port.CreditCardClient;
import dev.bootcamp.transactionservice.application.port.MovementRepository;
import dev.bootcamp.transactionservice.domain.exception.BusinessRuleException;
import dev.bootcamp.transactionservice.domain.model.account.Account;
import dev.bootcamp.transactionservice.domain.model.creditcard.CreditCard;
import io.reactivex.rxjava3.core.Single;
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
    public Single<BigDecimal> getAccountBalance(String customerId, String accountId) {
        return accountClient.findById(accountId)
                .switchIfEmpty(Single.error(() -> new BusinessRuleException("La cuenta no existe")))
                .map(account -> ensureBelongsTo(account, customerId))
                .flatMap(account -> movementRepository.getBalance(accountId));
    }

    @Override
    public Single<BigDecimal> getCreditCardAvailableCredit(String customerId, String creditCardId) {
        return creditCardClient.findById(creditCardId)
                .switchIfEmpty(Single.error(() -> new BusinessRuleException("La tarjeta de crédito no existe")))
                .map(creditCard -> ensureBelongsTo(creditCard, customerId))
                .flatMap(creditCard -> chargeRepository.getDebt(creditCardId).map(creditCard::availableCredit));
    }

    private static Account ensureBelongsTo(Account account, String customerId) {
        if (!account.belongsTo(customerId)) {
            throw new BusinessRuleException("La cuenta no pertenece al cliente");
        }
        return account;
    }

    private static CreditCard ensureBelongsTo(CreditCard creditCard, String customerId) {
        if (!creditCard.belongsTo(customerId)) {
            throw new BusinessRuleException("La tarjeta de crédito no pertenece al cliente");
        }
        return creditCard;
    }
}
