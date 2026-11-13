package dev.bootcamp.transactionservice.application.usecase;

import dev.bootcamp.transactionservice.application.port.DebitCardRepository;
import dev.bootcamp.transactionservice.domain.model.debitcard.DebitCard;
import io.reactivex.rxjava3.core.Completable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SaveDebitCardUseCaseImplTest {

    @Mock private DebitCardRepository debitCardRepository;

    @InjectMocks private SaveDebitCardUseCaseImpl useCase;

    @Test
    void savesTheCard() {
        DebitCard card = new DebitCard("d1", "c1", "a1");
        when(debitCardRepository.save(card)).thenReturn(Completable.complete());

        useCase.execute(card).test().assertComplete();

        verify(debitCardRepository).save(card);
    }
}
