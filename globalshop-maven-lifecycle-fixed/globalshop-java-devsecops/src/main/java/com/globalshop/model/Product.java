package com.globalshop.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "products")
public class Product {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 32)
    private String sku;
    @Column(nullable = false, length = 120)
    private String name;
    @Column(nullable = false, length = 500)
    private String description;
    @Column(nullable = false, length = 40)
    private String category;
    @Column(nullable = false, length = 30)
    private String icon;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;
    @Column(nullable = false)
    private int stock;
    @Column(nullable = false)
    private boolean featured;
    @Column(nullable = false)
    private boolean active = true;

    protected Product() {}

    public Product(String sku, String name, String description, String category,
                   String icon, BigDecimal price, int stock, boolean featured) {
        this.sku = sku;
        this.name = name;
        this.description = description;
        this.category = category;
        this.icon = icon;
        this.price = price;
        this.stock = stock;
        this.featured = featured;
    }

    public Long getId() { return id; }
    public String getSku() { return sku; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getCategory() { return category; }
    public String getIcon() { return icon; }
    public BigDecimal getPrice() { return price; }
    public int getStock() { return stock; }
    public boolean isFeatured() { return featured; }
    public boolean isActive() { return active; }
    public void setStock(int stock) { this.stock = stock; }
}
