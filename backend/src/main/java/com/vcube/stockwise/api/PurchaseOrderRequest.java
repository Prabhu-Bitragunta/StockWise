package com.vcube.stockwise.api;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
public record PurchaseOrderRequest(@NotNull @Positive Long supplierId,@NotEmpty List<@Valid Item> items,LocalDate expectedDeliveryDate){
 public record Item(@NotNull @Positive Long productId,@Positive int quantity,@DecimalMin("0.0") BigDecimal unitCost){}
}
