# Troubleshooting Guide

## Table of Contents
1. [Pod Crashes / CrashLoopBackOff](#pod-crashes--crashloopbackoff)
2. [Database Connection Issues](#database-connection-issues)
3. [SOAP Endpoint Failures](#soap-endpoint-failures)
4. [Memory Issues](#memory-issues)
5. [CPU Issues](#cpu-issues)
6. [Scaling Issues](#scaling-issues)
7. [Log Investigation](#log-investigation)
8. [Health Check Failures](#health-check-failures)
9. [Common HTTP Error Codes](#common-http-error-codes)

---

## Pod Crashes / CrashLoopBackOff

### Symptoms
- Pod status shows `CrashLoopBackOff` or `Error`
- Pod restarts repeatedly

### Diagnosis
```bash
# Check pod status and restart count
kubectl -n country-info get pods

# View pod events (scheduling, image pull, probe failures)
kubectl -n country-info describe pod <pod-name>

# View application logs from the crashed container
kubectl -n country-info logs <pod-name> --previous

# View recent events in the namespace
kubectl -n country-info get events --sort-by='.lastTimestamp' | tail -20
```

### Common Causes

**1. OOMKilled (Out of Memory)**
```bash
# Check if pod was OOMKilled
kubectl -n country-info describe pod <pod-name> | grep -A5 "Last State"
# Look for: Reason: OOMKilled

# Fix: Increase memory limits in deployment.yml
resources:
  limits:
    memory: 1536Mi  # Increase from 1Gi
```

**2. Database not ready**
```bash
# Check MySQL pod status
kubectl -n country-info get pods -l app.kubernetes.io/name=country-info-mysql

# The app depends on MySQL. If MySQL isn't ready, the app will fail to start.
# Check MySQL logs:
kubectl -n country-info logs -l app.kubernetes.io/name=country-info-mysql
```

**3. Invalid configuration**
```bash
# Verify ConfigMap values
kubectl -n country-info get configmap country-info-config -o yaml

# Verify Secrets exist
kubectl -n country-info get secret country-info-secret -o yaml
```

**4. Image pull failures**
```bash
# Check events for ImagePullBackOff
kubectl -n country-info describe pod <pod-name> | grep -A3 "Events"
# Ensure the image exists and registry credentials are configured
```

---

## Database Connection Issues

### Symptoms
- Application logs show `Connection refused` or `Communications link failure`
- Health endpoint returns `{"status":"DOWN","components":{"db":{"status":"DOWN"}}}`

### Diagnosis
```bash
# Check MySQL pod is running
kubectl -n country-info get pods -l app.kubernetes.io/name=country-info-mysql

# Check MySQL service exists and has endpoints
kubectl -n country-info get svc country-info-mysql
kubectl -n country-info get endpoints country-info-mysql

# Test connectivity from app pod to MySQL
kubectl -n country-info exec -it <app-pod-name> -- sh -c 'nc -zv country-info-mysql 3306'

# Check MySQL logs for auth failures
kubectl -n country-info logs -l app.kubernetes.io/name=country-info-mysql | tail -50

# Verify credentials match between secret and MySQL
kubectl -n country-info get secret country-info-secret -o jsonpath='{.data.DB_USERNAME}' | base64 -d
kubectl -n country-info get secret country-info-secret -o jsonpath='{.data.DB_PASSWORD}' | base64 -d
```

### Fixes
```bash
# If MySQL PVC is full:
kubectl -n country-info get pvc mysql-pvc
# Expand PVC if supported by storage class

# If MySQL is in CrashLoopBackOff, check data directory corruption:
kubectl -n country-info logs -l app.kubernetes.io/name=country-info-mysql

# Restart MySQL deployment:
kubectl -n country-info rollout restart deployment/country-info-mysql
```

---

## SOAP Endpoint Failures

### Symptoms
- POST requests return `502 Bad Gateway` with message about SOAP service
- Logs show `Circuit breaker is open` or `WebServiceException`

### Diagnosis
```bash
# Check application logs for SOAP errors
kubectl -n country-info logs -l app.kubernetes.io/name=country-info-service | grep -i "soap\|circuit\|retry"

# Check circuit breaker state via actuator
kubectl -n country-info port-forward svc/country-info-service 8080:80
curl http://localhost:8080/actuator/health | jq '.components.circuitBreakers'

# Test SOAP endpoint accessibility from pod
kubectl -n country-info exec -it <pod-name> -- sh -c \
  'curl -s -o /dev/null -w "%{http_code}" http://webservices.oorsprong.org/websamples.countryinfo/CountryInfoService.wso'

# Check if DNS resolution works
kubectl -n country-info exec -it <pod-name> -- nslookup webservices.oorsprong.org
```

### Fixes
```bash
# If the external SOAP service is down, the circuit breaker will open.
# The circuit breaker auto-recovers after the configured wait duration (30s).
# Check the circuit breaker config in application.yml.

# If DNS resolution fails, check CoreDNS:
kubectl -n kube-system get pods -l k8s-app=kube-dns
kubectl -n kube-system logs -l k8s-app=kube-dns

# If there's a network policy blocking egress:
kubectl -n country-info get networkpolicies
```

---

## Memory Issues

### Diagnosis
```bash
# Check current pod resource usage
kubectl -n country-info top pods

# Check JVM heap usage via metrics
kubectl -n country-info port-forward svc/country-info-service 8080:80
curl http://localhost:8080/actuator/metrics/jvm.memory.used | jq .
curl http://localhost:8080/actuator/metrics/jvm.memory.max | jq .

# Check for OOMKilled events
kubectl -n country-info get events | grep OOMKilled

# View resource limits vs actual usage
kubectl -n country-info describe pod <pod-name> | grep -A10 "Limits"
```

### Fixes
```bash
# Increase memory limits in deployment.yml and reapply
kubectl -n country-info edit deployment country-info-service
# Or edit k8s/deployment.yml and:
kubectl apply -f k8s/deployment.yml

# Tune JVM flags in ConfigMap:
# JAVA_OPTS: "-XX:MaxRAMPercentage=70.0 -XX:+UseG1GC"
```

---

## CPU Issues

### Diagnosis
```bash
# Check CPU usage
kubectl -n country-info top pods

# Check CPU throttling
kubectl -n country-info describe pod <pod-name> | grep -A5 "Limits"

# Check HPA status
kubectl -n country-info get hpa
kubectl -n country-info describe hpa country-info-service-hpa
```

### Fixes
```bash
# If CPU is consistently high, check for:
# 1. Thread pool exhaustion in SOAP client
# 2. Database query performance issues

# Increase CPU limits:
kubectl -n country-info edit deployment country-info-service

# Or let HPA scale out:
kubectl -n country-info get hpa -w  # Watch scaling events
```

---

## Scaling Issues

### Diagnosis
```bash
# Check HPA status and events
kubectl -n country-info describe hpa country-info-service-hpa

# Check if metrics-server is running
kubectl -n kube-system get deployment metrics-server

# Check current replica count vs desired
kubectl -n country-info get deployment country-info-service

# Check if pods are pending (insufficient cluster resources)
kubectl -n country-info get pods | grep Pending
kubectl -n country-info describe pod <pending-pod> | grep -A10 "Events"
```

### Fixes
```bash
# If metrics-server is not installed:
kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml

# If pods are Pending due to insufficient resources:
# Scale the cluster node pool, or reduce resource requests

# Manual scaling (bypass HPA):
kubectl -n country-info scale deployment country-info-service --replicas=5
```

---

## Log Investigation

### Real-time Log Streaming
```bash
# Follow all app pod logs
kubectl -n country-info logs -l app.kubernetes.io/name=country-info-service -f --all-containers

# Follow a specific pod
kubectl -n country-info logs <pod-name> -f

# View last 100 lines
kubectl -n country-info logs <pod-name> --tail=100
```

### Searching Logs
```bash
# Find SOAP-related errors
kubectl -n country-info logs -l app.kubernetes.io/name=country-info-service | grep -i "error\|exception\|soap"

# Find requests by correlation ID
kubectl -n country-info logs -l app.kubernetes.io/name=country-info-service | grep "abc-123-correlation-id"

# Find slow requests
kubectl -n country-info logs -l app.kubernetes.io/name=country-info-service | grep "took.*ms"
```

### Log Format
Logs follow structured format:
```
2024-01-15 10:30:00.123 [http-nio-8080-exec-1] [abc-123-uuid] INFO  CountryInfoService - Country lookup initiated for country=Kenya
```
Fields: `timestamp [thread] [correlationId] level logger - message`

---

## Health Check Failures

### Diagnosis
```bash
# Check liveness and readiness probe results
kubectl -n country-info describe pod <pod-name> | grep -A5 "Liveness\|Readiness"

# Manually test health endpoints
kubectl -n country-info port-forward <pod-name> 8080:8080
curl http://localhost:8080/actuator/health/liveness
curl http://localhost:8080/actuator/health/readiness
curl http://localhost:8080/actuator/health

# Check if startup probe is failing (pod keeps restarting before ready)
kubectl -n country-info describe pod <pod-name> | grep -A5 "Startup"
```

### Fixes
```bash
# If startup is slow (e.g., SOAP WSDL parsing takes long):
# Increase startupProbe failureThreshold in deployment.yml

# If readiness fails due to DB:
# Check database section above

# If liveness fails under load:
# Increase timeoutSeconds for liveness probe
```

---

## Common HTTP Error Codes

| Code | Meaning | Likely Cause |
|------|---------|--------------|
| 400  | Bad Request | Invalid/missing request body (`name` field blank) |
| 404  | Not Found | Country ID doesn't exist in database |
| 409  | Conflict | Country already persisted (duplicate POST) |
| 502  | Bad Gateway | SOAP service returned error or unreachable |
| 503  | Service Unavailable | Circuit breaker is open |
| 500  | Internal Server Error | Unhandled exception (check logs) |
