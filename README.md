# Country Info Service

Enterprise-grade Spring Boot microservice that aggregates country information from external SOAP services and exposes it through a RESTful API with full CRUD operations, backed by MySQL persistence.

Built for the **NCBA Backend Engineer Assessment**.

---

## Architecture Overview

```
┌─────────────┐     ┌──────────────────────────────────────────────────────┐
│   Client     │────▶│                  REST API Layer                      │
│  (Postman/   │     │         CountryInfoController (/api/v1/countries)    │
│   curl)      │◀────│                                                      │
└─────────────┘     └───────────────────────┬──────────────────────────────┘
                                            │
                    ┌───────────────────────▼──────────────────────────────┐
                    │                  Service Layer                        │
                    │              CountryInfoService                       │
                    │  • Name normalization  • Duplicate detection          │
                    │  • SOAP orchestration  • CRUD operations              │
                    └──────┬────────────────────────────┬──────────────────┘
                           │                            │
          ┌────────────────▼─────────┐    ┌────────────▼──────────────────┐
          │   SOAP Integration       │    │    Persistence Layer           │
          │  CountryInfoSoapClient   │    │   CountryInfoRepository        │
          │  • Circuit Breaker       │    │   • JPA/Hibernate              │
          │  • Retry with backoff    │    │   • Eager fetch joins          │
          │  • Response caching      │    │   • Cascade operations         │
          └──────────┬───────────────┘    └────────────┬──────────────────┘
                     │                                  │
          ┌──────────▼───────────────┐    ┌────────────▼──────────────────┐
          │  CountryInfo SOAP WSDL   │    │         MySQL 8.0              │
          │  (oorsprong.org)         │    │   country_info + language      │
          └──────────────────────────┘    └───────────────────────────────┘
```

## Sequence Flow

```
Client ──POST {"name":"kenya"}──▶ Controller
                                      │
                                      ▼
                              Normalize to "Kenya"
                                      │
                                      ▼
                              Check DB for duplicate
                                      │ (not found)
                                      ▼
                         SOAP: CountryISOCode("Kenya") → "KE"
                                      │
                                      ▼
                         SOAP: FullCountryInfo("KE") → {name, capital, ...}
                                      │
                                      ▼
                              Map to Entity + Languages
                                      │
                                      ▼
                              Persist to MySQL
                                      │
                                      ▼
Client ◀──201 {id, countryName, ...}──┘
```

## Tech Stack

| Component           | Technology                           |
|---------------------|--------------------------------------|
| Language            | Java 21 (LTS)                        |
| Framework           | Spring Boot 3.4.1                    |
| Build Tool          | Maven                                |
| Database            | MySQL 8.0                            |
| ORM                 | Spring Data JPA / Hibernate          |
| SOAP Client         | JAX-WS (jaxws-maven-plugin)         |
| Resilience          | Resilience4j (Circuit Breaker, Retry)|
| Caching             | Caffeine (in-process)                |
| Monitoring          | Spring Actuator + Micrometer + Prometheus |
| Containerization    | Docker (multi-stage)                 |
| Orchestration       | Kubernetes                           |
| Testing             | JUnit 5 + Mockito + MockMvc          |

## API Endpoints

| Method | Endpoint                 | Description                        |
|--------|--------------------------|------------------------------------|
| POST   | `/api/v1/countries`      | Lookup country via SOAP & persist  |
| GET    | `/api/v1/countries`      | Fetch all persisted countries      |
| GET    | `/api/v1/countries/{id}` | Fetch country by ID                |
| PUT    | `/api/v1/countries/{id}` | Update country record              |
| DELETE | `/api/v1/countries/{id}` | Delete country record              |

### Sample POST Request
```bash
curl -X POST http://localhost:8080/api/v1/countries \
  -H "Content-Type: application/json" \
  -d '{"name": "kenya"}'
```

### Sample Response
```json
{
  "id": 1,
  "countryName": "Kenya",
  "isoCode": "KE",
  "capitalCity": "Nairobi",
  "phoneCode": "254",
  "currencyISOCode": "KES",
  "countryFlagUrl": "http://www.oorsprong.org/WebSamples.CountryInfo/Flags/Kenya.jpg",
  "continentCode": "AF",
  "languages": [
    {
      "id": 1,
      "languageName": "Swahili",
      "languageCode": "swa"
    }
  ],
  "createdAt": "2024-01-15T10:30:00",
  "updatedAt": "2024-01-15T10:30:00"
}
```

