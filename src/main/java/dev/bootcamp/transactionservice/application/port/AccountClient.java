package dev.bootcamp.transactionservice.application.port;

import dev.bootcamp.transactionservice.domain.model.account.Account;
import io.reactivex.rxjava3.core.Maybe;

/**
 * Consulta de cuentas en account-service.
 */
public interface AccountClient {

    /**
     * Busca el recurso por su id.
     *
     * @return la cuenta, o vacío si no existe
     */
    Maybe<Account> findById(String accountId);
}
