package com.globalshop.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "shop_orders")
public class ShopOrder {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 48)
    private String orderNumber;
    @Column(nullable = false, length = 100)
    private String customerName;
    @Column(nullable = false, length = 254)
    private String customerEmail;
    @Column(nullable = false, length = 350)
    private String shippingAddress;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status = OrderStatus.PLACED;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;
    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    protected ShopOrder() {}

    public ShopOrder(String orderNumber, String customerName, String customerEmail, String shippingAddress) {
        this.orderNumber = orderNumber;
        this.customerName = customerName;
        this.customerEmail = customerEmail;
        this.shippingAddress = shippingAddress;
    }

    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
        total = total.add(item.getLineTotal());
    }

    public Long getId() { return id; }
    public String getOrderNumber() { return orderNumber; }
    public String getCustomerName() { return customerName; }
    public String getCustomerEmail() { return customerEmail; }
    public String getShippingAddress() { return shippingAddress; }
    public OrderStatus getStatus() { return status; }
    public BigDecimal getTotal() { return total; }
    public Instant getCreatedAt() { return createdAt; }
    public List<OrderItem> getItems() { return items; }
    public void setStatus(OrderStatus status) { this.status = status; }
}
