package dev.bootcamp.transactionservice.domain.model.transfer;

import dev.bootcamp.transactionservice.domain.exception.BusinessRuleException;
import dev.bootcamp.transactionservice.domain.exception.NotFoundException;
import dev.bootcamp.transactionservice.domain.model.account.Account;
import dev.bootcamp.transactionservice.domain.model.account.CheckingMovementPolicy;
import dev.bootcamp.transactionservice.domain.model.movement.Movement;
import dev.bootcamp.transactionservice.domain.model.movement.MovementType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransferTest {

    private static final LocalDate DATE = LocalDate.of(2026, 11, 15);
    private static final BigDecimal AMOUNT = new BigDecimal("50");

    private static Account account(String id, String customerId) {
        return new Account(
                id,
                customerId,
                new CheckingMovementPolicy(),
                BigDecimal.ZERO,
                null,
                null,
                null);
    }

    private final Account ownSource = account("a1", "100");
    private final Account ownTarget = account("a2", "100");
    private final Account thirdPartyTarget = account("b1", "200");

    @Test
    void ownTransferCreatesOutgoingAndIncomingMovements() {
        Transfer transfer =
                Transfer.between(TransferType.OWN, "100", ownSource, ownTarget, AMOUNT, DATE);

        assertThat(transfer.outgoing())
                .isEqualTo(new Movement("a1", MovementType.TRANSFER_OUT, AMOUNT, DATE));
        assertThat(transfer.incoming())
                .isEqualTo(new Movement("a2", MovementType.TRANSFER_IN, AMOUNT, DATE));
    }

    @Test
    void ownTransferRejectsTargetOfAnotherCustomer() {
        assertThatThrownBy(() -> Transfer.between(
                        TransferType.OWN,
                        "100",
                        ownSource,
                        thirdPartyTarget,
                        AMOUNT,
                        DATE))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("La cuenta destino no es del cliente; use la transferencia a terceros");
    }

    @Test
    void thirdPartyTransferGoesToAnotherCustomer() {
        Transfer transfer = Transfer.between(
                TransferType.THIRD_PARTY,
                "100",
                ownSource,
                thirdPartyTarget,
                AMOUNT,
                DATE);

        assertThat(transfer.type()).isEqualTo(TransferType.THIRD_PARTY);
        assertThat(transfer.incoming().accountId()).isEqualTo("b1");
    }

    @Test
    void thirdPartyTransferRejectsOwnTarget() {
        assertThatThrownBy(() -> Transfer.between(
                        TransferType.THIRD_PARTY,
                        "100",
                        ownSource,
                        ownTarget,
                        AMOUNT,
                        DATE))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void rejectsSameAccountAndInvalidAmount() {
        assertThatThrownBy(() -> Transfer.between(
                        TransferType.OWN,
                        "100",
                        ownSource,
                        ownSource,
                        AMOUNT,
                        DATE))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("La cuenta destino debe ser distinta de la de origen");
        assertThatThrownBy(() -> Transfer.between(
                        TransferType.OWN,
                        "100",
                        ownSource,
                        ownTarget,
                        BigDecimal.ZERO,
                        DATE))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void rejectsSourceOfAnotherCustomer() {
        assertThatThrownBy(() -> Transfer.between(
                        TransferType.THIRD_PARTY,
                        "200",
                        ownSource,
                        thirdPartyTarget,
                        AMOUNT,
                        DATE))
                .isInstanceOf(NotFoundException.class);
    }
}
