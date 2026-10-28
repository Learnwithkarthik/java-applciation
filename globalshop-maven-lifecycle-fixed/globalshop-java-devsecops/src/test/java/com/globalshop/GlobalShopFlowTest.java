package com.globalshop;

import com.globalshop.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class GlobalShopFlowTest {
    @Autowired MockMvc mvc;
    @Autowired ProductRepository products;

    @Test
    void storefrontListsRealSeededProducts() throws Exception {
        mvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(12)))
                .andExpect(jsonPath("$[0].sku", is("GS-AUD-001")));
    }

    @Test
    void checkoutReducesStockAndReturnsTrackingDetails() throws Exception {
        long id = products.findByActiveTrueOrderByIdAsc().getFirst().getId();
        int original = products.findById(id).orElseThrow().getStock();
        String body = """
                {"customerName":"Test Buyer","customerEmail":"buyer@example.com",
                 "shippingAddress":"123 Example Street, Bengaluru 560001",
                 "items":[{"productId":%d,"quantity":2}]}
                """.formatted(id);
        var result = mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderNumber", startsWith("GS-")))
                .andExpect(jsonPath("$.status", is("PLACED")))
                .andReturn();
        String json = result.getResponse().getContentAsString();
        String number = json.split("\"orderNumber\":\"")[1].split("\"")[0];
        mvc.perform(get("/api/orders/track").param("number", number).param("email", "buyer@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerName", is("Test Buyer")));
        mvc.perform(get("/api/orders/track").param("number", number).param("email", "wrong@example.com"))
                .andExpect(status().isNotFound());
        org.junit.jupiter.api.Assertions.assertEquals(original - 2,
                products.findById(id).orElseThrow().getStock());
    }

    @Test
    void invalidCheckoutAndUnprotectedAdminAccessAreRejected() throws Exception {
        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"customerName":"X","customerEmail":"wrong-email",
                     "shippingAddress":"123 Example Street", "items":[]}
                    """))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/admin/orders"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/stats").header("X-Admin-Key", "test-admin-secret"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.products", is(12)));
    }

    @Test
    void insufficientInventoryIsRejected() throws Exception {
        long id = products.findByActiveTrueOrderByIdAsc().stream()
                .filter(p -> p.getStock() < 8).findFirst().orElseThrow().getId();
        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"customerName":"Test Buyer","customerEmail":"buyer@example.com",
                     "shippingAddress":"123 Example Street, Bengaluru",
                     "items":[{"productId":%d,"quantity":20}]}
                    """.formatted(id)))
                .andExpect(status().isConflict());
    }
}
