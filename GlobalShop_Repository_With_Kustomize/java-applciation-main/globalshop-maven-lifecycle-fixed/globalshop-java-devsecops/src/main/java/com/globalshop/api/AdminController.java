package com.globalshop.api;

import com.globalshop.api.Requests.*;
import com.globalshop.api.Responses.*;
import com.globalshop.model.Product;
import com.globalshop.service.*;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

/** For local training only. Replace the shared key with an identity provider and RBAC before production. */
@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final ShopService service;
    private final byte[] key;
    public AdminController(ShopService service, @Value("${app.admin-key}") String key) {
        this.service = service;
        this.key = key.getBytes(StandardCharsets.UTF_8);
    }
    private void requireKey(String supplied) {
        if (supplied == null || !MessageDigest.isEqual(key, supplied.getBytes(StandardCharsets.UTF_8))) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "Invalid admin key");
        }
    }

    @GetMapping("/stats")
    public StatsView stats(@RequestHeader(value = "X-Admin-Key", required = false) String key) {
        requireKey(key);
        return service.stats();
    }
    @GetMapping("/orders")
    public List<OrderView> orders(@RequestHeader(value = "X-Admin-Key", required = false) String key) {
        requireKey(key);
        return service.allOrders();
    }
    @GetMapping("/products")
    public List<Product> products(@RequestHeader(value = "X-Admin-Key", required = false) String key) {
        requireKey(key);
        return service.catalog(null, null);
    }
    @PatchMapping("/products/{id}/stock")
    public Product stock(@RequestHeader(value = "X-Admin-Key", required = false) String key,
                         @PathVariable Long id, @Valid @RequestBody StockRequest req) {
        requireKey(key);
        return service.changeStock(id, req.stock());
    }
    @PatchMapping("/orders/{id}/status")
    public OrderView status(@RequestHeader(value = "X-Admin-Key", required = false) String key,
                            @PathVariable String id, @Valid @RequestBody StatusRequest req) {
        requireKey(key);
        return service.changeStatus(id, req.status());
    }
}
