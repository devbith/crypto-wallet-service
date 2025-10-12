package com.crypto.wallet.infrastructure.adapter.in.rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;

public record AddAssetRequest(

    @JsonProperty("symbol")
    @NotBlank(message = "Symbol is required")
    @Pattern(regexp = "^[A-Z0-9]{1,10}$", message = "Symbol must be 1-10 uppercase alphanumeric characters")
    String symbol,

    @JsonProperty("quantity")
    @NotNull(message = "Quantity is required")
    BigDecimal quantity,

    @JsonProperty("price")
    BigDecimal price) {

}
