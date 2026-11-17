# GlobalShop — Three-tier Kubernetes student lab

This is a **source-based split of your uploaded Spring Boot application**, not three copies of the Java image.

| Tier | Pod controlled by | Image | Service | Container port |
|---|---|---|---|---|
| Frontend | Deployment, 1 replica | `karthikeya91101/globalshop-frontend:3tier-v1` | `frontend-service` NodePort `30080` | 80 |
| Backend | Deployment, 1 replica | `karthikeya91101/globalshop-backend:3tier-v1` | `backend-service` ClusterIP | 8080 |
| Database | StatefulSet, 1 replica | `postgres:17-alpine` | `postgres-service` headless | 5432 |

Architecture: Browser -> NodePort/frontend-service -> Nginx frontend Pod -> (for `/api/` requests) backend-service -> Java Pod -> postgres-service -> PostgreSQL Pod + PVC.

**Important:** The browser sends a request to Nginx. Nginx proxies `/api/` traffic to the backend via Kubernetes DNS, so browser code can continue using relative `/api/...` URLs. The frontend pod is the reverse proxy client, not the user's browser making a cross-origin request.

## Step 1 — Build two images

You need Docker locally or on a Linux build VM. Replace `karthikeya91101` with your own Docker Hub username if needed.

```bash
docker login
docker build -t karthikeya91101/globalshop-backend:3tier-v1 ./backend
docker build -t karthikeya91101/globalshop-frontend:3tier-v1 ./frontend
docker push karthikeya91101/globalshop-backend:3tier-v1
docker push karthikeya91101/globalshop-frontend:3tier-v1
```

**Images have not been built or pushed by this pack.** If changing their names, also edit the Deployment YAML. The source in `backend/` is copied from your original project, including original static assets; Nginx serves its own separate copy of those assets from `frontend/site/`.

## Step 2 — Deploy on iximiuz or another Kubernetes cluster

```bash
kubectl apply -f k8s/00-namespace.yaml
kubectl apply -f k8s/01-secret.yaml
kubectl apply -f k8s/02-postgres-service.yaml
kubectl apply -f k8s/03-postgres-statefulset.yaml
kubectl -n globalshop-3tier rollout status statefulset/postgres --timeout=180s
kubectl apply -f k8s/05-backend-service.yaml
kubectl apply -f k8s/04-backend-deployment.yaml
kubectl -n globalshop-3tier rollout status deployment/backend --timeout=240s
kubectl apply -f k8s/07-frontend-service.yaml
kubectl apply -f k8s/06-frontend-deployment.yaml
kubectl -n globalshop-3tier rollout status deployment/frontend --timeout=180s
```

If no default StorageClass is available, the PostgreSQL PVC will remain Pending. Check with `kubectl get storageclass` and install/configure lab storage first.

## Step 3 — Verify and test

```bash
kubectl -n globalshop-3tier get pods -o wide
kubectl -n globalshop-3tier get svc
kubectl -n globalshop-3tier get endpointslices
kubectl -n globalshop-3tier get pvc
kubectl -n globalshop-3tier logs deploy/backend
kubectl -n globalshop-3tier port-forward svc/frontend-service 8088:80
```

In another terminal:

```bash
curl -i http://localhost:8088/
curl -i http://localhost:8088/api/products
```

Open `http://localhost:8088` in your browser if port-forwarding is accessible to your workstation. For iximiuz, access NodePort 30080 using its allowed node/port exposure method; external NodePort reachability is environment-dependent.

## Pod communication classroom experiments

1. Show DNS: `kubectl -n globalshop-3tier exec deploy/frontend -- getent hosts backend-service`
2. Frontend -> backend: `kubectl -n globalshop-3tier exec deploy/frontend -- wget -qO- http://backend-service:8080/api/products` (nginx Alpine commonly has wget; if unavailable, use a temporary debug Pod).
3. Backend -> database: inspect `kubectl -n globalshop-3tier logs deploy/backend`; its JDBC URL is `jdbc:postgresql://postgres-service:5432/globalshop`.
4. Service discovery: inspect `kubectl -n globalshop-3tier get endpointslices -l kubernetes.io/service-name=backend-service -o wide`.
5. Scale: `kubectl -n globalshop-3tier scale deployment/backend --replicas=2` and inspect EndpointSlices. Return to 1 for the three-Pod baseline.
6. Pod replacement: `kubectl -n globalshop-3tier delete pod -l app=globalshop-backend` then see a Deployment create a replacement.
7. NetworkPolicy (optional): `kubectl apply -f optional-labs/01-networkpolicy.yaml`, then test. Requires a NetworkPolicy-enforcing CNI. Existing default allowances for DNS/egress are unchanged by these ingress-only policies.
8. Apply the optional Ingress only after installing an appropriate Ingress controller and setting its real IngressClass. For Gateway API, install its CRDs and Gateway controller first and use a valid GatewayClass.

## Classroom caution

- `k8s/01-secret.yaml` contains a **lab-only demonstration password and admin key**. Replace them before shared/public deployment. For existing PostgreSQL PVC data, changing the Secret alone will not change the database user's stored password.
- One replica per tier is for learning and **not** high availability. PostgreSQL is stateful, so do not casually increase its replica count.
- All three tiers use Services and DNS for stable discovery; no hardcoded Pod IPs.
- `backend/` still includes the original Spring Boot static assets. Nginx is the public frontend, but an internal Java backend can still serve them; removing that duplication in a later refactor is optional.
- Do not apply the previous monolithic GlobalShop teaching pack in this namespace; keep experiments separate.
- Cleanup deletes resources; PVC deletion may permanently discard database contents depending on StorageClass reclaim policy.

## Cleanup

```bash
kubectl delete namespace globalshop-3tier
```
