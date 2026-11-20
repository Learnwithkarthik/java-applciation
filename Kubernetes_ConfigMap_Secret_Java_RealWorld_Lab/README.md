# Real-world Kubernetes ConfigMap + Secret Lab: GlobalShop

## Based on the uploaded Java source

The uploaded Spring Boot app (`src/main/resources/application.yml`) reads: `DB_URL`, `DB_USER`, `DB_PASSWORD`, `PORT`, `ADMIN_API_KEY`, `SEED_DEMO_DATA`. It exposes `/api/products`, `/api/admin/stats` (protected by `X-Admin-Key`), and `/actuator/health`.

**IMPORTANT**: The provided `karthikeya91101/myapp:v2` image tag is supplied by the user, but has not been verified against the uploaded source. These steps assume that image contains the uploaded GlobalShop Spring Boot application.

## 1. Prerequisites

A running Kubernetes cluster with accessible `kubectl`, image pulls enabled, a default NodePort range that includes 30080, and enough memory for Java and PostgreSQL. The example works in iximiuz lab clusters if a reachable node IP is available. Use `kubectl port-forward` when node IP/NodePort access is not available.

## 2. Deploy (run from this folder)

```bash
kubectl apply -f 00-namespace.yaml
kubectl apply -f 01-configmap.yaml
kubectl apply -f 02-secret.yaml
kubectl apply -f 03-postgres.yaml
kubectl -n shop-demo rollout status deployment/shop-postgres --timeout=180s
kubectl apply -f 04-app.yaml
kubectl -n shop-demo rollout status deployment/shop-app --timeout=240s
kubectl -n shop-demo get pods,svc,configmap,secret
```

## 3. Access the Java application

Use a separate terminal with:

```bash
kubectl -n shop-demo port-forward service/shop-app 8080:80
```

Then from your local terminal:

```bash
curl http://localhost:8080/actuator/health
curl http://localhost:8080/api/products
curl -i http://localhost:8080/api/admin/stats
curl -i -H 'X-Admin-Key: wrong' http://localhost:8080/api/admin/stats
curl -i -H 'X-Admin-Key: demo-admin-key-change-me' http://localhost:8080/api/admin/stats
```

Expected: health=UP, products JSON array, missing/wrong key=401, matching demo key=200. (Only if the supplied image actually matches the uploaded source.)

Alternatively from a machine able to reach a Kubernetes node:

```bash
curl http://<NODE_IP>:30080/api/products
```

## 4. Show the non-secret ConfigMap values

```bash
kubectl -n shop-demo describe configmap shop-config
kubectl -n shop-demo exec deploy/shop-app -- printenv DB_URL
kubectl -n shop-demo exec deploy/shop-app -- printenv PORT
```

Update the logging configuration and restart, since environment variables in *running* containers do not update in-place:

```bash
kubectl -n shop-demo patch configmap shop-config --type merge -p '{"data":{"LOGGING_LEVEL_ORG_SPRINGFRAMEWORK_WEB":"DEBUG"}}'
kubectl -n shop-demo rollout restart deployment/shop-app
kubectl -n shop-demo rollout status deployment/shop-app
kubectl -n shop-demo exec deploy/shop-app -- printenv LOGGING_LEVEL_ORG_SPRINGFRAMEWORK_WEB
curl http://localhost:8080/api/products
kubectl -n shop-demo logs deployment/shop-app --tail=60
```

Note: during a single-replica restart, a brief interruption may occur.

## 5. Show Secret-backed admin authentication

```bash
kubectl -n shop-demo get secret shop-secret
kubectl -n shop-demo describe secret shop-secret
```

Do **not** echo credentials in public class recordings. `kubectl get secret -o yaml` contains base64-encoded (not encrypted) values and should not be shown as proof of security.

Rotate only the ADMIN_API_KEY (do not rotate DB_PASSWORD in this lab without changing the actual PostgreSQL role password):

```bash
kubectl -n shop-demo patch secret shop-secret --type merge -p '{"stringData":{"ADMIN_API_KEY":"rotated-admin-key-demo"}}'
kubectl -n shop-demo rollout restart deployment/shop-app
kubectl -n shop-demo rollout status deployment/shop-app
curl -i -H 'X-Admin-Key: demo-admin-key-change-me' http://localhost:8080/api/admin/stats
curl -i -H 'X-Admin-Key: rotated-admin-key-demo' http://localhost:8080/api/admin/stats
```

Expected: old key=401; new key=200. Do not include the new key in actual production shell history; this lab uses throwaway credentials.

## 6. Troubleshooting

```bash
kubectl -n shop-demo get pods -o wide
kubectl -n shop-demo describe pod -l app=shop-app
kubectl -n shop-demo logs deploy/shop-app --tail=100
kubectl -n shop-demo logs deploy/shop-postgres --tail=100
kubectl -n shop-demo get endpoints shop-postgres shop-app
kubectl -n shop-demo get events --sort-by=.lastTimestamp
```

If Pods fail: check Docker image tag/architecture and image-pull access; memory/CPU; database readiness; `DB_URL`; or mismatch between published image and source. If `curl localhost` fails, confirm port-forward is still running. If port 8080 is used, choose `kubectl ... port-forward svc/shop-app 18080:80` and browse localhost:18080.

## 7. Cleanup

```bash
kubectl delete namespace shop-demo
```

## Production caveats

- This lab intentionally uses `emptyDir` for PostgreSQL; it is ephemeral and NOT production suitable. Use a PersistentVolumeClaim or managed DB.
- Plain Kubernetes Secrets are not automatically encrypted at rest unless encryption at rest is configured; base64 != encryption. Use RBAC least privilege, enable encryption at rest, and consider an external secrets provider.
- ConfigMaps and Secrets injected as environment variables are only read when a container starts. To refresh, restart the workload. Mounted files can update eventually, subject to mount style and app behavior.
- This demo intentionally uses one replica and a training-only shared admin key. Production requires secure user authentication/authorization, TLS, backup, and proper schema migration.