## Quick Start

### Prerequisites
- Java 21+
- Maven 3.9+
- MySQL 8.0+ (or use Docker)
- Docker & Docker Compose (for containerized deployment)

### Local Development

1. **Start MySQL** (skip if using Docker Compose):
```bash
mysql -u root -p -e "CREATE DATABASE country_info_db;"
```

2. **Build the project** (generates SOAP stubs from WSDL):
```bash
./mvnw clean package -DskipTests
```

3. **Run the application**:
```bash
./mvnw spring-boot:run
```

4. **Verify**:
```bash
curl http://localhost:8080/actuator/health
```

### Docker Compose (Recommended)

```bash
docker-compose up --build -d
```

This starts both MySQL and the application. The app waits for MySQL to be healthy before starting.

### Run Tests

```bash
./mvnw test
```

## Design Decisions

### Why Caffeine Cache?
Country data (capitals, ISO codes, phone codes) is semi-static. Caching SOAP responses in-process with a 24-hour TTL eliminates redundant external calls, reducing P99 latency from ~500ms to <5ms for repeated lookups.

### Why Circuit Breaker + Retry?
The external SOAP service is a third-party dependency outside our control. Circuit breaker prevents cascade failures during outages, while exponential backoff retries handle transient network issues. This is critical in banking environments where upstream SLA is not guaranteed.

### Why Sentence Case Normalization?
The SOAP service is case-sensitive for country name lookups. Normalizing input to sentence case ("kenya" -> "Kenya") ensures consistent resolution and prevents duplicate database entries for the same country with different casing.

### Why Eager Fetch with Join Queries?
Languages are always returned with country info. Using `@Query` with `LEFT JOIN FETCH` prevents N+1 query issues that would degrade performance under load.

### Why Correlation ID Filter?
In a microservices architecture, distributed tracing is essential for debugging production issues. The correlation ID propagates through MDC and appears in every log line, enabling end-to-end request tracing across services.

## Monitoring

- **Health**: `GET /actuator/health`
- **Metrics**: `GET /actuator/metrics`
- **Prometheus**: `GET /actuator/prometheus`
- **Cache Stats**: `GET /actuator/caches`

## Security Recommendations

For production deployment, add:
1. **API Gateway** (Kong/AWS ALB) for rate limiting and TLS termination
2. **OAuth2/JWT** authentication via Spring Security
3. **Secrets Management** via HashiCorp Vault or K8s External Secrets Operator
4. **Network Policies** to restrict pod-to-pod communication
5. **RBAC** for Kubernetes service accounts

## Project Structure

```
src/main/java/com/ncba/countryinfo/
├── CountryInfoApplication.java          # Application entry point
├── config/
│   ├── CacheConfig.java                 # Caffeine cache configuration
│   ├── CorrelationIdFilter.java         # Request tracing filter
│   └── SoapClientConfig.java            # JAX-WS SOAP client setup
├── controller/
│   └── CountryInfoController.java       # REST API endpoints
├── dto/
│   ├── ApiError.java                    # Standardized error response
│   ├── CountryRequest.java              # POST request body
│   ├── CountryResponse.java             # API response
│   ├── CountryUpdateRequest.java        # PUT request body
│   └── LanguageDto.java                 # Language sub-resource
├── entity/
│   ├── CountryInfo.java                 # JPA entity (parent)
│   └── Language.java                    # JPA entity (child)
├── exception/
│   ├── CountryNotFoundException.java    # 404 exception
│   ├── DuplicateCountryException.java   # 409 exception
│   └── GlobalExceptionHandler.java      # Centralized error handling
├── integration/
│   ├── CountryInfoSoapClient.java       # SOAP client with resilience
│   ├── CountrySoapLookupException.java  # SOAP failure exception
│   └── generated/                       # Auto-generated SOAP stubs
├── mapper/
│   └── CountryInfoMapper.java           # Entity <-> DTO mapping
├── repository/
│   └── CountryInfoRepository.java       # Spring Data JPA repository
├── service/
│   └── CountryInfoService.java          # Business logic orchestration
└── util/
    └── StringUtils.java                 # Name normalization utilities
```
