package dev.bootcamp.transactionservice.infrastructure.web.controller;

import dev.bootcamp.transactionservice.application.usecase.GetProductMovementsUseCase;
import dev.bootcamp.transactionservice.application.usecase.PayCreditUseCase;
import dev.bootcamp.transactionservice.infrastructure.web.api.CreditTransactionsApi;
import dev.bootcamp.transactionservice.infrastructure.web.dto.AmountRequestDto;
import dev.bootcamp.transactionservice.infrastructure.web.dto.PaymentResponseDto;
import dev.bootcamp.transactionservice.infrastructure.web.dto.ProductMovementResponseDto;
import dev.bootcamp.transactionservice.infrastructure.web.mapper.TransactionWebMapper;
import java.time.Clock;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Endpoints de pagos y movimientos de créditos personales y empresariales. */
@Slf4j
@RestController
@RequiredArgsConstructor
public class CreditTransactionController implements CreditTransactionsApi {

    private final PayCreditUseCase payCreditUseCase;
    private final GetProductMovementsUseCase getProductMovementsUseCase;
    private final Clock clock;

    @Override
    public Mono<ResponseEntity<PaymentResponseDto>> payCredit(
            String customerId,
            String creditId,
            Mono<AmountRequestDto> amountRequestDto,
            ServerWebExchange exchange) {
        return amountRequestDto
                .flatMap(
                        request ->
                                Mono.from(
                                        payCreditUseCase
                                                .pay(
                                                        customerId,
                                                        creditId,
                                                        request.getAmount(),
                                                        LocalDate.now(clock))
                                                .toFlowable()))
                .doOnNext(
                        payment ->
                                log.info(
                                        "Credit payment registered creditId={} customerId={}"
                                                + " amount={}",
                                        creditId,
                                        customerId,
                                        payment.amount()))
                .map(
                        payment ->
                                ResponseEntity.status(HttpStatus.CREATED)
                                        .body(TransactionWebMapper.toResponse(payment)));
    }

    @Override
    public Mono<ResponseEntity<Flux<ProductMovementResponseDto>>> getCreditMovements(
            String customerId, String creditId, ServerWebExchange exchange) {
        log.debug("Fetching movements creditId={} customerId={}", creditId, customerId);
        return MovementResponses.of(
                getProductMovementsUseCase.getCreditMovements(customerId, creditId));
    }
}
