package dev.bootcamp.transactionservice.application.usecase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.bootcamp.transactionservice.application.port.AccountClient;
import dev.bootcamp.transactionservice.application.port.MovementRepository;
import dev.bootcamp.transactionservice.domain.exception.BusinessRuleException;
import dev.bootcamp.transactionservice.domain.model.account.Account;
import dev.bootcamp.transactionservice.domain.model.account.AccountType;
import dev.bootcamp.transactionservice.domain.model.movement.Movement;
import dev.bootcamp.transactionservice.domain.model.movement.MovementType;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RegisterMovementUseCaseImplTest {

    private static final LocalDate DATE = LocalDate.of(2026, 11, 15);

    @Mock
    private MovementRepository movementRepository;

    @Mock
    private AccountClient accountClient;

    @InjectMocks
    private RegisterMovementUseCaseImpl useCase;

    @Test
    void rejectsUnknownAccount() {
        when(accountClient.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(99L, MovementType.DEPOSIT, DATE))
                .isInstanceOf(BusinessRuleException.class);
        verify(movementRepository, never()).save(any());
    }

    @Test
    void savesMovementWithinSavingsLimit() {
        when(accountClient.findById(1L)).thenReturn(Optional.of(new Account(1L, AccountType.SAVINGS, 2, null)));
        when(movementRepository.countByAccountIdAndMonth(1L, YearMonth.from(DATE))).thenReturn(1L);

        useCase.execute(1L, MovementType.DEPOSIT, DATE);

        verify(movementRepository).save(new Movement(1L, MovementType.DEPOSIT, DATE));
    }

    @Test
    void doesNotSaveMovementOverSavingsLimit() {
        when(accountClient.findById(1L)).thenReturn(Optional.of(new Account(1L, AccountType.SAVINGS, 2, null)));
        when(movementRepository.countByAccountIdAndMonth(1L, YearMonth.from(DATE))).thenReturn(2L);

        assertThatThrownBy(() -> useCase.execute(1L, MovementType.DEPOSIT, DATE))
                .isInstanceOf(BusinessRuleException.class);
        verify(movementRepository, never()).save(any());
    }

    @Test
    void doesNotSaveSecondFixedTermMovementInMonth() {
        when(accountClient.findById(1L)).thenReturn(Optional.of(new Account(1L, AccountType.FIXED_TERM, null, 15)));
        when(movementRepository.countByAccountIdAndMonth(1L, YearMonth.from(DATE))).thenReturn(1L);

        assertThatThrownBy(() -> useCase.execute(1L, MovementType.WITHDRAWAL, DATE))
                .isInstanceOf(BusinessRuleException.class);
        verify(movementRepository, never()).save(any());
    }
}
