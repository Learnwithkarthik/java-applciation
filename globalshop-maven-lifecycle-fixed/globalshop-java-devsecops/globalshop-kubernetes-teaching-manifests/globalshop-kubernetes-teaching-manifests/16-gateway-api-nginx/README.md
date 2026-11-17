# NGINX Gateway Fabric / Gateway API

This is the recommended modern replacement lab for Ingress.

Objects:

- `GatewayClass` — installed/owned by the controller (`nginx`)
- `Gateway` — requests an NGINX data plane / listener
- `HTTPRoute` — routes requests to Kubernetes Services
- `Service` — points to the application Pods

Check:

```bash
kubectl get gatewayclass
kubectl get gateway -n globalshop
kubectl describe gateway globalshop-gateway -n globalshop
kubectl get httproute -n globalshop
kubectl describe httproute globalshop -n globalshop
kubectl get svc -n globalshop
```
