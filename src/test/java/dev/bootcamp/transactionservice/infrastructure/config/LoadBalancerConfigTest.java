package dev.bootcamp.transactionservice.infrastructure.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.client.loadbalancer.reactive.ReactorLoadBalancerExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifica que los WebClient del servicio resuelvan los demás servicios por Eureka.
 */
@SpringBootTest
class LoadBalancerConfigTest {

    @Autowired
    private WebClient.Builder webClientBuilder;

    @Test
    void webClientBuilderUsesLoadBalancer() {
        webClientBuilder.filters(filters -> assertThat(filters)
                .anySatisfy(filter -> assertThat(filter)
                        .isInstanceOf(ReactorLoadBalancerExchangeFilterFunction.class)));
    }
}
