package dev.bootcamp.transactionservice.infrastructure.web.controller;

import dev.bootcamp.transactionservice.application.usecase.ChargeCreditCardUseCase;
import dev.bootcamp.transactionservice.application.usecase.GetAvailableBalanceUseCase;
import dev.bootcamp.transactionservice.application.usecase.GetProductMovementsUseCase;
import dev.bootcamp.transactionservice.application.usecase.PayCreditCardUseCase;
import dev.bootcamp.transactionservice.infrastructure.web.api.CreditCardTransactionsApi;
import dev.bootcamp.transactionservice.infrastructure.web.dto.AmountRequestDto;
import dev.bootcamp.transactionservice.infrastructure.web.dto.AvailableCreditResponseDto;
import dev.bootcamp.transactionservice.infrastructure.web.dto.ChargeResponseDto;
import dev.bootcamp.transactionservice.infrastructure.web.dto.PaymentResponseDto;
import dev.bootcamp.transactionservice.infrastructure.web.dto.ProductMovementResponseDto;
import dev.bootcamp.transactionservice.infrastructure.web.mapper.TransactionWebMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.time.LocalDate;

/**
 * Endpoints de consumos, pagos, crédito disponible y movimientos de tarjetas.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class CreditCardTransactionController implements CreditCardTransactionsApi {

    private final ChargeCreditCardUseCase chargeCreditCardUseCase;
    private final PayCreditCardUseCase payCreditCardUseCase;
    private final GetAvailableBalanceUseCase getAvailableBalanceUseCase;
    private final GetProductMovementsUseCase getProductMovementsUseCase;
    private final Clock clock;

    @Override
    public Mono<ResponseEntity<ChargeResponseDto>> chargeCreditCard(
            String customerId,
            String creditCardId,
            Mono<AmountRequestDto> amountRequestDto,
            ServerWebExchange exchange) {
        return amountRequestDto
                .flatMap(
                        request ->
                                Mono.from(
                                        chargeCreditCardUseCase
                                                .charge(
                                                        customerId,
                                                        creditCardId,
                                                        request.getAmount(),
                                                        LocalDate.now(clock))
                                                .toFlowable()))
                .doOnNext(
                        charge ->
                                log.info(
                                        "Charge registered creditCardId={} customerId={} amount={}",
                                        creditCardId,
                                        customerId,
                                        charge.amount()))
                .map(
                        charge ->
                                ResponseEntity.status(HttpStatus.CREATED)
                                        .body(TransactionWebMapper.toResponse(charge)));
    }

    @Override
    public Mono<ResponseEntity<PaymentResponseDto>> payThirdPartyCreditCard(
            String customerId,
            String creditCardId,
            Mono<AmountRequestDto> amountRequestDto,
            ServerWebExchange exchange) {
        return amountRequestDto
                .flatMap(request -> Mono.from(payCreditCardUseCase
                        .payThirdParty(
                                customerId,
                                creditCardId,
                                request.getAmount(),
                                LocalDate.now(clock))
                        .toFlowable()))
                .doOnNext(payment -> log.info(
                        "Third-party card payment registered creditCardId={} payerCustomerId={}"
                                + " amount={}",
                        creditCardId,
                        customerId,
                        payment.amount()))
                .map(payment -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(TransactionWebMapper.toResponse(payment)));
    }

    @Override
    public Mono<ResponseEntity<PaymentResponseDto>> payCreditCard(
            String customerId,
            String creditCardId,
            Mono<AmountRequestDto> amountRequestDto,
            ServerWebExchange exchange) {
        return amountRequestDto
                .flatMap(
                        request ->
                                Mono.from(
                                        payCreditCardUseCase
                                                .pay(
                                                        customerId,
                                                        creditCardId,
                                                        request.getAmount(),
                                                        LocalDate.now(clock))
                                                .toFlowable()))
                .doOnNext(
                        payment ->
                                log.info(
                                        "Credit card payment registered creditCardId={}"
                                                + " customerId={} amount={}",
                                        creditCardId,
                                        customerId,
                                        payment.amount()))
                .map(
                        payment ->
                                ResponseEntity.status(HttpStatus.CREATED)
                                        .body(TransactionWebMapper.toResponse(payment)));
    }

    @Override
    public Mono<ResponseEntity<AvailableCreditResponseDto>> getCreditCardAvailableCredit(
            String customerId,
            String creditCardId,
            ServerWebExchange exchange) {
        log.debug(
                "Fetching available credit creditCardId={} customerId={}",
                creditCardId,
                customerId);
        return Mono.from(
                        getAvailableBalanceUseCase
                                .getCreditCardAvailableCredit(customerId, creditCardId)
                                .toFlowable())
                .map(
                        available ->
                                ResponseEntity.ok(
                                        new AvailableCreditResponseDto(creditCardId, available)));
    }

    @Override
    public Mono<ResponseEntity<Flux<ProductMovementResponseDto>>> getCreditCardMovements(
            String customerId,
            String creditCardId,
            ServerWebExchange exchange) {
        log.debug("Fetching movements creditCardId={} customerId={}", creditCardId, customerId);
        return MovementResponses.of(
                getProductMovementsUseCase.getCreditCardMovements(customerId, creditCardId));
    }
}
