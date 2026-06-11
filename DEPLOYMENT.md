# Deployment Guide

## Table of Contents
1. [Local Development Setup](#local-development-setup)
2. [Docker Deployment](#docker-deployment)
3. [Kubernetes Deployment](#kubernetes-deployment)
4. [Verification Steps](#verification-steps)

---

## Local Development Setup

### Prerequisites
- Java 21 (verify: `java -version`)
- Maven 3.9+ (verify: `mvn -version`)
- MySQL 8.0+ running locally

### Step 1: Database Setup
```bash
mysql -u root -p <<EOF
CREATE DATABASE IF NOT EXISTS country_info_db;
CREATE USER IF NOT EXISTS 'countryinfo'@'localhost' IDENTIFIED BY 'countryinfo_secret';
GRANT ALL PRIVILEGES ON country_info_db.* TO 'countryinfo'@'localhost';
FLUSH PRIVILEGES;
EOF
```

### Step 2: Build (includes SOAP stub generation from WSDL)
```bash
./mvnw clean package -DskipTests
```

### Step 3: Run
```bash
./mvnw spring-boot:run \
  -Dspring-boot.run.arguments="--DB_HOST=localhost --DB_USERNAME=countryinfo --DB_PASSWORD=countryinfo_secret"
```

### Step 4: Verify
```bash
curl -s http://localhost:8080/actuator/health | jq .
```

---

## Docker Deployment

### Prerequisites
- Docker 24+
- Docker Compose v2+

### Step 1: Build and Start
```bash
docker-compose up --build -d
```

### Step 2: Check Container Status
```bash
docker-compose ps
docker-compose logs -f app
```

### Step 3: Verify
```bash
# Wait ~60s for app startup, then:
curl -s http://localhost:8080/actuator/health | jq .
```

### Step 4: Test the API
```bash
# Create a country
curl -X POST http://localhost:8080/api/v1/countries \
  -H "Content-Type: application/json" \
  -d '{"name": "kenya"}'

# Fetch all countries
curl -s http://localhost:8080/api/v1/countries | jq .
```

### Stopping
```bash
docker-compose down           # Stop containers
docker-compose down -v        # Stop and remove volumes (deletes DB data)
```

---

## Kubernetes Deployment

### Prerequisites
- `kubectl` configured and connected to a cluster
- Docker registry accessible from the cluster (or local images with Minikube/Kind)

### Step 1: Build and Push Docker Image
```bash
# Build the image
docker build -t country-info-service:1.0.0 .

# For Minikube (load image directly):
minikube image load country-info-service:1.0.0

# For remote registry:
docker tag country-info-service:1.0.0 <registry>/country-info-service:1.0.0
docker push <registry>/country-info-service:1.0.0
# Update k8s/deployment.yml image field accordingly
```

### Step 2: Create Namespace
```bash
kubectl apply -f k8s/namespace.yml
```

### Step 3: Create ConfigMap and Secrets
```bash
kubectl apply -f k8s/configmap.yml
kubectl apply -f k8s/secret.yml
```

> **Production Note**: Replace base64-encoded secrets with External Secrets Operator or Vault integration.

### Step 4: Deploy MySQL
```bash
kubectl apply -f k8s/mysql-deployment.yml

# Wait for MySQL to be ready
kubectl -n country-info wait --for=condition=available deployment/country-info-mysql --timeout=120s
```

### Step 5: Deploy Application
```bash
kubectl apply -f k8s/deployment.yml
kubectl apply -f k8s/service.yml

# Wait for rollout
kubectl -n country-info rollout status deployment/country-info-service --timeout=180s
```

### Step 6: Configure Auto-Scaling
```bash
kubectl apply -f k8s/hpa.yml
```

### Step 7: Configure Ingress (Optional)
```bash
kubectl apply -f k8s/ingress.yml
```

> Requires an Ingress controller (e.g., nginx-ingress) installed in the cluster.

### Step 8: Verify Deployment
```bash
# Check all resources
kubectl -n country-info get all

# Check pod logs
kubectl -n country-info logs -l app.kubernetes.io/name=country-info-service -f

# Port-forward for local access
kubectl -n country-info port-forward svc/country-info-service 8080:80

# Test health endpoint
curl http://localhost:8080/actuator/health
```

### Full Deployment Script
```bash
#!/bin/bash
set -e

echo "Deploying Country Info Service to Kubernetes..."

kubectl apply -f k8s/namespace.yml
kubectl apply -f k8s/configmap.yml
kubectl apply -f k8s/secret.yml
kubectl apply -f k8s/mysql-deployment.yml

echo "Waiting for MySQL..."
kubectl -n country-info wait --for=condition=available deployment/country-info-mysql --timeout=120s

kubectl apply -f k8s/deployment.yml
kubectl apply -f k8s/service.yml
kubectl apply -f k8s/hpa.yml
kubectl apply -f k8s/ingress.yml

echo "Waiting for application..."
kubectl -n country-info rollout status deployment/country-info-service --timeout=180s

echo "Deployment complete. Resources:"
kubectl -n country-info get all
```

---

## Verification Steps

### Health Check
```bash
curl http://localhost:8080/actuator/health
# Expected: {"status":"UP","components":{"db":{"status":"UP"},"diskSpace":{"status":"UP"}}}
```

### Create Country
```bash
curl -X POST http://localhost:8080/api/v1/countries \
  -H "Content-Type: application/json" \
  -d '{"name": "tanzania"}'
```

### Fetch All Countries
```bash
curl http://localhost:8080/api/v1/countries
```

### Check Prometheus Metrics
```bash
curl http://localhost:8080/actuator/prometheus | grep country
```

### Check Circuit Breaker Status
```bash
curl http://localhost:8080/actuator/health | jq '.components.circuitBreakers'
```
