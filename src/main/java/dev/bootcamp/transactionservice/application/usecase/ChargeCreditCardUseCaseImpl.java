package dev.bootcamp.transactionservice.application.usecase;

import dev.bootcamp.transactionservice.application.port.ChargeRepository;
import dev.bootcamp.transactionservice.application.port.CreditCardClient;
import dev.bootcamp.transactionservice.domain.exception.NotFoundException;
import dev.bootcamp.transactionservice.domain.model.charge.Charge;
import dev.bootcamp.transactionservice.domain.model.creditcard.CreditCard;
import io.reactivex.rxjava3.core.Single;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Implementación de {@link ChargeCreditCardUseCase}. */
public class ChargeCreditCardUseCaseImpl implements ChargeCreditCardUseCase {
    private static final String CREDIT_CARD_NOT_FOUND = "La tarjeta de crédito no existe";
    private static final String CREDIT_CARD_NOT_OWNED =
            "La tarjeta de crédito no pertenece al cliente";

    private final ChargeRepository chargeRepository;
    private final CreditCardClient creditCardClient;

    /** Crea el caso de uso con los adaptadores de sus puertos. */
    public ChargeCreditCardUseCaseImpl(
            ChargeRepository chargeRepository, CreditCardClient creditCardClient) {
        this.chargeRepository = chargeRepository;
        this.creditCardClient = creditCardClient;
    }

    @Override
    public Single<Charge> charge(
            String customerId, String creditCardId, BigDecimal amount, LocalDate date) {
        return Single.fromCallable(() -> new Charge(creditCardId, amount, date))
                .flatMap(
                        charge ->
                                creditCardClient
                                        .findById(creditCardId)
                                        .switchIfEmpty(
                                                Single.error(
                                                        () ->
                                                                new NotFoundException(
                                                                        CREDIT_CARD_NOT_FOUND)))
                                        .map(creditCard -> ensureBelongsTo(creditCard, customerId))
                                        .flatMap(
                                                creditCard ->
                                                        chargeRepository
                                                                .getDebt(creditCardId)
                                                                .map(
                                                                        debt -> {
                                                                            creditCard
                                                                                    .validateCharge(
                                                                                            charge,
                                                                                            debt);
                                                                            return charge;
                                                                        })))
                .flatMap(chargeRepository::save);
    }

    private static CreditCard ensureBelongsTo(CreditCard creditCard, String customerId) {
        if (!creditCard.belongsTo(customerId)) {
            throw new NotFoundException(CREDIT_CARD_NOT_OWNED);
        }
        return creditCard;
    }
}
