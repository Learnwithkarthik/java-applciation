package com.globalshop.api;

import com.globalshop.api.Requests.CheckoutRequest;
import com.globalshop.api.Responses.OrderView;
import com.globalshop.model.Product;
import com.globalshop.service.ShopService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api")
public class ShopController {
    private final ShopService service;
    public ShopController(ShopService service) { this.service = service; }

    @GetMapping("/products")
    public List<Product> products(@RequestParam(required = false) String category,
                                  @RequestParam(required = false) String search) {
        return service.catalog(category, search);
    }
    @GetMapping("/products/{id}")
    public Product product(@PathVariable Long id) { return service.getProduct(id); }

    @PostMapping("/orders")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderView checkout(@Valid @RequestBody CheckoutRequest checkout) {
        return service.checkout(checkout);
    }

    @GetMapping("/orders/track")
    public OrderView track(@RequestParam String number,
                           @RequestParam String email) {
        if (number.isBlank() || email.isBlank() || !email.contains("@")) {
            throw new com.globalshop.service.BusinessException(HttpStatus.BAD_REQUEST, "Valid order number and email required");
        }
        return service.track(number, email);
    }
}
