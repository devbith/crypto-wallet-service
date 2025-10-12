package com.crypto.wallet.infrastructure.adapter.in.rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record ProfitSimulationRequest(
    @JsonProperty("assets")
    @NotNull(message = "Assets list is required")
    @Valid
    List<AssetSimulationDto> assets) {

}
