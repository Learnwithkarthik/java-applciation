# GlobalShop — Java Spring Boot with Kubernetes Kustomize

This repository contains the original Java application, Dockerfile, Java/DevSecOps materials and GitHub Actions workflow from the uploaded source, plus a new `k8s/` Kustomize demo.

## Start here

- **[Kustomize lab guide](k8s/README.md)** — base, overlays, deploy and verify on iximiuz.
- **[Original Java application README](globalshop-maven-lifecycle-fixed/globalshop-java-devsecops/README.md)** — Java, Docker, Maven and local app instructions.
- **Java application**: `globalshop-maven-lifecycle-fixed/globalshop-java-devsecops/`
- **Kustomize**: `k8s/base/` and `k8s/overlays/{dev,staging,prod}/`

**Before deploying:** the image `karthikeya91101/myapp:v2` in the base is an earlier example and has **not** been verified to match the uploaded source. Build and push this application's image or update the image to a known-correct public reference as described in `k8s/README.md`.

GitHub Action `gcp-auth.yaml` is preserved as supplied. It pushes an image to a particular Artifact Registry repository and requires matching GCP/GitHub configuration; it does not automatically change the Kustomize image field.
