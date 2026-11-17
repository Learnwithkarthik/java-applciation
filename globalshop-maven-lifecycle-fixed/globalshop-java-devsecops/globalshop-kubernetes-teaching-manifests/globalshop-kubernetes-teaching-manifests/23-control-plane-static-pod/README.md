# Control plane and Static Pod lab

Core control-plane components are not normal application objects that you usually deploy with `kubectl apply`.
In a kubeadm cluster, components such as the API server, scheduler and controller manager are commonly run as **static Pods** managed by the kubelet on the control-plane node.

On a disposable lab only:

```bash
sudo cp 23-control-plane-static-pod/01-static-pod-example.yaml /etc/kubernetes/manifests/static-nginx-demo.yaml
kubectl get pods -A -o wide | grep static-nginx

# cleanup
sudo rm /etc/kubernetes/manifests/static-nginx-demo.yaml
```

Do not modify the real kube-apiserver / scheduler / controller-manager manifests for a beginner demo.
