package com.globalshop.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

public final class Requests {
    private Requests() {}

    public record LineRequest(@NotNull @Positive Long productId,
                              @Min(1) @Max(20) int quantity) {}

    public record CheckoutRequest(
            @NotBlank @Size(max = 100) String customerName,
            @NotBlank @Email @Size(max = 254) String customerEmail,
            @NotBlank @Size(min = 10, max = 350) String shippingAddress,
            @NotEmpty @Size(max = 30) List<@Valid LineRequest> items) {}

    public record StockRequest(@Min(0) @Max(100000) int stock) {}

    public record StatusRequest(@NotNull com.globalshop.model.OrderStatus status) {}
}
