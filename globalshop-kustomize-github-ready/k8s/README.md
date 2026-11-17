# Kustomize hands-on lab — GlobalShop

## Concept

- `base/` holds the common Deployment and ClusterIP Service.
- `overlays/dev`, `overlays/staging`, `overlays/prod` reuse the base with environment-specific namespace, resource requests/limits and demo-data configuration.
- **Namespaces are optional for Kustomize.** Here we use three to demonstrate isolation on one cluster; these are not separate production-grade clusters.
- `kubectl apply -k` renders and applies an overlay; `kubectl kustomize` renders it without applying.

## Prerequisites

1. An iximiuz Kubernetes lab with `kubectl` configured.
2. A **public Docker image built from the included Java source**. The `base/deployment.yaml` currently references `karthikeya91101/myapp:v2` from earlier labs, but **this image has not been verified to contain this uploaded application**. Update that reference if necessary.
3. To build and push your own image from the repository root:

```bash
cd globalshop-maven-lifecycle-fixed/globalshop-java-devsecops
docker build -t YOUR_DOCKERHUB_USERNAME/globalshop-java:v1 .
docker push YOUR_DOCKERHUB_USERNAME/globalshop-java:v1
```

Then change the image field in `k8s/base/deployment.yaml` to your public image before applying. You can instead use Kustomize `images:` per overlay later.

## 1 — Inspect the base

```bash
kubectl kustomize k8s/base
```

## 2 — Render each overlay (no changes to cluster)

```bash
kubectl kustomize k8s/overlays/dev
kubectl kustomize k8s/overlays/staging
kubectl kustomize k8s/overlays/prod
```

Notice the generated `namespace`, `environment` labels, and different CPU/memory limits and `SEED_DEMO_DATA` values.

## 3 — Apply the overlays

Each overlay includes its Namespace resource, so namespaces are automatically created if they don't exist. If you already created them manually, that's fine.

```bash
kubectl apply -k k8s/overlays/dev
kubectl apply -k k8s/overlays/staging
kubectl apply -k k8s/overlays/prod
```

## 4 — Verify

```bash
kubectl get deployment,po,svc -n globalshop-dev
kubectl get deployment,po,svc -n globalshop-staging
kubectl get deployment,po,svc -n globalshop-prod
kubectl rollout status deployment/globalshop -n globalshop-dev
kubectl describe deployment globalshop -n globalshop-prod
```

If pods fail, inspect:

```bash
kubectl describe pod -l app=globalshop -n globalshop-dev
kubectl logs -l app=globalshop -n globalshop-dev --tail=100
```

## 5 — Test from your machine / terminal

```bash
kubectl -n globalshop-dev port-forward svc/globalshop-service 8080:80
```

In another terminal on the **same machine**:

```bash
curl -i http://localhost:8080/actuator/health
curl -i http://localhost:8080/
```

For the iximiuz hosted lab, use the platform's forwarded-port preview when `localhost` isn't available in your local browser. Stop the port-forward with Ctrl+C. For staging/prod, use `-n globalshop-staging` or `-n globalshop-prod`, and choose an available local port.

## 6 — Demonstrate the Kustomize benefit

Modify *only* `k8s/overlays/staging/deployment-patch.yaml` (e.g., CPU requests), render again, then apply the staging overlay. The base and other environments remain unchanged. Keep replicas at 1 for the in-memory H2 lab.

```bash
kubectl kustomize k8s/overlays/staging
kubectl apply -k k8s/overlays/staging
```

## Cleanup

To remove the demo workloads:

```bash
kubectl delete -k k8s/overlays/dev
kubectl delete -k k8s/overlays/staging
kubectl delete -k k8s/overlays/prod
```

**WARNING:** This cleanup also deletes the three demo namespaces through the included `namespace.yaml` files, including unrelated resources placed in those namespaces. Inspect the namespaces first.

## Limits and production notes

- All three overlays run H2 in-memory. Data is lost when pods restart and each pod has independent data. The staging/prod names are learning labels, **not production readiness**.
- The application provides a development default admin key in `application.yml`. Never expose the admin endpoints publicly with that default. Use Kubernetes Secrets and a real authentication model before a production deployment.
- No Ingress, TLS, PostgreSQL persistence, image registry authentication, or resource quotas are configured for this introductory lab.
- Image reference is not verified; `ImagePullBackOff` or running a different application means you must build and reference the correct image.
