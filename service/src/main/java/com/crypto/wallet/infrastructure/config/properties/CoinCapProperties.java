package com.crypto.wallet.infrastructure.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "crypto-wallet.coincap.api")
public record CoinCapProperties(String baseUrl, String key) {
}