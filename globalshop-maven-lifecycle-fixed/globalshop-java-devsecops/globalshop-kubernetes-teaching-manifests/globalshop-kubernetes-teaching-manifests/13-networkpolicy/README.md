# NetworkPolicy demo

Apply in this order:

```bash
kubectl apply -f 13-networkpolicy/01-app-and-service.yaml
kubectl apply -f 13-networkpolicy/04-client-pods.yaml

# Before policy: both should work
kubectl exec -n globalshop allowed-client -- curl -s http://globalshop-netpol:8080/actuator/health
kubectl exec -n globalshop blocked-client -- curl -s http://globalshop-netpol:8080/actuator/health

kubectl apply -f 13-networkpolicy/02-default-deny-ingress.yaml
kubectl apply -f 13-networkpolicy/03-allow-client-to-app.yaml

# After policy: allowed-client should work; blocked-client should fail/time out.
```
