package dev.bootcamp.transactionservice.infrastructure.persistence;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

/**
 * Repositorio de Spring Data para {@link YankiOperationDocument}.
 */
public interface YankiOperationMongoRepository
        extends ReactiveMongoRepository<YankiOperationDocument, String> {}
