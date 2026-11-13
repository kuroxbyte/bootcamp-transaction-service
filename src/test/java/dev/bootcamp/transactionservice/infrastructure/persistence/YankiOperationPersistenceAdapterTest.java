package dev.bootcamp.transactionservice.infrastructure.persistence;

import dev.bootcamp.transactionservice.domain.model.yanki.YankiOperation;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mongodb.MongoDBContainer;

import java.util.concurrent.TimeUnit;

@Testcontainers
@DataMongoTest
@Import(YankiOperationPersistenceAdapter.class)
class YankiOperationPersistenceAdapterTest {

    @Container @ServiceConnection static MongoDBContainer mongo = new MongoDBContainer("mongo:7.0");

    @Autowired private YankiOperationPersistenceAdapter adapter;

    @Test
    void savesAndFindsTheResultByOperationId() {
        YankiOperation rejected = YankiOperation.rejected("payment:p1:debit", "Saldo insuficiente");
        adapter.save(rejected).blockingAwait();
        adapter.save(YankiOperation.completed("link:l1:transfer")).blockingAwait();

        adapter.findById("payment:p1:debit").test().awaitDone(5, TimeUnit.SECONDS)
                .assertValue(rejected);
        adapter.findById("link:l1:transfer").test().awaitDone(5, TimeUnit.SECONDS)
                .assertValue(YankiOperation.completed("link:l1:transfer"));
        adapter.findById("payment:p9:debit").test().awaitDone(5, TimeUnit.SECONDS)
                .assertNoValues();
    }
}
