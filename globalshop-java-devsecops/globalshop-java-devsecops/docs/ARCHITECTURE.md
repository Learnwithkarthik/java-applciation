# GlobalShop architecture, APIs and business rules

## Components

- `static/index.html`, `static/js/shop.js`, `static/css/style.css`: the client app (search, filter, bag, checkout, order status).
- `static/admin.html`, `static/js/admin.js`: admin operations dashboard; training-only API key prompt.
- `ShopController`: public catalog, checkout, order tracking.
- `AdminController`: API-key-gated operational reporting and inventory/order changes.
- `ShopService`: transactions, stock locking, order state machine, reporting.
- `ProductRepository`, `OrderRepository`: Spring Data JPA.
- `Product`, `ShopOrder`, `OrderItem`: relational entities.
- `DemoData`: 12 initial products inserted only when there are zero products.
- `ApiExceptionHandler`: consistent errors and HTTP statuses.

### Database (conceptual)

```text
products
  id PK, sku UNIQUE, name, description, category, price, stock, active, featured

shop_orders
  id PK, order_number UNIQUE, customer_name, customer_email,
  shipping_address, status, total, created_at

order_items
  id PK, order_id FK -> shop_orders.id,
  product_id, sku, product_name, unit_price, quantity, line_total
```

Order item price/name/SKU are captured when an order is placed, so later product changes do not rewrite the customer's historical order. The current demo has no separate user accounts or payment provider.

## Endpoints

| Method | Path | Security | Purpose |
|---|---|---|---|
| GET | `/` | public | Storefront |
| GET | `/admin.html` | public page only | Admin UI; API calls still require key |
| GET | `/api/products` | public | List active products; optional `category`, `search` |
| GET | `/api/products/{id}` | public | Product details |
| POST | `/api/orders` | public demo | Create order, decrease inventory |
| GET | `/api/orders/track?number=...&email=...` | number + email | Track order |
| GET | `/api/admin/stats` | `X-Admin-Key` | Revenue/order/product summary |
| GET | `/api/admin/orders` | `X-Admin-Key` | Recent orders |
| GET | `/api/admin/products` | `X-Admin-Key` | Stock management product list |
| PATCH | `/api/admin/products/{id}/stock` | `X-Admin-Key` | Change inventory |
| PATCH | `/api/admin/orders/{orderNumber}/status` | `X-Admin-Key` | Move order to next state |
| GET | `/actuator/health` | public | Container/orchestration health probe |

## Demo checkout HTTP examples

Create an order (adjust product ID using GET `/api/products`):

```bash
curl -X POST http://localhost:8080/api/orders \
  -H 'Content-Type: application/json' \
  -d '{"customerName":"Demo Student", "customerEmail":"demo@example.com", "shippingAddress":"123 Demo Street, Bengaluru 560001", "items":[{"productId":1,"quantity":2}]}'
```

Expected HTTP status is **201**, with `orderNumber`, status `PLACED`, total, and item details in the JSON response. Track with the returned number and the exact email:

```bash
curl 'http://localhost:8080/api/orders/track?number=GS-REPLACE&email=demo@example.com'
```

Admin (default training key shown below; replace it in non-local setups):

```bash
curl -H 'X-Admin-Key: dev-only-change-me' http://localhost:8080/api/admin/stats
curl -X PATCH http://localhost:8080/api/admin/products/1/stock \
  -H 'X-Admin-Key: dev-only-change-me' -H 'Content-Type: application/json' \
  -d '{"stock":30}'

curl -X PATCH http://localhost:8080/api/admin/orders/GS-REPLACE/status \
  -H 'X-Admin-Key: dev-only-change-me' -H 'Content-Type: application/json' \
  -d '{"status":"PACKING"}'
```

## Order state machine

```text
               +--- CANCELLED (restock products)
               |
PLACED ------> PACKING ------> SHIPPED ------> DELIVERED
                     |
                     +-------> CANCELLED (restock products)
```

- Orders in `DELIVERED` / `CANCELLED` are terminal; a second status update is rejected with 409.
- Cancelling `PLACED` or `PACKING` returns stock to the catalog.
- Cancelling after shipment is blocked in this training app to avoid fake inventory restoration after dispatch.
- Checkout applies pessimistic DB locks on products in ascending ID order to minimize oversell/deadlock risk.
- If any line has insufficient stock, the complete transaction rolls back.
- Checkout accepts up to 30 distinct input lines and 20 units per product; duplicate entries are normalized.
- Price is server-computed, never trusted from the browser.

## What makes this more than a Hello World app

A database-backed purchase affects stock, orders and reporting. The customer UI and admin UI see those same changes through REST APIs. The system implements basic validation, transaction boundaries, business workflows and end-to-end integration tests. You can safely practice real DevSecOps concepts on it, then extend it for services/queues/auth and Kubernetes later.

## Kubernetes phase (not implemented yet)

When ready, introduce a proper secrets manager and deploy the application with:

1. Container registry and image tag promotion from Nexus/CI.
2. Spring Boot `Deployment`, `Service`, probes based on `/actuator/health/liveness` and `/actuator/health/readiness`.
3. ConfigMap (non-sensitive configuration) and Secret / external secret provider (DB password/admin secret).
4. PostgreSQL as managed database or StatefulSet with persistent storage for demo use.
5. Resource requests/limits, rollout strategies, HPA, and an Ingress controller or cloud LoadBalancer.
6. Replace demo admin key with identity provider authorization and persistent schema migration.
