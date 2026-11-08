package dev.bootcamp.transactionservice.infrastructure.config;

import dev.bootcamp.transactionservice.application.port.AccountClient;
import dev.bootcamp.transactionservice.application.port.ChargeRepository;
import dev.bootcamp.transactionservice.application.port.CreditCardClient;
import dev.bootcamp.transactionservice.application.port.CreditClient;
import dev.bootcamp.transactionservice.application.port.MovementRepository;
import dev.bootcamp.transactionservice.application.port.PaymentRepository;
import dev.bootcamp.transactionservice.application.usecase.ChargeCreditCardUseCase;
import dev.bootcamp.transactionservice.application.usecase.ChargeCreditCardUseCaseImpl;
import dev.bootcamp.transactionservice.application.usecase.GetAvailableBalanceUseCase;
import dev.bootcamp.transactionservice.application.usecase.GetAvailableBalanceUseCaseImpl;
import dev.bootcamp.transactionservice.application.usecase.GetProductMovementsUseCase;
import dev.bootcamp.transactionservice.application.usecase.GetProductMovementsUseCaseImpl;
import dev.bootcamp.transactionservice.application.usecase.PayCreditCardUseCase;
import dev.bootcamp.transactionservice.application.usecase.PayCreditCardUseCaseImpl;
import dev.bootcamp.transactionservice.application.usecase.PayCreditUseCase;
import dev.bootcamp.transactionservice.application.usecase.PayCreditUseCaseImpl;
import dev.bootcamp.transactionservice.application.usecase.RegisterMovementUseCase;
import dev.bootcamp.transactionservice.application.usecase.RegisterMovementUseCaseImpl;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registra los casos de uso como beans. Los casos de uso no dependen de Spring; aquí se les
 * inyectan los adaptadores de sus puertos.
 */
@Configuration
public class UseCaseConfig {

    /** Reloj con el que el servidor asigna la fecha de cada transacción. */
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }

    /** Registra el caso de uso {@link RegisterMovementUseCase}. */
    @Bean
    public RegisterMovementUseCase registerMovementUseCase(
            MovementRepository movementRepository, AccountClient accountClient) {
        return new RegisterMovementUseCaseImpl(movementRepository, accountClient);
    }

    /** Registra el caso de uso {@link PayCreditUseCase}. */
    @Bean
    public PayCreditUseCase payCreditUseCase(
            PaymentRepository paymentRepository, CreditClient creditClient) {
        return new PayCreditUseCaseImpl(paymentRepository, creditClient);
    }

    /** Registra el caso de uso {@link PayCreditCardUseCase}. */
    @Bean
    public PayCreditCardUseCase payCreditCardUseCase(
            PaymentRepository paymentRepository, CreditCardClient creditCardClient) {
        return new PayCreditCardUseCaseImpl(paymentRepository, creditCardClient);
    }

    /** Registra el caso de uso {@link ChargeCreditCardUseCase}. */
    @Bean
    public ChargeCreditCardUseCase chargeCreditCardUseCase(
            ChargeRepository chargeRepository, CreditCardClient creditCardClient) {
        return new ChargeCreditCardUseCaseImpl(chargeRepository, creditCardClient);
    }

    /** Registra el caso de uso {@link GetAvailableBalanceUseCase}. */
    @Bean
    public GetAvailableBalanceUseCase getAvailableBalanceUseCase(
            AccountClient accountClient,
            MovementRepository movementRepository,
            CreditCardClient creditCardClient,
            ChargeRepository chargeRepository) {
        return new GetAvailableBalanceUseCaseImpl(
                accountClient, movementRepository, creditCardClient, chargeRepository);
    }

    /** Registra el caso de uso {@link GetProductMovementsUseCase}. */
    @Bean
    public GetProductMovementsUseCase getProductMovementsUseCase(
            AccountClient accountClient,
            CreditClient creditClient,
            CreditCardClient creditCardClient,
            MovementRepository movementRepository,
            PaymentRepository paymentRepository,
            ChargeRepository chargeRepository) {
        return new GetProductMovementsUseCaseImpl(
                accountClient,
                creditClient,
                creditCardClient,
                movementRepository,
                paymentRepository,
                chargeRepository);
    }
}
