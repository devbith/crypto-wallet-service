package com.crypto.wallet.application.model.primitives;

import java.util.Objects;
import java.util.UUID;

public record WalletId(String value) {
    
    public WalletId {
        Objects.requireNonNull(value, "WalletId cannot be null");
        if (value.trim().isEmpty()) {
            throw new IllegalArgumentException("WalletId cannot be empty");
        }
        try {
            UUID.fromString(value.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("WalletId must be a valid UUID format");
        }
        value = value.trim();
    }
    
    public static WalletId of(String walletId) {
        return new WalletId(walletId);
    }
    
    public static WalletId generate() {
        return new WalletId(UUID.randomUUID().toString());
    }
    
    public UUID toUUID() {
        return UUID.fromString(value);
    }
    
    @Override
    public String toString() {
        return value;
    }
}
