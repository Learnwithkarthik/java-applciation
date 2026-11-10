package com.globalshop.config;

import com.globalshop.model.Product;
import com.globalshop.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.math.BigDecimal;
import java.util.List;

@Configuration
public class DemoData {
    private static Product p(String sku, String name, String desc, String category,
                             String icon, String price, int stock, boolean featured) {
        return new Product(sku, name, desc, category, icon, new BigDecimal(price), stock, featured);
    }
    @Bean
    CommandLineRunner seed(ProductRepository repo, @Value("${app.seed-demo-data:true}") boolean enabled) {
        return args -> {
            if (!enabled || repo.count() > 0) return;
            repo.saveAll(List.of(
                p("GS-AUD-001", "Studio Wireless Headphones", "40-hour battery · active noise cancellation · soft comfort fit", "Audio", "🎧", "6499.00", 24, true),
                p("GS-CMP-002", "Orbit Mechanical Keyboard", "Hot-swappable switches · tactile typing · RGB backlight", "Workspace", "⌨️", "4999.00", 18, true),
                p("GS-WEA-003", "Pulse Smartwatch Pro", "Health metrics · GPS tracking · 7-day battery", "Wearables", "⌚", "8999.00", 7, true),
                p("GS-HOM-004", "Aura Smart Ambient Lamp", "16 million colors · voice control · warm glow", "Home", "💡", "2799.00", 32, false),
                p("GS-CMP-005", "Edge 4K USB-C Monitor", "27-inch IPS display · USB-C power delivery · crisp 4K", "Workspace", "🖥️", "28999.00", 6, true),
                p("GS-AUD-006", "Mini Portable Speaker", "360° audio · IPX7 waterproof · compact design", "Audio", "🔊", "3499.00", 16, false),
                p("GS-ACC-007", "Nomad Travel Backpack", "Recycled fabric · 16-inch laptop sleeve · weather resistant", "Lifestyle", "🎒", "4299.00", 23, false),
                p("GS-ACC-008", "Flow Insulated Bottle", "Vacuum insulated · 24-hour cold · leakproof lid", "Lifestyle", "🥤", "1299.00", 45, false),
                p("GS-WEA-009", "Fitness Band Air", "Sleep insights · heart rate · lightweight comfort", "Wearables", "📟", "2999.00", 13, false),
                p("GS-HOM-010", "Brew Smart Coffee Maker", "Programmable brewing · keep warm · compact design", "Home", "☕", "6599.00", 9, true),
                p("GS-CMP-011", "Swift Ergonomic Mouse", "Silent clicks · multi-device wireless · precision sensor", "Workspace", "🖱️", "1899.00", 37, false),
                p("GS-AUD-012", "Airbuds Lite", "Clear calls · quick pairing · pocket charging case", "Audio", "🎵", "2399.00", 28, false)
            ));
        };
    }
}
