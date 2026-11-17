# RBAC verification

```bash
kubectl apply -f 12-rbac/

kubectl auth can-i list pods   --as=system:serviceaccount:globalshop:globalshop-reader   -n globalshop

kubectl auth can-i delete pods   --as=system:serviceaccount:globalshop:globalshop-reader   -n globalshop
```

Expected: `yes` for list, `no` for delete.
