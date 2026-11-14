package dev.bootcamp.transactionservice.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Habilita las tareas programadas (cobro mensual de comisiones).
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
