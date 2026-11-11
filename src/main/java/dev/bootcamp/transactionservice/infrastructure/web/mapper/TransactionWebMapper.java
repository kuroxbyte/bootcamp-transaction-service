package dev.bootcamp.transactionservice.infrastructure.web.mapper;

import dev.bootcamp.transactionservice.domain.model.account.DailyAverageBalance;
import dev.bootcamp.transactionservice.domain.model.charge.Charge;
import dev.bootcamp.transactionservice.domain.model.history.ProductMovement;
import dev.bootcamp.transactionservice.domain.model.movement.Movement;
import dev.bootcamp.transactionservice.domain.model.payment.Payment;
import dev.bootcamp.transactionservice.domain.model.report.ProductReport;
import dev.bootcamp.transactionservice.domain.model.report.ProductTypeReport;
import dev.bootcamp.transactionservice.domain.model.transfer.Transfer;
import dev.bootcamp.transactionservice.infrastructure.web.dto.ChargeResponseDto;
import dev.bootcamp.transactionservice.infrastructure.web.dto.DailyAverageBalanceResponseDto;
import dev.bootcamp.transactionservice.infrastructure.web.dto.MovementResponseDto;
import dev.bootcamp.transactionservice.infrastructure.web.dto.MovementTotalResponseDto;
import dev.bootcamp.transactionservice.infrastructure.web.dto.MovementTypeDto;
import dev.bootcamp.transactionservice.infrastructure.web.dto.PaymentResponseDto;
import dev.bootcamp.transactionservice.infrastructure.web.dto.PaymentTargetDto;
import dev.bootcamp.transactionservice.infrastructure.web.dto.ProductMovementResponseDto;
import dev.bootcamp.transactionservice.infrastructure.web.dto.ProductMovementTypeDto;
import dev.bootcamp.transactionservice.infrastructure.web.dto.ProductReportResponseDto;
import dev.bootcamp.transactionservice.infrastructure.web.dto.ProductTypeDto;
import dev.bootcamp.transactionservice.infrastructure.web.dto.ProductTypeReportResponseDto;
import dev.bootcamp.transactionservice.infrastructure.web.dto.TransferResponseDto;
import dev.bootcamp.transactionservice.infrastructure.web.dto.TransferTypeDto;

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
     * Convierte {@link Transfer} en su DTO de respuesta, con la comisión de cada cuenta.
     */
    public static TransferResponseDto toResponse(Transfer transfer) {
        return new TransferResponseDto(
                TransferTypeDto.valueOf(transfer.type().name()),
                transfer.outgoing().accountId(),
                transfer.incoming().accountId(),
                transfer.outgoing().amount(),
                transfer.outgoing().date(),
                transfer.outgoing().commission(),
                transfer.incoming().commission());
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

    /**
     * Convierte {@link ProductReport} en su DTO de respuesta.
     */
    public static ProductReportResponseDto toResponse(ProductReport report) {
        return new ProductReportResponseDto(
                report.from(),
                report.to(),
                report.products().stream().map(TransactionWebMapper::toResponse).toList());
    }

    private static ProductTypeReportResponseDto toResponse(ProductTypeReport product) {
        return new ProductTypeReportResponseDto(
                ProductTypeDto.valueOf(product.type().name()),
                product.openedProducts(),
                product.transactions(),
                product.commissions(),
                product.movements().stream()
                        .map(total -> new MovementTotalResponseDto(
                                ProductMovementTypeDto.valueOf(total.type().name()),
                                total.count(),
                                total.amount()))
                        .toList());
    }
}
