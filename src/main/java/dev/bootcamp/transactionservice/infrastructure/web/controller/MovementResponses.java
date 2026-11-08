package dev.bootcamp.transactionservice.infrastructure.web.controller;

import dev.bootcamp.transactionservice.domain.model.history.ProductMovement;
import dev.bootcamp.transactionservice.infrastructure.web.dto.ProductMovementResponseDto;
import dev.bootcamp.transactionservice.infrastructure.web.mapper.TransactionWebMapper;
import io.reactivex.rxjava3.core.Flowable;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Arma la respuesta de las consultas de movimientos de un producto. */
final class MovementResponses {

    private MovementResponses() {}

    /**
     * Convierte el historial en la respuesta HTTP. Si el producto no existe o no pertenece al
     * cliente, el error se propaga antes de responder, para que el manejador global devuelva 404 y
     * no un 200 vacío.
     */
    static Mono<ResponseEntity<Flux<ProductMovementResponseDto>>> of(
            Flowable<ProductMovement> movements) {
        return Flux.from(movements)
                .map(TransactionWebMapper::toResponse)
                .collectList()
                .map(list -> ResponseEntity.ok(Flux.fromIterable(list)));
    }
}
