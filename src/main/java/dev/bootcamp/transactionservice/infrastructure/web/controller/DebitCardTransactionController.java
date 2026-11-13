package dev.bootcamp.transactionservice.infrastructure.web.controller;

import dev.bootcamp.transactionservice.application.usecase.RegisterMovementUseCase;
import dev.bootcamp.transactionservice.infrastructure.web.api.DebitCardTransactionsApi;
import dev.bootcamp.transactionservice.infrastructure.web.dto.AmountRequestDto;
import dev.bootcamp.transactionservice.infrastructure.web.dto.MovementResponseDto;
import dev.bootcamp.transactionservice.infrastructure.web.mapper.TransactionWebMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.time.LocalDate;

/**
 * Endpoints de pagos con tarjeta de débito.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class DebitCardTransactionController implements DebitCardTransactionsApi {

    private final RegisterMovementUseCase registerMovementUseCase;
    private final Clock clock;

    @Override
    public Mono<ResponseEntity<MovementResponseDto>> payWithDebitCard(
            String customerId,
            String debitCardId,
            Mono<AmountRequestDto> amountRequestDto,
            ServerWebExchange exchange) {
        return amountRequestDto
                .flatMap(request -> Mono.from(registerMovementUseCase
                        .payWithDebitCard(
                                customerId,
                                debitCardId,
                                request.getAmount(),
                                LocalDate.now(clock))
                        .toFlowable()))
                .doOnNext(movement -> log.info(
                        "Debit card payment registered debitCardId={} accountId={} amount={}",
                        debitCardId,
                        movement.accountId(),
                        movement.amount()))
                .map(movement -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(TransactionWebMapper.toResponse(movement)));
    }
}
