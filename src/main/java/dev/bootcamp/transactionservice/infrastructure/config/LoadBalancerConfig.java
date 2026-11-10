package dev.bootcamp.transactionservice.infrastructure.config;

import org.springframework.boot.webclient.WebClientCustomizer;
import org.springframework.cloud.client.loadbalancer.reactive.ReactorLoadBalancerExchangeFilterFunction;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Resuelve por Eureka las llamadas a otros microservicios.
 *
 * <p>Las URLs base usan el nombre con el que cada servicio se registra (por ejemplo
 * {@code http://customer-service}); el filtro de balanceo de carga lo reemplaza por la dirección
 * de una de sus instancias registradas.
 */
@Configuration
public class LoadBalancerConfig {

    /**
     * Agrega el balanceo de carga por Eureka a los WebClient creados con el builder de Spring.
     *
     * @param loadBalancerFilter filtro que elige la instancia registrada del servicio
     * @return el customizador del builder
     */
    @Bean
    public WebClientCustomizer loadBalancedWebClientCustomizer(
            ReactorLoadBalancerExchangeFilterFunction loadBalancerFilter) {
        return builder -> builder.filter(loadBalancerFilter);
    }
}
