# Architectural Decision Records (ADR)

## ADR-001: Layered Architecture with Strict Separation of Concerns

**Status**: Accepted

**Context**: The assessment requires building a microservice that integrates SOAP and REST, persists data, and should be production-grade.

**Decision**: Adopt a strict layered architecture: Controller -> Service -> Repository/Integration, with DTOs at API boundaries and entities at persistence boundaries. The mapper layer handles all transformations.

**Rationale**:
- Controllers remain thin - no business logic, only request/response handling
- Service layer orchestrates business workflows (SOAP calls, persistence, validation)
- Integration layer encapsulates all external service communication
- Changes to SOAP contract don't propagate beyond the integration/mapper layers
- Aligns with NCBA's enterprise architecture standards

**Trade-offs**: Slightly more boilerplate than a simpler structure, but the separation pays off in testability and maintainability at scale.

---

## ADR-002: JAX-WS with Maven Stub Generation for SOAP Integration

**Status**: Accepted

**Context**: Need to consume a WSDL-based SOAP service. Options include raw HTTP/XML, Spring-WS templates, or JAX-WS code generation.

**Decision**: Use `jaxws-maven-plugin` to generate type-safe SOAP stubs at build time from the WSDL URL.

**Rationale**:
- Generated stubs provide compile-time type safety
- WSDL changes are caught at build time rather than runtime
- Eliminates manual XML parsing/construction
- JAX-WS is the Jakarta EE standard and has first-class JDK support
- Generated code lives in `target/generated-sources`, keeping the source tree clean

**Trade-offs**: Build depends on WSDL endpoint availability. For CI, the WSDL could be committed as a local file.

---

## ADR-003: Resilience4j for Fault Tolerance

**Status**: Accepted

**Context**: The external SOAP service is a third-party dependency with no SLA guarantee. The assessment explicitly requires retries, timeouts, circuit breakers, and fallbacks.

**Decision**: Use Resilience4j with circuit breaker (count-based, 50% failure threshold, 30s open duration) and retry (3 attempts, exponential backoff).

**Rationale**:
- **Circuit Breaker**: Prevents cascade failures when the SOAP service is down. After 5+ calls with >50% failure rate, the circuit opens and immediately rejects calls for 30s, allowing the upstream to recover.
- **Retry**: Handles transient network failures (timeouts, connection resets) with exponential backoff to avoid thundering herd.
- **Timeout**: SOAP client configured with 5s connect / 10s read timeouts to prevent thread blocking.
- Resilience4j is lightweight, annotation-driven, and integrates with Spring Boot Actuator for monitoring.

**Trade-offs**: Adds complexity. For a service with a single reliable upstream, this might be over-engineered. But in banking production environments, upstream reliability cannot be assumed.

---

## ADR-004: Caffeine In-Process Cache

**Status**: Accepted

**Context**: Country information (capitals, ISO codes, currencies) is semi-static. The SOAP service is an external call adding ~200-500ms latency per request.

**Decision**: Cache SOAP responses using Caffeine with 24-hour TTL and max 500 entries.

**Rationale**:
- Country data changes extremely rarely (maybe a capital moves once per decade)
- Eliminates redundant SOAP calls for the same country
- Caffeine is the fastest JVM cache implementation (near-zero overhead)
- Cache stats exposed via Actuator for monitoring hit rates
- No external infrastructure required (unlike Redis)

**Trade-offs**: In a multi-instance deployment, each pod has its own cache (no shared invalidation). Acceptable for this use case since data staleness risk is negligible.

---

## ADR-005: Correlation ID for Distributed Tracing

**Status**: Accepted

**Context**: The assessment requires structured logging and monitoring capabilities.

**Decision**: Implement a servlet filter that generates a UUID correlation ID per request, propagated via SLF4J MDC, and returned in the `X-Correlation-ID` response header.

**Rationale**:
- Every log line includes the correlation ID, enabling end-to-end request tracing
- Clients can pass their own correlation ID via the request header
- Foundation for distributed tracing when integrating with Jaeger/Zipkin
- Zero additional infrastructure required

---

## ADR-006: Kubernetes Deployment Strategy

**Status**: Accepted

**Context**: The assessment requires K8s deployment with production-grade configuration.

**Decision**: Use RollingUpdate strategy with `maxSurge: 1, maxUnavailable: 0`, startup/liveness/readiness probes, and HPA with CPU/memory metrics.

**Rationale**:
- **Zero-downtime deployments**: `maxUnavailable: 0` ensures at least 2 pods are always serving traffic during updates
- **Startup Probe**: Allows JVM warmup without triggering liveness kills
- **Readiness Probe**: Removes unhealthy pods from service load balancer
- **Liveness Probe**: Restarts pods stuck in deadlock
- **HPA**: Auto-scales between 2-10 replicas based on CPU (70%) and memory (80%) thresholds
- **preStop hook**: 10s sleep before SIGTERM allows in-flight requests to complete
- **terminationGracePeriodSeconds**: 45s matches Spring's graceful shutdown timeout

---

## ADR-007: Database Schema Design

**Status**: Accepted

**Context**: Need to persist country information with associated languages in a relational model.

**Decision**: Two tables with a one-to-many relationship. `country_info` has unique indexes on `country_name` and `iso_code`. Cascade ALL with orphan removal on the languages collection.

**Rationale**:
- Unique constraints prevent duplicate country entries at the database level
- `CascadeType.ALL` simplifies persistence - saving a country automatically saves its languages
- `orphanRemoval = true` ensures language records are cleaned up when a country is deleted
- `FetchType.LAZY` on languages with explicit `JOIN FETCH` queries prevents N+1 issues
- `@CreationTimestamp` and `@UpdateTimestamp` provide audit trail without manual management

---

## ADR-008: Multi-Stage Docker Build

**Status**: Accepted

**Context**: Need a production-ready Docker image.

**Decision**: Two-stage build: JDK 21 for compilation, JRE 21 for runtime. Non-root user. Container-aware JVM flags.

**Rationale**:
- Build stage uses full JDK; runtime stage uses JRE-only (~150MB smaller)
- Non-root user (`appuser`) follows security best practices
- `-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0` ensures JVM respects container memory limits
- Dependency caching in Docker layer speeds up rebuilds
- `HEALTHCHECK` instruction enables Docker-native health monitoring
