package com.globalshop.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
public class OrderItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private ShopOrder order;
    @Column(nullable = false)
    private Long productId;
    @Column(nullable = false, length = 32)
    private String sku;
    @Column(nullable = false, length = 120)
    private String productName;
    @Column(nullable = false)
    private int quantity;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal lineTotal;

    protected OrderItem() {}
    public OrderItem(Product product, int quantity) {
        this.productId = product.getId();
        this.sku = product.getSku();
        this.productName = product.getName();
        this.quantity = quantity;
        this.unitPrice = product.getPrice();
        this.lineTotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
    void setOrder(ShopOrder order) { this.order = order; }
    public Long getId() { return id; }
    public Long getProductId() { return productId; }
    public String getSku() { return sku; }
    public String getProductName() { return productName; }
    public int getQuantity() { return quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public BigDecimal getLineTotal() { return lineTotal; }
}
