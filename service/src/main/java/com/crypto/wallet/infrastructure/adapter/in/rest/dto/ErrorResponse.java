package com.crypto.wallet.infrastructure.adapter.in.rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;
import java.util.List;

public record ErrorResponse(
    
    @JsonProperty("status")
    int status,
    
    @JsonProperty("message")
    String message,
    
    @JsonProperty("details")
    List<String> details,
    
    @JsonProperty("path")
    String path,
    
    @JsonProperty("timestamp")
    OffsetDateTime timestamp
    
) {
    
    public static ErrorResponse of(int status, String message, String path) {
        return new ErrorResponse(status, message, List.of(), path, OffsetDateTime.now());
    }
    
    public static ErrorResponse of(int status, String message, List<String> details, String path) {
        return new ErrorResponse(status, message, details, path, OffsetDateTime.now());
    }
}