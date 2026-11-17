# CRD lesson

A CRD extends the Kubernetes API. This demo stores a `TrainingApp` object, but there is intentionally **no controller/operator** to turn it into a Deployment.

```bash
kubectl apply -f 18-crd/01-crd.yaml
kubectl apply -f 18-crd/02-custom-resource.yaml
kubectl get trainingapps -n globalshop
kubectl get tapp globalshop -n globalshop -o yaml
```
