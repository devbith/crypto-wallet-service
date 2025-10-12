package com.crypto.wallet.infrastructure.adapter.in.rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record WalletResponse(
    
    @JsonProperty("id")
    String id,
    
    @JsonProperty("total")
    BigDecimal total,
    
    @JsonProperty("assets")
    List<AssetResponse> assets,
    
    @JsonProperty("created_at")
    OffsetDateTime createdAt
    
) {}