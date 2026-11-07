package dev.bootcamp.transactionservice.infrastructure.web.mapper;

import dev.bootcamp.transactionservice.domain.model.charge.Charge;
import dev.bootcamp.transactionservice.domain.model.history.ProductMovement;
import dev.bootcamp.transactionservice.domain.model.movement.Movement;
import dev.bootcamp.transactionservice.domain.model.payment.Payment;
import dev.bootcamp.transactionservice.infrastructure.web.dto.ChargeResponseDto;
import dev.bootcamp.transactionservice.infrastructure.web.dto.MovementResponseDto;
import dev.bootcamp.transactionservice.infrastructure.web.dto.MovementTypeDto;
import dev.bootcamp.transactionservice.infrastructure.web.dto.PaymentResponseDto;
import dev.bootcamp.transactionservice.infrastructure.web.dto.PaymentTargetDto;
import dev.bootcamp.transactionservice.infrastructure.web.dto.ProductMovementResponseDto;
import dev.bootcamp.transactionservice.infrastructure.web.dto.ProductMovementTypeDto;

/** Conversión entre el dominio y los DTOs del contrato OpenAPI. */
public final class TransactionWebMapper {

    private TransactionWebMapper() {
    }

    public static MovementResponseDto toResponse(Movement movement) {
        return new MovementResponseDto(movement.accountId(), MovementTypeDto.valueOf(movement.type().name()),
                movement.amount(), movement.date());
    }

    public static PaymentResponseDto toResponse(Payment payment) {
        return new PaymentResponseDto(PaymentTargetDto.valueOf(payment.target().name()), payment.productId(),
                payment.amount(), payment.date());
    }

    public static ChargeResponseDto toResponse(Charge charge) {
        return new ChargeResponseDto(charge.creditCardId(), charge.amount(), charge.date());
    }

    public static ProductMovementResponseDto toResponse(ProductMovement movement) {
        return new ProductMovementResponseDto(ProductMovementTypeDto.valueOf(movement.type().name()),
                movement.amount(), movement.date());
    }
}
