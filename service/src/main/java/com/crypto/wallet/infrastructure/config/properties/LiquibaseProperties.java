package com.crypto.wallet.infrastructure.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "spring.liquibase")
public record LiquibaseProperties(String changeLog, String contexts) {
}