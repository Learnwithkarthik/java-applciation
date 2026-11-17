# NGINX: classic Ingress vs modern Gateway API

## A. Classic Kubernetes Ingress (maintained F5 NGINX Ingress Controller)

```bash
helm install nic oci://ghcr.io/nginx/charts/nginx-ingress \
  --version 2.8.1 \
  --create-namespace -n nginx-ingress \
  --set controller.service.type=NodePort

kubectl get ingressclass
```

Use: `15-ingress/*.yaml`.

## B. Modern Gateway API (NGINX Gateway Fabric)

```bash
kubectl kustomize \
  "https://github.com/nginx/nginx-gateway-fabric/config/crd/gateway-api/standard?ref=v2.8.1" \
  | kubectl apply -f -

helm install ngf oci://ghcr.io/nginx/charts/nginx-gateway-fabric \
  --create-namespace -n nginx-gateway \
  --set nginx.service.type=NodePort

kubectl get gatewayclass
```

Use: `16-gateway-api-nginx/*.yaml`.

Do not confuse the retired community **ingress-nginx** project with the maintained F5 **NGINX Ingress Controller**.
