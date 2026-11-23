# GlobalShop: Kong API Gateway lab (DB-less Kong, Kubernetes)

## Grounded against uploaded source

- Spring Boot app: `globalshop-maven-lifecycle-fixed/globalshop-java-devsecops`
- `ShopController.java`: `GET /api/products`, `GET /api/products/{id}`, `POST /api/orders`, `GET /api/orders/track`
- `AdminController.java`: `/api/admin/*` requires the existing `X-Admin-Key` header at the application level
- Frontend `shop.js` loads `/api/products` **without an API key**, so the ordinary storefront must stay public.
- Existing ClusterIP manifest: `globalshop-service` in namespace `globalshop`, port `8080`, selector `app: globalshop`
- Existing NodePort manifest: `globalshop-nodeport` on `30080`; this older entry point bypasses Kong if it remains exposed.

## Why Kong for THIS app?

A partner mobile app can use a separate `/partner/products` URL protected by an API key and consumer-specific rate limit. Checkout and order tracking also get basic IP-based rate limits. Kong forwards all allowed traffic to the **unchanged** Spring Boot application. This is a teaching exercise for API policy management; one monolith does not require a dedicated gateway simply to work.

## Architecture

Customer or partner -> Kong `globalshop-kong:80` -> existing `globalshop-service:8080` -> Spring Boot -> Database

## Prerequisites

- A running Kubernetes cluster (iximiuz works; GKE also works)
- `kubectl` configured
- Existing `globalshop` namespace, deployment/pods, `globalshop-service` ClusterIP
- Local TCP port 8000 free for `kubectl port-forward`
- Kubernetes NodePort 30090 not already allocated (otherwise edit or remove `nodePort` in 03 file)

Check your backend first:

```bash
kubectl get pods -n globalshop --show-labels
kubectl get svc globalshop-service -n globalshop
kubectl get endpointslice -n globalshop -l kubernetes.io/service-name=globalshop-service
```

To check Java API separately (run port-forward in terminal A, curl in B):

```bash
kubectl -n globalshop port-forward svc/globalshop-service 8080:8080
# second terminal:
curl -i http://localhost:8080/api/products
```

Stop that port-forward before continuing.

## Deploy Kong

```bash
kubectl apply -f 01-kong-config.yaml
kubectl apply -f 02-kong-deployment.yaml
kubectl apply -f 03-kong-service.yaml
kubectl rollout status deployment/globalshop-kong -n globalshop
kubectl get pods,svc -n globalshop
```

If the Kong pod fails to start:

```bash
kubectl logs -n globalshop deployment/globalshop-kong --tail=100
kubectl describe pod -n globalshop -l app=globalshop-kong
```

Run port-forward in terminal A:

```bash
kubectl -n globalshop port-forward svc/globalshop-kong 8000:80
```

In terminal B:

```bash
curl -i http://localhost:8000/                           # 200 HTML
curl -i http://localhost:8000/api/products                # 200 JSON
curl -i http://localhost:8000/partner/products            # 401 missing key
curl -i -H 'apikey: demo-key-change-me' http://localhost:8000/partner/products # 200 JSON
```

Run repeated calls within the same minute to demonstrate 429:

```bash
for i in 1 2 3 4 5 6; do
  curl -s -o /dev/null -w '%{http_code}\n' -H 'apikey: demo-key-change-me' \
    http://localhost:8000/partner/products
done
```

Expect up to three successful calls per minute for that consumer on the single Kong instance; later calls yield 429. Calls made earlier in the same minute count. Ordinary `/api/products` remains public. `/api/admin/*` continues to rely on the Java backend's `X-Admin-Key`, so no new admin API bypass is introduced by the lab.

Optional helper: `bash 04-test-commands.sh`.

## What each YAML does

1. `01-kong-config.yaml`: ConfigMap holding Kong's declarative config. It defines Services (backends), Routes (request matching), one Consumer + credential, and Plugins (key-auth + rate-limiting).
2. `02-kong-deployment.yaml`: Kong pod, database-free mode, mounts declarative config and listens on 8000.
3. `03-kong-service.yaml`: NodePort service on 30090 forwarding to Kong pod's port 8000.

## About GKE

On GKE, NodePort does not automatically provide a public IP. `kubectl port-forward` is the simplest test. For a public test, use a supported external load balancer / gateway configuration with HTTPS and do not leave a training API key internet-accessible. A `LoadBalancer` type service may provision billable Google Cloud resources; don't turn it on casually.

## Security and scope

- This is an **isolated demo only**; the dummy API key is deliberately stored in a ConfigMap for reproducibility.
- The product catalog remains publicly accessible on `/api/products`, so `/partner/products` is **not a confidentiality boundary**.
- `local` rate limiting is per Kong instance, not a reliable shared global rate limit across multiple gateway replicas. Use a distributed strategy for production.
- Rate limiting is not a substitute for fraud controls, idempotency keys, app authorization or CAPTCHA/WAF when appropriate.
- Do not remove Java's `X-Admin-Key` check just because a gateway is present. Production should replace this shared-key mechanism with proper identity and role-based authorization.
- Existing direct NodePort `30080`, if enabled, bypasses Kong. Lock down/remove the old external path after gateway validation. **Do not delete** the ClusterIP `globalshop-service`.
- For production, plan TLS/HTTPS, secret management, proper JWT/OIDC, observability, a maintained version policy, and a Kubernetes-native Kong controller if managing routing through Kubernetes Gateway API.
- ConfigMap mounted via subPath is not automatically reloaded: after changes to config, run `kubectl rollout restart deployment/globalshop-kong -n globalshop`.

## Cleanup (Kong only)

```bash
kubectl delete -f 03-kong-service.yaml
kubectl delete -f 02-kong-deployment.yaml
kubectl delete -f 01-kong-config.yaml
```

No Java app components, application database or existing Kubernetes resources are removed by these commands.
