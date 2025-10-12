package com.crypto.wallet.infrastructure.adapter.in.rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public record ProfitSimulationResponse(
    
    @JsonProperty("total")
    BigDecimal total,
    
    @JsonProperty("best_asset")
    String bestAsset,
    
    @JsonProperty("best_performance")
    BigDecimal bestPerformance,
    
    @JsonProperty("worst_asset")
    String worstAsset,
    
    @JsonProperty("worst_performance")
    BigDecimal worstPerformance
    
) {}