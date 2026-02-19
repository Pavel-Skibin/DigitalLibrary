package org.nahap.bookcatalogservice.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Конфигурация для асинхронного выполнения задач
 */
@Configuration
@EnableAsync
public class AsyncConfig {
    // @Async методы будут выполняться асинхронно
}
