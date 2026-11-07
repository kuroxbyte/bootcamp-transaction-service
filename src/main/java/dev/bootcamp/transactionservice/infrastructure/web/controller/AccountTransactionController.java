package dev.bootcamp.transactionservice.infrastructure.web.controller;

import dev.bootcamp.transactionservice.application.usecase.GetAvailableBalanceUseCase;
import dev.bootcamp.transactionservice.application.usecase.GetProductMovementsUseCase;
import dev.bootcamp.transactionservice.application.usecase.RegisterMovementUseCase;
import dev.bootcamp.transactionservice.infrastructure.web.api.AccountTransactionsApi;
import dev.bootcamp.transactionservice.infrastructure.web.dto.AccountBalanceResponseDto;
import dev.bootcamp.transactionservice.infrastructure.web.dto.AmountRequestDto;
import dev.bootcamp.transactionservice.infrastructure.web.dto.MovementResponseDto;
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

@Slf4j
@RestController
@RequiredArgsConstructor
public class AccountTransactionController implements AccountTransactionsApi {

    private final RegisterMovementUseCase registerMovementUseCase;
    private final GetAvailableBalanceUseCase getAvailableBalanceUseCase;
    private final GetProductMovementsUseCase getProductMovementsUseCase;
    private final Clock clock;

    @Override
    public Mono<ResponseEntity<MovementResponseDto>> deposit(String xCustomerId, String accountId,
                                                             Mono<AmountRequestDto> amountRequestDto,
                                                             ServerWebExchange exchange) {
        return amountRequestDto
                .flatMap(request -> Mono.from(registerMovementUseCase
                        .deposit(xCustomerId, accountId, request.getAmount(), LocalDate.now(clock))
                        .toFlowable()))
                .doOnNext(movement -> log.info("Deposit registered accountId={} customerId={} amount={}",
                        accountId, xCustomerId, movement.amount()))
                .map(movement -> ResponseEntity.status(HttpStatus.CREATED).body(TransactionWebMapper.toResponse(movement)));
    }

    @Override
    public Mono<ResponseEntity<MovementResponseDto>> withdraw(String xCustomerId, String accountId,
                                                              Mono<AmountRequestDto> amountRequestDto,
                                                              ServerWebExchange exchange) {
        return amountRequestDto
                .flatMap(request -> Mono.from(registerMovementUseCase
                        .withdraw(xCustomerId, accountId, request.getAmount(), LocalDate.now(clock))
                        .toFlowable()))
                .doOnNext(movement -> log.info("Withdrawal registered accountId={} customerId={} amount={}",
                        accountId, xCustomerId, movement.amount()))
                .map(movement -> ResponseEntity.status(HttpStatus.CREATED).body(TransactionWebMapper.toResponse(movement)));
    }

    @Override
    public Mono<ResponseEntity<AccountBalanceResponseDto>> getAccountBalance(String xCustomerId, String accountId,
                                                                             ServerWebExchange exchange) {
        log.debug("Fetching balance accountId={} customerId={}", accountId, xCustomerId);
        return Mono.from(getAvailableBalanceUseCase.getAccountBalance(xCustomerId, accountId).toFlowable())
                .map(balance -> ResponseEntity.ok(new AccountBalanceResponseDto(accountId, balance)));
    }

    @Override
    public Mono<ResponseEntity<Flux<ProductMovementResponseDto>>> getAccountMovements(String xCustomerId,
                                                                                      String accountId,
                                                                                      ServerWebExchange exchange) {
        log.debug("Fetching movements accountId={} customerId={}", accountId, xCustomerId);
        return MovementResponses.of(getProductMovementsUseCase.getAccountMovements(xCustomerId, accountId));
    }
}
