package dev.bootcamp.transactionservice.infrastructure.config;

import dev.bootcamp.transactionservice.application.port.AccountClient;
import dev.bootcamp.transactionservice.application.port.BankProductClient;
import dev.bootcamp.transactionservice.application.port.ChargeRepository;
import dev.bootcamp.transactionservice.application.port.CreditCardClient;
import dev.bootcamp.transactionservice.application.port.CreditClient;
import dev.bootcamp.transactionservice.application.port.DebitCardRepository;
import dev.bootcamp.transactionservice.application.port.MovementRepository;
import dev.bootcamp.transactionservice.application.port.PaymentRepository;
import dev.bootcamp.transactionservice.application.port.TransactionEventPublisher;
import dev.bootcamp.transactionservice.application.usecase.ChargeCreditCardUseCase;
import dev.bootcamp.transactionservice.application.usecase.ChargeCreditCardUseCaseImpl;
import dev.bootcamp.transactionservice.application.usecase.GenerateProductReportUseCase;
import dev.bootcamp.transactionservice.application.usecase.GenerateProductReportUseCaseImpl;
import dev.bootcamp.transactionservice.application.usecase.GetAvailableBalanceUseCase;
import dev.bootcamp.transactionservice.application.usecase.GetAvailableBalanceUseCaseImpl;
import dev.bootcamp.transactionservice.application.usecase.GetDailyAverageBalanceUseCase;
import dev.bootcamp.transactionservice.application.usecase.GetDailyAverageBalanceUseCaseImpl;
import dev.bootcamp.transactionservice.application.usecase.GetProductMovementsUseCase;
import dev.bootcamp.transactionservice.application.usecase.GetProductMovementsUseCaseImpl;
import dev.bootcamp.transactionservice.application.usecase.PayCreditCardUseCase;
import dev.bootcamp.transactionservice.application.usecase.PayCreditCardUseCaseImpl;
import dev.bootcamp.transactionservice.application.usecase.PayCreditUseCase;
import dev.bootcamp.transactionservice.application.usecase.PayCreditUseCaseImpl;
import dev.bootcamp.transactionservice.application.usecase.RegisterMovementUseCase;
import dev.bootcamp.transactionservice.application.usecase.RegisterMovementUseCaseImpl;
import dev.bootcamp.transactionservice.application.usecase.SaveDebitCardUseCase;
import dev.bootcamp.transactionservice.application.usecase.SaveDebitCardUseCaseImpl;
import dev.bootcamp.transactionservice.application.usecase.TransferUseCase;
import dev.bootcamp.transactionservice.application.usecase.TransferUseCaseImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Registra los casos de uso como beans. Los casos de uso no dependen de Spring; aquí se les
 * inyectan los adaptadores de sus puertos.
 */
@Configuration
public class UseCaseConfig {

    /**
     * Reloj con el que el servidor asigna la fecha de cada transacción.
     */
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }

    /**
     * Registra el caso de uso {@link RegisterMovementUseCase}.
     */
    @Bean
    public RegisterMovementUseCase registerMovementUseCase(
            MovementRepository movementRepository,
            AccountClient accountClient,
            DebitCardRepository debitCardRepository) {
        return new RegisterMovementUseCaseImpl(
                movementRepository,
                accountClient,
                debitCardRepository);
    }

    /**
     * Registra el caso de uso {@link PayCreditUseCase}.
     */
    @Bean
    public PayCreditUseCase payCreditUseCase(
            PaymentRepository paymentRepository,
            CreditClient creditClient,
            TransactionEventPublisher transactionEventPublisher) {
        return new PayCreditUseCaseImpl(
                paymentRepository,
                creditClient,
                transactionEventPublisher);
    }

    /**
     * Registra el caso de uso {@link PayCreditCardUseCase}.
     */
    @Bean
    public PayCreditCardUseCase payCreditCardUseCase(
            PaymentRepository paymentRepository,
            CreditCardClient creditCardClient,
            TransactionEventPublisher transactionEventPublisher) {
        return new PayCreditCardUseCaseImpl(
                paymentRepository,
                creditCardClient,
                transactionEventPublisher);
    }

    /**
     * Registra el caso de uso {@link ChargeCreditCardUseCase}.
     */
    @Bean
    public ChargeCreditCardUseCase chargeCreditCardUseCase(
            ChargeRepository chargeRepository,
            CreditCardClient creditCardClient,
            TransactionEventPublisher transactionEventPublisher) {
        return new ChargeCreditCardUseCaseImpl(
                chargeRepository,
                creditCardClient,
                transactionEventPublisher);
    }

    /**
     * Registra el caso de uso {@link GetAvailableBalanceUseCase}.
     */
    @Bean
    public GetAvailableBalanceUseCase getAvailableBalanceUseCase(
            AccountClient accountClient,
            MovementRepository movementRepository,
            CreditCardClient creditCardClient,
            ChargeRepository chargeRepository) {
        return new GetAvailableBalanceUseCaseImpl(
                accountClient, movementRepository, creditCardClient, chargeRepository);
    }

    /**
     * Registra el caso de uso {@link GetProductMovementsUseCase}.
     */
    @Bean
    public GetProductMovementsUseCase getProductMovementsUseCase(
            AccountClient accountClient,
            CreditClient creditClient,
            CreditCardClient creditCardClient,
            MovementRepository movementRepository,
            PaymentRepository paymentRepository,
            ChargeRepository chargeRepository,
            DebitCardRepository debitCardRepository) {
        return new GetProductMovementsUseCaseImpl(
                accountClient,
                creditClient,
                creditCardClient,
                movementRepository,
                paymentRepository,
                chargeRepository,
                debitCardRepository);
    }

    /**
     * Registra el caso de uso {@link GetDailyAverageBalanceUseCase}.
     */
    @Bean
    public GetDailyAverageBalanceUseCase getDailyAverageBalanceUseCase(
            AccountClient accountClient,
            MovementRepository movementRepository,
            Clock clock) {
        return new GetDailyAverageBalanceUseCaseImpl(accountClient, movementRepository, clock);
    }

    /**
     * Registra el caso de uso {@link TransferUseCase}.
     */
    @Bean
    public TransferUseCase transferUseCase(
            AccountClient accountClient,
            MovementRepository movementRepository) {
        return new TransferUseCaseImpl(accountClient, movementRepository);
    }

    /**
     * Registra el caso de uso {@link GenerateProductReportUseCase}.
     */
    @Bean
    public GenerateProductReportUseCase generateProductReportUseCase(
            BankProductClient bankProductClient,
            MovementRepository movementRepository,
            PaymentRepository paymentRepository,
            ChargeRepository chargeRepository) {
        return new GenerateProductReportUseCaseImpl(
                bankProductClient,
                movementRepository,
                paymentRepository,
                chargeRepository);
    }

    /**
     * Registra el caso de uso {@link SaveDebitCardUseCase}.
     */
    @Bean
    public SaveDebitCardUseCase saveDebitCardUseCase(DebitCardRepository debitCardRepository) {
        return new SaveDebitCardUseCaseImpl(debitCardRepository);
    }
}
