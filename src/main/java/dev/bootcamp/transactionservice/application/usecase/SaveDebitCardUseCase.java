package dev.bootcamp.transactionservice.application.usecase;

import dev.bootcamp.transactionservice.domain.model.debitcard.DebitCard;
import io.reactivex.rxjava3.core.Completable;

/**
 * Caso de uso para guardar en la copia local una tarjeta de débito emitida o actualizada en
 * account-service (eventos {@code DebitCardIssued} y {@code DebitCardAccountsUpdated}).
 *
 * <p>Es idempotente: cada evento trae el estado completo de la tarjeta. Es reactivo: no hace nada
 * hasta que se suscribe al {@link Completable}.
 */
public interface SaveDebitCardUseCase {

    /**
     * Guarda o reemplaza la tarjeta.
     */
    Completable execute(DebitCard debitCard);

    /**
     * Quita de la copia local una tarjeta dada de baja ({@code DebitCardClosed}): ya no se puede
     * pagar con ella. Es idempotente.
     */
    Completable close(String debitCardId);
}
