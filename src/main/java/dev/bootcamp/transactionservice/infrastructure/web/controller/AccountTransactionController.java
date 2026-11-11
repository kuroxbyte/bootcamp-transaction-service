package dev.bootcamp.transactionservice.infrastructure.web.controller;

import dev.bootcamp.transactionservice.application.usecase.GetAvailableBalanceUseCase;
import dev.bootcamp.transactionservice.application.usecase.GetDailyAverageBalanceUseCase;
import dev.bootcamp.transactionservice.application.usecase.GetProductMovementsUseCase;
import dev.bootcamp.transactionservice.application.usecase.RegisterMovementUseCase;
import dev.bootcamp.transactionservice.application.usecase.TransferUseCase;
import dev.bootcamp.transactionservice.domain.model.transfer.Transfer;
import dev.bootcamp.transactionservice.infrastructure.web.api.AccountTransactionsApi;
import dev.bootcamp.transactionservice.infrastructure.web.dto.AccountBalanceResponseDto;
import dev.bootcamp.transactionservice.infrastructure.web.dto.AmountRequestDto;
import dev.bootcamp.transactionservice.infrastructure.web.dto.DailyAverageBalanceResponseDto;
import dev.bootcamp.transactionservice.infrastructure.web.dto.MovementResponseDto;
import dev.bootcamp.transactionservice.infrastructure.web.dto.ProductMovementResponseDto;
import dev.bootcamp.transactionservice.infrastructure.web.dto.TransferRequestDto;
import dev.bootcamp.transactionservice.infrastructure.web.dto.TransferResponseDto;
import dev.bootcamp.transactionservice.infrastructure.web.mapper.TransactionWebMapper;
import io.reactivex.rxjava3.core.Single;
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
import java.time.YearMonth;

/**
 * Endpoints de depósitos, retiros, transferencias, saldo y movimientos de cuentas bancarias.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class AccountTransactionController implements AccountTransactionsApi {

    private final RegisterMovementUseCase registerMovementUseCase;
    private final GetAvailableBalanceUseCase getAvailableBalanceUseCase;
    private final GetProductMovementsUseCase getProductMovementsUseCase;
    private final GetDailyAverageBalanceUseCase getDailyAverageBalanceUseCase;
    private final TransferUseCase transferUseCase;
    private final Clock clock;

    @Override
    public Mono<ResponseEntity<MovementResponseDto>> deposit(
            String customerId,
            String accountId,
            Mono<AmountRequestDto> amountRequestDto,
            ServerWebExchange exchange) {
        return amountRequestDto
                .flatMap(
                        request ->
                                Mono.from(
                                        registerMovementUseCase
                                                .deposit(
                                                        customerId,
                                                        accountId,
                                                        request.getAmount(),
                                                        LocalDate.now(clock))
                                                .toFlowable()))
                .doOnNext(
                        movement ->
                                log.info(
                                        "Deposit registered accountId={} customerId={} amount={}",
                                        accountId,
                                        customerId,
                                        movement.amount()))
                .map(
                        movement ->
                                ResponseEntity.status(HttpStatus.CREATED)
                                        .body(TransactionWebMapper.toResponse(movement)));
    }

    @Override
    public Mono<ResponseEntity<MovementResponseDto>> withdraw(
            String customerId,
            String accountId,
            Mono<AmountRequestDto> amountRequestDto,
            ServerWebExchange exchange) {
        return amountRequestDto
                .flatMap(
                        request ->
                                Mono.from(
                                        registerMovementUseCase
                                                .withdraw(
                                                        customerId,
                                                        accountId,
                                                        request.getAmount(),
                                                        LocalDate.now(clock))
                                                .toFlowable()))
                .doOnNext(
                        movement ->
                                log.info(
                                        "Withdrawal registered accountId={} customerId={}"
                                                + " amount={}",
                                        accountId,
                                        customerId,
                                        movement.amount()))
                .map(
                        movement ->
                                ResponseEntity.status(HttpStatus.CREATED)
                                        .body(TransactionWebMapper.toResponse(movement)));
    }

    @Override
    public Mono<ResponseEntity<TransferResponseDto>> transferToOwnAccount(
            String customerId,
            String accountId,
            Mono<TransferRequestDto> transferRequestDto,
            ServerWebExchange exchange) {
        return transferRequestDto.flatMap(request -> created(transferUseCase.transferToOwnAccount(
                customerId,
                accountId,
                request.getTargetAccountId(),
                request.getAmount(),
                LocalDate.now(clock))));
    }

    @Override
    public Mono<ResponseEntity<TransferResponseDto>> transferToThirdParty(
            String customerId,
            String accountId,
            Mono<TransferRequestDto> transferRequestDto,
            ServerWebExchange exchange) {
        return transferRequestDto.flatMap(request -> created(transferUseCase.transferToThirdParty(
                customerId,
                accountId,
                request.getTargetAccountId(),
                request.getAmount(),
                LocalDate.now(clock))));
    }

    private static Mono<ResponseEntity<TransferResponseDto>> created(Single<Transfer> transfer) {
        return Mono.from(transfer.toFlowable())
                .doOnNext(saved -> log.info(
                        "Transfer registered type={} sourceAccountId={} targetAccountId={}"
                                + " amount={}",
                        saved.type(),
                        saved.outgoing().accountId(),
                        saved.incoming().accountId(),
                        saved.outgoing().amount()))
                .map(saved -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(TransactionWebMapper.toResponse(saved)));
    }

    @Override
    public Mono<ResponseEntity<AccountBalanceResponseDto>> getAccountBalance(
            String customerId,
            String accountId,
            ServerWebExchange exchange) {
        log.debug("Fetching balance accountId={} customerId={}", accountId, customerId);
        return Mono.from(
                        getAvailableBalanceUseCase
                                .getAccountBalance(customerId, accountId)
                                .toFlowable())
                .map(
                        balance ->
                                ResponseEntity.ok(
                                        new AccountBalanceResponseDto(accountId, balance)));
    }

    @Override
    public Mono<ResponseEntity<DailyAverageBalanceResponseDto>> getAccountDailyAverage(
            String customerId,
            String accountId,
            String month,
            ServerWebExchange exchange) {
        log.debug(
                "Fetching daily average accountId={} customerId={} month={}",
                accountId,
                customerId,
                month);
        return Mono.from(
                        getDailyAverageBalanceUseCase
                                .execute(customerId, accountId, YearMonth.parse(month))
                                .toFlowable())
                .map(average -> ResponseEntity.ok(
                        TransactionWebMapper.toResponse(accountId, average)));
    }

    @Override
    public Mono<ResponseEntity<Flux<ProductMovementResponseDto>>> getAccountMovements(
            String customerId,
            String accountId,
            ServerWebExchange exchange) {
        log.debug("Fetching movements accountId={} customerId={}", accountId, customerId);
        return MovementResponses.of(
                getProductMovementsUseCase.getAccountMovements(customerId, accountId));
    }
}
