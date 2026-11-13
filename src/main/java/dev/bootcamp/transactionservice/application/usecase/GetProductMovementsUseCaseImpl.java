package dev.bootcamp.transactionservice.application.usecase;

import dev.bootcamp.transactionservice.application.port.AccountClient;
import dev.bootcamp.transactionservice.application.port.ChargeRepository;
import dev.bootcamp.transactionservice.application.port.CreditCardClient;
import dev.bootcamp.transactionservice.application.port.CreditClient;
import dev.bootcamp.transactionservice.application.port.DebitCardRepository;
import dev.bootcamp.transactionservice.application.port.MovementRepository;
import dev.bootcamp.transactionservice.application.port.PaymentRepository;
import dev.bootcamp.transactionservice.domain.exception.NotFoundException;
import dev.bootcamp.transactionservice.domain.model.history.ProductMovement;
import dev.bootcamp.transactionservice.domain.model.payment.PaymentTarget;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Implementación de {@link GetProductMovementsUseCase}.
 */
public class GetProductMovementsUseCaseImpl implements GetProductMovementsUseCase {
    private static final String CREDIT_CARD_NOT_FOUND = "La tarjeta de crédito no existe";
    private static final String CREDIT_CARD_NOT_OWNED =
            "La tarjeta de crédito no pertenece al cliente";

    private static final Comparator<ProductMovement> BY_DATE =
            Comparator.comparing(ProductMovement::date);

    private final AccountClient accountClient;
    private final CreditClient creditClient;
    private final CreditCardClient creditCardClient;
    private final MovementRepository movementRepository;
    private final PaymentRepository paymentRepository;
    private final ChargeRepository chargeRepository;
    private final DebitCardRepository debitCardRepository;

    /**
     * Crea el caso de uso con los adaptadores de sus puertos.
     */
    public GetProductMovementsUseCaseImpl(
            AccountClient accountClient,
            CreditClient creditClient,
            CreditCardClient creditCardClient,
            MovementRepository movementRepository,
            PaymentRepository paymentRepository,
            ChargeRepository chargeRepository,
            DebitCardRepository debitCardRepository) {
        this.accountClient = accountClient;
        this.creditClient = creditClient;
        this.creditCardClient = creditCardClient;
        this.movementRepository = movementRepository;
        this.paymentRepository = paymentRepository;
        this.chargeRepository = chargeRepository;
        this.debitCardRepository = debitCardRepository;
    }

    @Override
    public Flowable<ProductMovement> getAccountMovements(String customerId, String accountId) {
        return accountClient
                .findById(accountId)
                .switchIfEmpty(Single.error(() -> new NotFoundException("La cuenta no existe")))
                .flatMapPublisher(
                        account ->
                                account.belongsTo(customerId)
                                        ? movementRepository
                                                .findByAccountId(accountId)
                                                .map(ProductMovement::from)
                                                .sorted(BY_DATE)
                                        : Flowable.error(
                                                new NotFoundException(
                                                        "La cuenta no pertenece al cliente")));
    }

    @Override
    public Flowable<ProductMovement> getCreditMovements(String customerId, String creditId) {
        return creditClient
                .findById(creditId)
                .switchIfEmpty(Single.error(() -> new NotFoundException("El crédito no existe")))
                .flatMapPublisher(
                        credit ->
                                credit.belongsTo(customerId)
                                        ? paymentRepository
                                                .findByTargetAndProductId(
                                                        PaymentTarget.CREDIT, creditId)
                                                .map(ProductMovement::from)
                                                .sorted(BY_DATE)
                                        : Flowable.error(
                                                new NotFoundException(
                                                        "El crédito no pertenece al cliente")));
    }

    @Override
    public Flowable<ProductMovement> getCreditCardMovements(
            String customerId,
            String creditCardId) {
        return creditCardClient
                .findById(creditCardId)
                .switchIfEmpty(Single.error(() -> new NotFoundException(CREDIT_CARD_NOT_FOUND)))
                .flatMapPublisher(
                        creditCard ->
                                creditCard.belongsTo(customerId)
                                        ? Flowable.merge(
                                                        chargeRepository
                                                                .findByCreditCardId(creditCardId)
                                                                .map(ProductMovement::from),
                                                        paymentRepository
                                                                .findByTargetAndProductId(
                                                                        PaymentTarget.CREDIT_CARD,
                                                                        creditCardId)
                                                                .map(ProductMovement::from))
                                                .sorted(BY_DATE)
                                        : Flowable.error(
                                                new NotFoundException(CREDIT_CARD_NOT_OWNED)));
    }

    @Override
    public Flowable<ProductMovement> getCreditCardLastMovements(
            String customerId,
            String creditCardId) {
        return getCreditCardMovements(customerId, creditCardId)
                .toList()
                .flatMapPublisher(movements -> {
                    List<ProductMovement> newestFirst = new ArrayList<>(movements);
                    Collections.reverse(newestFirst);
                    return Flowable.fromIterable(newestFirst).take(LAST_MOVEMENTS);
                });
    }

    @Override
    public Flowable<ProductMovement> getDebitCardLastMovements(
            String customerId,
            String debitCardId) {
        return debitCardRepository.findById(debitCardId)
                .filter(card -> card.belongsTo(customerId))
                .switchIfEmpty(Single.error(() -> new NotFoundException(
                        "La tarjeta de débito no existe o no pertenece al cliente")))
                .flatMapPublisher(card -> movementRepository.findByDebitCardId(debitCardId)
                        .map(ProductMovement::from)
                        .sorted(BY_DATE.reversed())
                        .take(LAST_MOVEMENTS));
    }
}
