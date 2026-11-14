package dev.bootcamp.transactionservice.application.usecase;

import dev.bootcamp.transactionservice.application.port.DebitCardRepository;
import dev.bootcamp.transactionservice.domain.model.debitcard.DebitCard;
import io.reactivex.rxjava3.core.Completable;
import lombok.RequiredArgsConstructor;

/**
 * Implementación de {@link SaveDebitCardUseCase}.
 */
@RequiredArgsConstructor
public class SaveDebitCardUseCaseImpl implements SaveDebitCardUseCase {
    private final DebitCardRepository debitCardRepository;

    @Override
    public Completable execute(DebitCard debitCard) {
        return debitCardRepository.save(debitCard);
    }

    @Override
    public Completable close(String debitCardId) {
        return debitCardRepository.deleteById(debitCardId);
    }
}
