package dev.bootcamp.transactionservice.infrastructure.web.mapper;

import dev.bootcamp.transactionservice.domain.model.account.DailyAverageBalance;
import dev.bootcamp.transactionservice.domain.model.charge.Charge;
import dev.bootcamp.transactionservice.domain.model.history.ProductMovement;
import dev.bootcamp.transactionservice.domain.model.movement.Movement;
import dev.bootcamp.transactionservice.domain.model.payment.Payment;
import dev.bootcamp.transactionservice.infrastructure.web.dto.ChargeResponseDto;
import dev.bootcamp.transactionservice.infrastructure.web.dto.DailyAverageBalanceResponseDto;
import dev.bootcamp.transactionservice.infrastructure.web.dto.MovementResponseDto;
import dev.bootcamp.transactionservice.infrastructure.web.dto.MovementTypeDto;
import dev.bootcamp.transactionservice.infrastructure.web.dto.PaymentResponseDto;
import dev.bootcamp.transactionservice.infrastructure.web.dto.PaymentTargetDto;
import dev.bootcamp.transactionservice.infrastructure.web.dto.ProductMovementResponseDto;
import dev.bootcamp.transactionservice.infrastructure.web.dto.ProductMovementTypeDto;

/**
 * Conversión entre el dominio y los DTOs del contrato OpenAPI.
 */
public final class TransactionWebMapper {

    private TransactionWebMapper() {}

    /**
     * Convierte {@link Movement} en su DTO de respuesta.
     */
    public static MovementResponseDto toResponse(Movement movement) {
        return new MovementResponseDto(
                movement.accountId(),
                MovementTypeDto.valueOf(movement.type().name()),
                movement.amount(),
                movement.date(),
                movement.commission());
    }

    /**
     * Convierte {@link Payment} en su DTO de respuesta.
     */
    public static PaymentResponseDto toResponse(Payment payment) {
        return new PaymentResponseDto(
                PaymentTargetDto.valueOf(payment.target().name()),
                payment.productId(),
                payment.amount(),
                payment.date());
    }

    /**
     * Convierte {@link Charge} en su DTO de respuesta.
     */
    public static ChargeResponseDto toResponse(Charge charge) {
        return new ChargeResponseDto(charge.creditCardId(), charge.amount(), charge.date());
    }

    /**
     * Convierte {@link ProductMovement} en su DTO de respuesta.
     */
    public static ProductMovementResponseDto toResponse(ProductMovement movement) {
        return new ProductMovementResponseDto(
                ProductMovementTypeDto.valueOf(movement.type().name()),
                movement.amount(),
                movement.date())
                .commission(movement.commission());
    }

    /**
     * Convierte {@link DailyAverageBalance} en su DTO de respuesta; el mínimo y si se alcanza solo
     * vienen si la cuenta exige un mínimo.
     */
    public static DailyAverageBalanceResponseDto toResponse(
            String accountId,
            DailyAverageBalance average) {
        return new DailyAverageBalanceResponseDto(
                accountId,
                average.month().toString(),
                average.days(),
                average.average())
                .minimumDailyAverage(average.minimumDailyAverage())
                .meetsMinimum(average.meetsMinimum());
    }
}
