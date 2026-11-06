package dev.bootcamp.transactionservice.application.usecase;

import dev.bootcamp.transactionservice.application.port.AccountClient;
import dev.bootcamp.transactionservice.application.port.ChargeRepository;
import dev.bootcamp.transactionservice.application.port.CreditCardClient;
import dev.bootcamp.transactionservice.application.port.CreditClient;
import dev.bootcamp.transactionservice.application.port.MovementRepository;
import dev.bootcamp.transactionservice.application.port.PaymentRepository;
import dev.bootcamp.transactionservice.domain.exception.BusinessRuleException;
import dev.bootcamp.transactionservice.domain.model.account.Account;
import dev.bootcamp.transactionservice.domain.model.credit.Credit;
import dev.bootcamp.transactionservice.domain.model.creditcard.CreditCard;
import dev.bootcamp.transactionservice.domain.model.history.ProductMovement;
import dev.bootcamp.transactionservice.domain.model.payment.PaymentTarget;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public class GetProductMovementsUseCaseImpl implements GetProductMovementsUseCase {
    private static final Comparator<ProductMovement> BY_DATE = Comparator.comparing(ProductMovement::date);

    private final AccountClient accountClient;
    private final CreditClient creditClient;
    private final CreditCardClient creditCardClient;
    private final MovementRepository movementRepository;
    private final PaymentRepository paymentRepository;
    private final ChargeRepository chargeRepository;

    public GetProductMovementsUseCaseImpl(AccountClient accountClient, CreditClient creditClient,
                                          CreditCardClient creditCardClient, MovementRepository movementRepository,
                                          PaymentRepository paymentRepository, ChargeRepository chargeRepository) {
        this.accountClient = accountClient;
        this.creditClient = creditClient;
        this.creditCardClient = creditCardClient;
        this.movementRepository = movementRepository;
        this.paymentRepository = paymentRepository;
        this.chargeRepository = chargeRepository;
    }

    @Override
    public List<ProductMovement> getAccountMovements(Long customerId, Long accountId) {
        Account account = accountClient.findById(accountId)
                .orElseThrow(() -> new BusinessRuleException("La cuenta no existe"));
        if (!account.belongsTo(customerId)) {
            throw new BusinessRuleException("La cuenta no pertenece al cliente");
        }
        return movementRepository.findByAccountId(accountId).stream()
                .map(ProductMovement::from)
                .sorted(BY_DATE)
                .toList();
    }

    @Override
    public List<ProductMovement> getCreditMovements(Long customerId, Long creditId) {
        Credit credit = creditClient.findById(creditId)
                .orElseThrow(() -> new BusinessRuleException("El crédito no existe"));
        if (!credit.belongsTo(customerId)) {
            throw new BusinessRuleException("El crédito no pertenece al cliente");
        }
        return paymentRepository.findByTargetAndProductId(PaymentTarget.CREDIT, creditId).stream()
                .map(ProductMovement::from)
                .sorted(BY_DATE)
                .toList();
    }

    @Override
    public List<ProductMovement> getCreditCardMovements(Long customerId, Long creditCardId) {
        CreditCard creditCard = creditCardClient.findById(creditCardId)
                .orElseThrow(() -> new BusinessRuleException("La tarjeta de crédito no existe"));
        if (!creditCard.belongsTo(customerId)) {
            throw new BusinessRuleException("La tarjeta de crédito no pertenece al cliente");
        }
        Stream<ProductMovement> charges = chargeRepository.findByCreditCardId(creditCardId).stream()
                .map(ProductMovement::from);
        Stream<ProductMovement> payments = paymentRepository
                .findByTargetAndProductId(PaymentTarget.CREDIT_CARD, creditCardId).stream()
                .map(ProductMovement::from);
        return Stream.concat(charges, payments)
                .sorted(BY_DATE)
                .toList();
    }
}
