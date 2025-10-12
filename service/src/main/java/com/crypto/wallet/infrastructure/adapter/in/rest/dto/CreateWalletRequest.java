package com.crypto.wallet.infrastructure.adapter.in.rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateWalletRequest(@JsonProperty("email")
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    String email) {

}