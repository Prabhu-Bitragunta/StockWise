package com.vcube.stockwise.api;
import jakarta.validation.constraints.*;
public record TransactionRequest(@NotNull @Positive Long productId,@NotBlank String type,int quantity,@Size(max=500) String referenceNote) {}
