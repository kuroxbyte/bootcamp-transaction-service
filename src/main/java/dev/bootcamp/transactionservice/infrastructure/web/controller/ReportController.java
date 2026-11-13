package dev.bootcamp.transactionservice.infrastructure.web.controller;

import dev.bootcamp.transactionservice.application.usecase.GenerateProductReportUseCase;
import dev.bootcamp.transactionservice.application.usecase.GetProductMovementsUseCase;
import dev.bootcamp.transactionservice.infrastructure.web.api.ReportsApi;
import dev.bootcamp.transactionservice.infrastructure.web.dto.ProductMovementResponseDto;
import dev.bootcamp.transactionservice.infrastructure.web.dto.ProductReportResponseDto;
import dev.bootcamp.transactionservice.infrastructure.web.mapper.TransactionWebMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

/**
 * Endpoints de reportes del banco.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class ReportController implements ReportsApi {

    private final GenerateProductReportUseCase generateProductReportUseCase;
    private final GetProductMovementsUseCase getProductMovementsUseCase;

    @Override
    public Mono<ResponseEntity<ProductReportResponseDto>> getProductReport(
            LocalDate from,
            LocalDate to,
            ServerWebExchange exchange) {
        log.info("Generating product report from={} to={}", from, to);
        return Mono.from(generateProductReportUseCase.execute(from, to).toFlowable())
                .map(report -> ResponseEntity.ok(TransactionWebMapper.toResponse(report)));
    }

    @Override
    public Mono<ResponseEntity<Flux<ProductMovementResponseDto>>> getCreditCardLastMovements(
            String customerId,
            String creditCardId,
            ServerWebExchange exchange) {
        log.debug(
                "Fetching last movements creditCardId={} customerId={}",
                creditCardId,
                customerId);
        return MovementResponses.of(
                getProductMovementsUseCase.getCreditCardLastMovements(customerId, creditCardId));
    }

    @Override
    public Mono<ResponseEntity<Flux<ProductMovementResponseDto>>> getDebitCardLastMovements(
            String customerId,
            String debitCardId,
            ServerWebExchange exchange) {
        log.debug(
                "Fetching last movements debitCardId={} customerId={}",
                debitCardId,
                customerId);
        return MovementResponses.of(
                getProductMovementsUseCase.getDebitCardLastMovements(customerId, debitCardId));
    }
}
