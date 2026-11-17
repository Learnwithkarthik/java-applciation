# GlobalShop Kubernetes Teaching Manifest Pack

This pack is built for the uploaded **GlobalShop Spring Boot application**.

Application facts used by these manifests:

- Container port: `8080`
- Health endpoint: `/actuator/health`
- Public API: `/api/products`
- Admin UI: `/admin.html`
- Default demo DB mode: H2 file database
- Full storage lab: PostgreSQL StatefulSet
- Teaching image configured in this pack: `karthikeya91101/myapp:v2`

> If your image tag is different, replace `karthikeya91101/myapp:v2` in the YAML files before applying them.

## Important teaching rule

**Do not run `kubectl apply -f .` on the whole repository.**
The folders are independent labs and intentionally reuse names such as `globalshop` and `globalshop-service`.
Apply one lab at a time and clean it up before moving to another lab.

## Recommended lab order

| # | Topic | Main Kubernetes objects |
|---|---|---|
| 00 | Namespace | Namespace + Pod Security Admission labels |
| 01 | Pod | Pod, probes, resources |
| 02 | ReplicaSet | ReplicaSet |
| 03 | Deployment & strategies | Deployment, RollingUpdate, Recreate, Blue/Green, Canary |
| 04 | Services | ClusterIP, NodePort, LoadBalancer, ExternalName, Headless |
| 05 | Configuration | ConfigMap, Secret, envFrom |
| 06 | Volumes | emptyDir, PVC, persistent H2 |
| 07 | StatefulSet | PostgreSQL StatefulSet, Headless Service, PVC template |
| 08 | DaemonSet | One Pod per node |
| 09 | Jobs | Job, CronJob |
| 10 | HPA | HorizontalPodAutoscaler (`autoscaling/v2`) |
| 11 | Resource management | ResourceQuota, LimitRange, QoS examples |
| 12 | RBAC | ServiceAccount, Role, RoleBinding |
| 13 | NetworkPolicy | Default deny + allow rules |
| 14 | Pod security | Pod Security Admission + restricted Pod |
| 15 | Ingress | Standard Kubernetes Ingress object |
| 16 | NGINX Gateway / API Gateway | Gateway API: Gateway + HTTPRoute |
| 17 | Endpoints | EndpointSlice + legacy Endpoints comparison |
| 18 | CRD | CustomResourceDefinition + Custom Resource |
| 19 | Availability | PriorityClass + PodDisruptionBudget |
| 20 | Probes | startup, readiness, liveness |
| 21 | Scheduling | nodeSelector, affinity, tolerations |
| 22 | Init container | initContainers |
| 23 | Control plane / Static Pod | static Pod example (not `kubectl apply`) |

## Start every normal lab

```bash
kubectl apply -f 00-namespace/01-namespace.yaml
kubectl get ns globalshop --show-labels
```

Check the nodes:

```bash
kubectl get nodes -o wide
```

## Fast verification commands

```bash
kubectl get all -n globalshop
kubectl get pods -n globalshop -o wide
kubectl describe pod -n globalshop <POD_NAME>
kubectl logs -n globalshop <POD_NAME>
```

To test the app from inside the cluster after a Service is created:

```bash
kubectl run curl --rm -it --restart=Never   --image=curlimages/curl:8.12.1 -n globalshop --   curl -s http://globalshop-service:8080/api/products
```

## iximiuz notes

- `ClusterIP` works inside the cluster.
- `NodePort` is the easiest way to access the app from a node in a bare-metal/kubeadm lab.
- A `LoadBalancer` Service needs a load-balancer implementation. Without one, `EXTERNAL-IP` can stay `Pending`.
- HPA needs Metrics Server. Check with `kubectl top nodes` and `kubectl top pods -n globalshop`.
- NetworkPolicy needs a CNI that enforces NetworkPolicy (for example Calico/Cilium).
- Gateway API needs Gateway API CRDs **and a Gateway controller**.

## About Ingress and NGINX in 2026

The Kubernetes `Ingress` API still exists, but it is frozen and Kubernetes recommends Gateway API for new designs. The community **ingress-nginx** controller was retired in March 2026.

For teaching classic Ingress, the `15-ingress` folder uses the actively maintained **F5 NGINX Ingress Controller** (NGINX Open Source), not the retired community ingress-nginx project.

For the modern NGINX lab, use **NGINX Gateway Fabric**, which implements Gateway API with NGINX as the data plane. The `16-gateway-api-nginx` folder contains `Gateway` and `HTTPRoute` manifests.

### Install NGINX Gateway Fabric for an iximiuz-style lab

Prerequisites: `helm` and `kubectl`.

```bash
# Gateway API resources supported by the current NGF release
kubectl kustomize   "https://github.com/nginx/nginx-gateway-fabric/config/crd/gateway-api/standard?ref=v2.8.1"   | kubectl apply -f -

# NGINX Gateway Fabric. NodePort is convenient in a bare-metal lab.
helm install ngf oci://ghcr.io/nginx/charts/nginx-gateway-fabric   --create-namespace -n nginx-gateway   --set nginx.service.type=NodePort

kubectl get gatewayclass
```

Then:

```bash
kubectl apply -f 03-deployment/01-deployment-rollingupdate.yaml
kubectl apply -f 04-services/01-clusterip.yaml
kubectl apply -f 16-gateway-api-nginx/01-gateway.yaml
kubectl apply -f 16-gateway-api-nginx/02-httproute.yaml
kubectl get gateway,httproute -n globalshop
kubectl get svc -n globalshop
```

## Deprecated syllabus items

### PodSecurityPolicy

`PodSecurityPolicy` was removed from Kubernetes in v1.25. Teach it as a historical concept only. Use **Pod Security Admission** today. See `14-pod-security`.

### Endpoints

The old `Endpoints` API is deprecated. Teach **EndpointSlice** as the modern object. A legacy sample is included only for comparison.

## Suggested cleanup between labs

For most labs:

```bash
kubectl delete namespace globalshop
kubectl apply -f 00-namespace/01-namespace.yaml
```

Cluster-scoped objects such as `PriorityClass` and CRDs must be deleted separately when you finish those labs.
