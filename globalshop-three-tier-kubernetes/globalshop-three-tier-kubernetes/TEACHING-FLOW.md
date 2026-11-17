# Instructor walkthrough: how three Pods talk

## 1. Pod vs Service

A Pod owns an ephemeral Pod IP. A Service is a stable DNS name and virtual access point to matching Pods. Kubernetes selectors connect Services to Pods, tracked via EndpointSlices.

## 2. Browser -> frontend

The frontend Pod hosts the same HTML/CSS/JS files from the uploaded application, now served by Nginx. The frontend NodePort Service exposes the UI in the lab.

## 3. Frontend -> backend

The original JS calls relative `/api/products`, `/api/orders`, `/api/orders/track`, and `/api/admin/*`. Nginx receives each HTTP `/api/` request and forwards it to `http://backend-service:8080` through Kubernetes DNS. The frontend and backend are distinct Pods.

## 4. Backend -> database

The original Spring Boot app reads DB_URL, DB_USER, and DB_PASSWORD. Here DB_URL uses `jdbc:postgresql://postgres-service:5432/globalshop`. PostgreSQL data is persisted by a StatefulSet claim template.

## 5. Hands-on questions

- What fails when the backend Pod is deleted? How does Deployment self-heal?
- Why shouldn't Nginx use the backend Pod IP directly?
- What happens if you scale only the backend to two Pods?
- Why does the database use StatefulSet and PVC instead of a plain Pod?
- What is the difference between a NodePort frontend and a ClusterIP backend?
- Which TCP ports are in use on each hop?
- What is the difference between an HTTP reverse proxy and a Kubernetes Service?
- How can a NetworkPolicy limit direct access to database Pods?
