package com.crypto.wallet.application.model.primitives;

import java.util.Objects;

public record Symbol(String value) {
    
    public Symbol {
        Objects.requireNonNull(value, "Symbol cannot be null");
        if (value.trim().isEmpty()) {
            throw new IllegalArgumentException("Symbol cannot be empty");
        }
        if (value.trim().length() > 10) {
            throw new IllegalArgumentException("Symbol cannot be longer than 10 characters");
        }
        value = value.trim().toUpperCase();
        if (!value.matches("^[A-Z0-9]+$")) {
            throw new IllegalArgumentException("Symbol must contain only uppercase letters and numbers");
        }
    }
    
    public static Symbol of(String symbol) {
        return new Symbol(symbol);
    }
    
    @Override
    public String toString() {
        return value;
    }
}
