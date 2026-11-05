package dev.bootcamp.transactionservice.application.port;

import dev.bootcamp.transactionservice.domain.model.account.Account;
import java.util.Optional;

/** Consulta de cuentas en account-service. */
public interface AccountClient {
    Optional<Account> findById(Long accountId);
}
