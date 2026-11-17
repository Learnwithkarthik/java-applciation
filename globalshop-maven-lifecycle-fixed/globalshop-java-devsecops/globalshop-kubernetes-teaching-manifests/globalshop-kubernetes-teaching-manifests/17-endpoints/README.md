# EndpointSlice hands-on

```bash
kubectl apply -f 17-endpoints/01-backend-pod.yaml
kubectl apply -f 17-endpoints/02-selectorless-service.yaml

POD_IP=$(kubectl get pod endpoint-backend -n globalshop -o jsonpath='{.status.podIP}')
sed "s/__BACKEND_POD_IP__/$POD_IP/"   17-endpoints/03-manual-endpointslice-template.yaml   | kubectl apply -f -

kubectl get endpointslice -n globalshop
kubectl run curl --rm -it --restart=Never --image=curlimages/curl:8.12.1   -n globalshop -- curl -I http://manual-backend
```

The `99-legacy-endpoints-template.yaml` file is included only to explain the older `Endpoints` API.
