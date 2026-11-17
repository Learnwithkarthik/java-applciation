# Classroom command sheet

## Pod

```bash
kubectl apply -f 01-pod/01-pod.yaml
kubectl get pod -n globalshop -o wide
kubectl describe pod globalshop-pod -n globalshop
kubectl logs globalshop-pod -n globalshop
kubectl port-forward pod/globalshop-pod 8080:8080 -n globalshop
```

Open `http://localhost:8080` or test:

```bash
curl http://localhost:8080/actuator/health
curl http://localhost:8080/api/products
```

## ReplicaSet

```bash
kubectl apply -f 02-replicaset/01-replicaset.yaml
kubectl get rs,pods -n globalshop
kubectl delete pod -n globalshop <ONE_POD>
kubectl get pods -n globalshop -w
kubectl scale rs globalshop-rs --replicas=5 -n globalshop
```

## Deployment and rollout

```bash
kubectl apply -f 03-deployment/01-deployment-rollingupdate.yaml
kubectl rollout status deployment/globalshop -n globalshop
kubectl get rs,pods -n globalshop
# After you push a second image tag, update to it, for example:
# kubectl set image deployment/globalshop globalshop=<YOUR_IMAGE>:v3 -n globalshop
kubectl rollout history deployment/globalshop -n globalshop
kubectl rollout undo deployment/globalshop -n globalshop
```

## Services

```bash
kubectl apply -f 04-services/01-clusterip.yaml
kubectl get svc,endpointslice -n globalshop

kubectl apply -f 04-services/02-nodeport.yaml
kubectl get svc globalshop-nodeport -n globalshop
curl http://<NODE-IP>:30080/api/products

kubectl apply -f 04-services/03-loadbalancer.yaml
kubectl get svc globalshop-loadbalancer -n globalshop -w
```

## ConfigMap and Secret

```bash
kubectl apply -f 05-config-secrets/01-configmap.yaml
kubectl apply -f 05-config-secrets/02-secret.yaml
kubectl get cm,secret -n globalshop
kubectl get secret globalshop-secret -n globalshop -o yaml
```

## StatefulSet

```bash
kubectl apply -f 07-statefulset/
kubectl get statefulset,pod,pvc,svc -n globalshop
kubectl exec -it postgres-0 -n globalshop -- psql -U globalshop -d globalshop
```

## DaemonSet

```bash
kubectl apply -f 08-daemonset/01-node-agent-daemonset.yaml
kubectl get ds,pods -n globalshop -o wide
```

## Jobs and CronJobs

```bash
kubectl apply -f 09-jobs/01-job.yaml
kubectl get jobs,pods -n globalshop
kubectl logs -n globalshop job/globalshop-health-check

kubectl apply -f 09-jobs/02-cronjob.yaml
kubectl get cronjob,jobs -n globalshop -w
```

## HPA

```bash
kubectl apply -f 10-hpa/
kubectl get hpa -n globalshop -w
kubectl top pods -n globalshop
```

Generate load from another terminal:

```bash
kubectl run load-generator --rm -it --restart=Never   --image=busybox:1.37 -n globalshop --   sh -c 'while true; do wget -q -O- http://globalshop-hpa:8080/api/products >/dev/null; done'
```

## ResourceQuota / LimitRange / QoS

```bash
kubectl apply -f 11-resource-management/01-resourcequota.yaml
kubectl apply -f 11-resource-management/02-limitrange.yaml
kubectl describe quota -n globalshop
kubectl describe limitrange -n globalshop
kubectl get pod -n globalshop -o custom-columns=NAME:.metadata.name,QOS:.status.qosClass
```

## RBAC

```bash
kubectl apply -f 12-rbac/
kubectl auth can-i list pods --as=system:serviceaccount:globalshop:globalshop-reader -n globalshop
kubectl auth can-i delete pods --as=system:serviceaccount:globalshop:globalshop-reader -n globalshop
```

## NetworkPolicy

See `13-networkpolicy/README.md`.

## CRD

```bash
kubectl apply -f 18-crd/01-crd.yaml
kubectl apply -f 18-crd/02-custom-resource.yaml
kubectl get tapp -n globalshop
```

## PDB / Priority

```bash
kubectl apply -f 19-availability/
kubectl get priorityclass
kubectl get pdb -n globalshop
kubectl describe pdb globalshop-pdb -n globalshop
```
