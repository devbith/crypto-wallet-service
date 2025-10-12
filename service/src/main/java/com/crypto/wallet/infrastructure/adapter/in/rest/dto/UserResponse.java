package com.crypto.wallet.infrastructure.adapter.in.rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;

public record UserResponse(
    
    @JsonProperty("email")
    String email,
    
    @JsonProperty("wallet_id")
    String walletId,
    
    @JsonProperty("created_at")
    OffsetDateTime createdAt
    
) {}