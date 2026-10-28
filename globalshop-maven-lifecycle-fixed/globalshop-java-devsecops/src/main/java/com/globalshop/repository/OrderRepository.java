package com.globalshop.repository;

import com.globalshop.model.ShopOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<ShopOrder, Long> {
    Optional<ShopOrder> findByOrderNumberIgnoreCase(String orderNumber);
    Optional<ShopOrder> findByOrderNumberIgnoreCaseAndCustomerEmailIgnoreCase(String orderNumber, String customerEmail);
    List<ShopOrder> findAllByOrderByCreatedAtDesc();
}
