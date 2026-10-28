package com.globalshop.api;

import com.globalshop.model.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class Responses {
    private Responses() {}

    public record ItemView(Long productId, String sku, String name, int quantity,
                           BigDecimal unitPrice, BigDecimal lineTotal) {
        public static ItemView of(OrderItem item) {
            return new ItemView(item.getProductId(), item.getSku(), item.getProductName(),
                    item.getQuantity(), item.getUnitPrice(), item.getLineTotal());
        }
    }

    public record OrderView(String orderNumber, String customerName, String customerEmail,
                            String shippingAddress, String status, BigDecimal total,
                            Instant createdAt, List<ItemView> items) {
        public static OrderView of(ShopOrder order) {
            return new OrderView(order.getOrderNumber(), order.getCustomerName(),
                    order.getCustomerEmail(), order.getShippingAddress(),
                    order.getStatus().name(), order.getTotal(), order.getCreatedAt(),
                    order.getItems().stream().map(ItemView::of).toList());
        }
    }

    public record StatsView(long products, long orders, long lowStock,
                            BigDecimal revenue, long pendingOrders) {}
}
