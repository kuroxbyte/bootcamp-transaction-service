package dev.bootcamp.transactionservice.domain.model.movement;

import java.time.LocalDate;

public record Movement(Long accountId, MovementType type, LocalDate date) {
}
