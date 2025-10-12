package com.crypto.wallet.application.model.primitives;

import java.util.Objects;
import java.util.regex.Pattern;

public record Email(String value) {
    
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
    );
    
    public Email {
        Objects.requireNonNull(value, "Email cannot be null");
        if (!EMAIL_PATTERN.matcher(value.trim()).matches()) {
            throw new IllegalArgumentException("Invalid email format: " + value);
        }
        value = value.trim().toLowerCase();
    }
    
    public static Email of(String email) {
        return new Email(email);
    }
    
    @Override
    public String toString() {
        return value;
    }
}
