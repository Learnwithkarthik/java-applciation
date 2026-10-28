package com.globalshop.service;

import com.globalshop.api.Requests.*;
import com.globalshop.api.Responses.*;
import com.globalshop.model.*;
import com.globalshop.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.*;

@Service
public class ShopService {
    private static final Logger log = LoggerFactory.getLogger(ShopService.class);
    private final ProductRepository products;
    private final OrderRepository orders;
    public ShopService(ProductRepository products, OrderRepository orders) {
        this.products = products;
        this.orders = orders;
    }

    @Transactional(readOnly = true)
    public List<Product> catalog(String category, String search) {
        String c = category == null ? "" : category.trim();
        String s = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
        return products.findByActiveTrueOrderByIdAsc().stream()
            .filter(p -> c.isBlank() || p.getCategory().equalsIgnoreCase(c))
            .filter(p -> s.isBlank() || p.getName().toLowerCase(Locale.ROOT).contains(s)
                || p.getDescription().toLowerCase(Locale.ROOT).contains(s)
                || p.getSku().toLowerCase(Locale.ROOT).contains(s))
            .toList();
    }

    @Transactional(readOnly = true)
    public Product getProduct(Long id) {
        Product product = products.findById(id).orElseThrow(() ->
            new BusinessException(HttpStatus.NOT_FOUND, "Product not found"));
        if (!product.isActive()) throw new BusinessException(HttpStatus.NOT_FOUND, "Product not found");
        return product;
    }

    @Transactional
    public OrderView checkout(CheckoutRequest request) {
        // Normalize duplicate product lines; lock products in ID order to avoid overselling/deadlocks.
        Map<Long, Integer> quantityById = new TreeMap<>();
        for (LineRequest line : request.items()) {
            quantityById.merge(line.productId(), line.quantity(), Integer::sum);
        }
        if (quantityById.values().stream().anyMatch(qty -> qty > 20)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Maximum 20 units per product per order");
        }
        ShopOrder order = new ShopOrder("GS-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT),
                request.customerName().trim(), request.customerEmail().trim().toLowerCase(Locale.ROOT),
                request.shippingAddress().trim());
        for (var line : quantityById.entrySet()) {
            Product product = products.findActiveByIdForUpdate(line.getKey()).orElseThrow(() ->
                new BusinessException(HttpStatus.NOT_FOUND, "A selected product is unavailable"));
            if (product.getStock() < line.getValue()) {
                throw new BusinessException(HttpStatus.CONFLICT,
                        "Not enough stock for " + product.getName() + ". Available: " + product.getStock());
            }
            product.setStock(product.getStock() - line.getValue());
            order.addItem(new OrderItem(product, line.getValue()));
        }
        ShopOrder saved = orders.save(order);
        log.info("Order placed: orderNumber={} itemCount={}", saved.getOrderNumber(), saved.getItems().size());
        return OrderView.of(saved);
    }

    @Transactional(readOnly = true)
    public OrderView track(String number, String email) {
        return orders.findByOrderNumberIgnoreCaseAndCustomerEmailIgnoreCase(number.trim(), email.trim())
                .map(OrderView::of)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "Order not found. Check the order number and email."));
    }

    @Transactional(readOnly = true)
    public List<OrderView> allOrders() {
        return orders.findAllByOrderByCreatedAtDesc().stream().map(OrderView::of).toList();
    }

    @Transactional(readOnly = true)
    public StatsView stats() {
        List<ShopOrder> all = orders.findAll();
        return new StatsView(products.count(), all.size(),
            products.findByActiveTrueOrderByIdAsc().stream().filter(p -> p.getStock() < 8).count(),
            all.stream().filter(o -> o.getStatus() != OrderStatus.CANCELLED)
                .map(ShopOrder::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add),
            all.stream().filter(o -> o.getStatus() == OrderStatus.PLACED || o.getStatus() == OrderStatus.PACKING).count());
    }

    @Transactional
    public Product changeStock(Long id, int stock) {
        Product p = products.findActiveByIdForUpdate(id).orElseThrow(() ->
                new BusinessException(HttpStatus.NOT_FOUND, "Product not found"));
        p.setStock(stock);
        return p;
    }

    @Transactional
    public OrderView changeStatus(String number, OrderStatus newStatus) {
        ShopOrder order = orders.findByOrderNumberIgnoreCase(number).orElseThrow(() ->
                new BusinessException(HttpStatus.NOT_FOUND, "Order not found"));
        OrderStatus old = order.getStatus();
        if (old == OrderStatus.DELIVERED || old == OrderStatus.CANCELLED) {
            throw new BusinessException(HttpStatus.CONFLICT, "Finalized orders cannot change status");
        }
        boolean allowed = switch (old) {
            case PLACED -> newStatus == OrderStatus.PACKING || newStatus == OrderStatus.CANCELLED;
            case PACKING -> newStatus == OrderStatus.SHIPPED || newStatus == OrderStatus.CANCELLED;
            case SHIPPED -> newStatus == OrderStatus.DELIVERED;
            default -> false;
        };
        if (!allowed) throw new BusinessException(HttpStatus.CONFLICT, "Invalid order status transition");
        order.setStatus(newStatus);
        // Restore inventory for orders cancelled before dispatch.
        if (newStatus == OrderStatus.CANCELLED) {
            List<OrderItem> items = order.getItems().stream()
                    .sorted(Comparator.comparing(OrderItem::getProductId)).toList();
            for (OrderItem item : items) {
                Product p = products.findActiveByIdForUpdate(item.getProductId()).orElseThrow(() ->
                    new BusinessException(HttpStatus.CONFLICT, "Cannot restore stock: missing product"));
                p.setStock(p.getStock() + item.getQuantity());
            }
        }
        log.info("Order status changed: number={} status={}", order.getOrderNumber(), newStatus);
        return OrderView.of(order);
    }
}
