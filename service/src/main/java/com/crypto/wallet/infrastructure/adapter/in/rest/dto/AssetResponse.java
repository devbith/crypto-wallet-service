package com.crypto.wallet.infrastructure.adapter.in.rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record AssetResponse(

    @JsonProperty("symbol") String symbol,

    @JsonProperty("quantity") BigDecimal quantity,

    @JsonProperty("price") BigDecimal price,

    @JsonProperty("value") BigDecimal value,

    @JsonProperty("updated_at") OffsetDateTime updatedAt) {

}
