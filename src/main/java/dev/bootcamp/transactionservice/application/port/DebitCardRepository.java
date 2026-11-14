package dev.bootcamp.transactionservice.application.port;

import dev.bootcamp.transactionservice.domain.model.debitcard.DebitCard;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;

/**
 * Copia local de las tarjetas de débito (datos maestros), con caché.
 */
public interface DebitCardRepository {

    /**
     * Guarda o reemplaza la tarjeta.
     */
    Completable save(DebitCard debitCard);

    /**
     * Busca la tarjeta por su id.
     *
     * @return la tarjeta, o vacío si transaction-service todavía no la conoce
     */
    Maybe<DebitCard> findById(String debitCardId);

    /**
     * Quita la tarjeta de la copia local (y de la caché); no falla si no existe.
     */
    Completable deleteById(String debitCardId);
}
