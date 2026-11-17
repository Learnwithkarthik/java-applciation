# NGINX Ingress hands-on

The `Ingress` object and an Ingress controller are different things:

1. `Ingress` is the Kubernetes API object containing routing rules.
2. A controller watches the object and configures a real reverse proxy/load balancer.

## Important 2026 distinction

The community **ingress-nginx** project was retired in March 2026. That does **not** mean every NGINX-based ingress controller is gone. F5 maintains **NGINX Ingress Controller** with NGINX Open Source.

For an iximiuz / kubeadm lab, install the maintained F5 NGINX Ingress Controller and expose it as NodePort:

```bash
helm install nic oci://ghcr.io/nginx/charts/nginx-ingress \
  --version 2.8.1 \
  --create-namespace -n nginx-ingress \
  --set controller.service.type=NodePort

kubectl get ingressclass
kubectl get svc -n nginx-ingress
```

The chart creates an IngressClass named `nginx`, matching the manifests in this folder.

Then deploy the app + ClusterIP service + Ingress:

```bash
kubectl apply -f 03-deployment/01-deployment-rollingupdate.yaml
kubectl apply -f 04-services/01-clusterip.yaml
kubectl apply -f 15-ingress/01-ingress.yaml

kubectl get ingress -n globalshop
kubectl get svc -n nginx-ingress
```

For a new architecture, also teach `16-gateway-api-nginx` because Kubernetes recommends Gateway API for new designs.
