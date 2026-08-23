# FinInsight

**AI-powered personal finance management platform** built with Spring Boot, Java 21, PostgreSQL, Redis, Apache Kafka, and React.

FinInsight is a production-grade modular monolith that combines multi-dimensional financial analytics, event-driven architecture, distributed caching, and an AI-powered financial insights layer with full-stack React frontend.

---

## Features

| Feature | Description |
| --- | --- |
| **Authentication** | Stateless JWT access tokens, HttpOnly refresh token cookie rotation, pessimistic locking |
| **Transactions** | Income/expense CRUD with dynamic filtering, date-range queries, and pagination |
| **Categories** | System-default and user-defined custom categories with user isolation |
| **Budgets** | Monthly category budget limits with real-time threshold status tracking |
| **Dashboard** | Composite overview: KPIs, monthly cash flow, category breakdown, recent transactions |
| **Analytics** | Financial summary, spending by category, monthly trends, budget overview, top category |
| **AI Financial Insights** | OpenAI-compatible provider abstraction, deterministic fallback engine, Redis rate limiting |
| **Redis Caching** | User-isolated cache keys, TTL configuration, graceful degradation on Redis failure |
| **Kafka Event-Driven** | Transactional Outbox Pattern, idempotent consumers, Dead Letter Topics |
| **OpenAPI / Swagger** | JWT-authenticated Swagger UI at `/swagger-ui/index.html` |
| **Observability** | Spring Boot Actuator liveness/readiness probes, SLF4J MDC correlation ID tracing |
| **Docker** | Multi-stage Dockerfile, Docker Compose full stack including Nginx-served frontend |
| **CI/CD** | GitHub Actions pipeline (Java 21 + Node 20, backend tests + frontend tests + build) |

---

## Architecture

```text
┌─────────────────────────────────────────────────────────────┐
│                     React Frontend (Vite)                   │
│         TypeScript · Axios · React Context · Vitest         │
└──────────────────────────┬──────────────────────────────────┘
                           │ HTTP / JWT Bearer
┌──────────────────────────▼──────────────────────────────────┐
│              Spring Boot REST API (Port 8080)               │
│                   Modular Monolith — Java 21                │
│  ┌────────┐ ┌─────────────┐ ┌────────┐ ┌────────────────┐  │
│  │  Auth  │ │Transactions │ │Budgets │ │  Categories    │  │
│  └────────┘ └─────────────┘ └────────┘ └────────────────┘  │
│  ┌───────────┐ ┌───────────┐ ┌────────────────────────────┐ │
│  │ Dashboard │ │ Analytics │ │      AI Insights           │ │
│  └───────────┘ └───────────┘ └────────────────────────────┘ │
│  ┌──────────────────────────────────────────────────────┐   │
│  │  common: Cache · Events · Outbox · Exceptions · DTOs │   │
│  └──────────────────────────────────────────────────────┘   │
└─────┬────────────┬──────────────┬───────────────────────────┘
      │            │              │
      ▼            ▼              ▼
 PostgreSQL      Redis 7      Apache Kafka
 (JPA/Flyway)  (Spring Cache) (Outbox → Topics → Consumers)
```

### Event-Driven Flow

```text
Business Mutation (Transaction / Budget / Category)
      ↓
Transactional Outbox (written atomically in same DB transaction)
      ↓
OutboxScheduler (polls every 1 second, batch size: 50)
      ↓
KafkaEventPublisher → Kafka Topic
      ↓
Idempotent Consumer (deduplication via ProcessedEvent table)
      ↓
Cache Invalidation (user-scoped Redis SCAN + delete)
```

### AI Insights Flow

```text
GET /api/v1/ai/insights?from=&to= (JWT Authenticated)
      ↓
AiRateLimiter (Redis sliding-window per user — 429 if exceeded)
      ↓
@Cacheable (ai:insights::{userId}:{from}:{to})
      ↓ cache miss
FinancialContextBuilder (aggregates BigDecimal financials, sanitizes category names — zero PII)
      ↓
AiProviderClient → OpenAI-compatible HTTP (timeout + retry)
      ↓ provider failure
DeterministicInsightEngine (rule-based fallback, fallback=true)
      ↓
AiInsightResponse (summary, insights, recommendations, legal disclaimer)
```

---

## Technology Stack

| Layer | Technology |
| --- | --- |
| Language | Java 21 |
| Framework | Spring Boot 4.1.1 |
| Security | Spring Security 7, JJWT 0.12.6 |
| Database | PostgreSQL, Spring Data JPA, Hibernate, Flyway |
| Cache | Redis 7, Spring Cache (Lettuce), user-isolated SCAN eviction |
| Messaging | Apache Kafka (KRaft), Spring Kafka, Transactional Outbox |
| AI | OpenAI-compatible REST client, Deterministic Rule Engine fallback |
| API Docs | SpringDoc OpenAPI 2.8.5 (Swagger UI) |
| Observability | Spring Boot Actuator, SLF4J MDC (X-Correlation-ID) |
| Containerization | Docker (multi-stage), Docker Compose, Nginx |
| Frontend | React 18, TypeScript, Vite 6, Axios, React Router |
| Testing (BE) | JUnit 5, Mockito, Testcontainers, Spring Boot Test |
| Testing (FE) | Vitest, React Testing Library, jsdom |
| CI | GitHub Actions (Java 21 Corretto + Node.js 20) |

---

## Prerequisites

| Requirement | Version |
| --- | --- |
| Java | 21+ |
| Maven | 3.9+ (wrapper included: `mvnw.cmd`) |
| Node.js | 20+ |
| PostgreSQL | 15+ |
| Redis | 7+ |
| Apache Kafka | 3+ (KRaft mode supported) |
| Docker | Recommended for full-stack local setup |

---

## Environment Configuration

Copy the example environment file and fill in your values:

```bash
cp .env.example .env
```

> **Important:** Never commit `.env` to version control. The `.env.example` file contains only placeholder values — no real secrets.

Key environment variables:

| Variable | Description | Production Required |
| --- | --- | :---: |
| `POSTGRES_PASSWORD` | Database password | ✅ |
| `JWT_SECRET` | HMAC-SHA256 secret (≥ 32 chars) | ✅ |
| `REDIS_PASSWORD` | Redis authentication password | ✅ |
| `AI_API_KEY` | OpenAI-compatible provider API key | If AI enabled |
| `JWT_COOKIE_SECURE` | Set to `true` in HTTPS production | ✅ |

Full variable reference is documented in the `.env.example` file.

---

## Running the Backend

```powershell
# Windows — start infrastructure via Docker, then run backend locally
docker compose up -d postgres redis kafka

# Set JAVA_HOME if needed
$env:JAVA_HOME = "C:\Path\To\jdk-21"

# Run with dev profile
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev
```

```bash
# Linux / macOS
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

---

## Running the Frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend starts at **<http://localhost:3000>** with an automatic proxy to the Spring Boot backend at `http://localhost:8080`.

---

## Docker (Full Stack)

```bash
# Start the complete stack (PostgreSQL, Redis, Kafka, Backend, Frontend/Nginx)
docker compose up -d --build

# Or start only infrastructure services for local development
docker compose up -d postgres redis kafka
```

> **Note:** Docker CLI must be installed and running. If unavailable, start services manually per the prerequisites above.

---

## API Documentation

| URL | Description |
| --- | --- |
| `http://localhost:8080/swagger-ui/index.html` | Swagger UI (interactive API explorer) |
| `http://localhost:8080/v3/api-docs` | OpenAPI 3.0 JSON specification |
| `http://localhost:8080/actuator/health` | Application health status |
| `http://localhost:8080/actuator/health/liveness` | Kubernetes liveness probe |
| `http://localhost:8080/actuator/health/readiness` | Kubernetes readiness probe |
| `http://localhost:3000` | React Frontend |

### JWT Authentication in Swagger UI

1. Open `http://localhost:8080/swagger-ui/index.html`
2. Call `POST /api/v1/auth/login` to obtain an `accessToken`
3. Click **Authorize** (🔒) and enter: `Bearer <your-access-token>`
4. All authenticated endpoints are now accessible

---

## REST API Endpoints

| Method | Path | Auth | Description |
| --- | --- | :---: | --- |
| POST | `/api/v1/auth/register` | ❌ | Register new user |
| POST | `/api/v1/auth/login` | ❌ | Login and receive JWT |
| POST | `/api/v1/auth/refresh` | ❌ | Rotate refresh token (HttpOnly cookie) |
| POST | `/api/v1/auth/logout` | ❌ | Revoke session |
| GET | `/api/v1/auth/me` | ✅ | Get authenticated user profile |
| GET | `/api/v1/transactions` | ✅ | List transactions (paginated, filtered) |
| POST | `/api/v1/transactions` | ✅ | Create transaction |
| GET | `/api/v1/transactions/{id}` | ✅ | Get transaction by ID |
| PUT | `/api/v1/transactions/{id}` | ✅ | Update transaction |
| DELETE | `/api/v1/transactions/{id}` | ✅ | Delete transaction |
| GET | `/api/v1/categories` | ✅ | List categories |
| POST | `/api/v1/categories` | ✅ | Create custom category |
| PUT | `/api/v1/categories/{id}` | ✅ | Update custom category |
| DELETE | `/api/v1/categories/{id}` | ✅ | Delete custom category |
| GET | `/api/v1/budgets` | ✅ | List budgets (paginated) |
| POST | `/api/v1/budgets` | ✅ | Create budget |
| PUT | `/api/v1/budgets/{id}` | ✅ | Update budget |
| DELETE | `/api/v1/budgets/{id}` | ✅ | Delete budget |
| GET | `/api/v1/dashboard` | ✅ | Composite dashboard |
| GET | `/api/v1/analytics/summary` | ✅ | Financial summary |
| GET | `/api/v1/analytics/spending-by-category` | ✅ | Category spending breakdown |
| GET | `/api/v1/analytics/monthly-summary` | ✅ | Monthly income/expense trend |
| GET | `/api/v1/analytics/budget-overview` | ✅ | Budget utilization overview |
| GET | `/api/v1/analytics/top-category` | ✅ | Top spending category |
| GET | `/api/v1/ai/insights` | ✅ | AI-powered financial insights |

---

## Testing

### Backend

```powershell
# Windows — unit tests only (no Docker required)
$env:JAVA_HOME = "C:\Users\Emrah\.jdks\corretto-21.0.11"
.\mvnw.cmd test "-Dtest=*ServiceTest*,*Event*,*Jwt*,*Handler*,*Filter*,*ConfigTest*,*Ai*,*Context*,*Engine*,*Limiter*,*Client*,*OpenApi*,*Cache*,*Production*"

# Full suite including integration tests (requires Docker + Testcontainers)
.\mvnw.cmd test
```

### Frontend

```bash
cd frontend
npm run test          # Vitest unit, integration, concurrency & E2E suite
npx tsc --noEmit      # TypeScript strict type check (0 errors expected)
npm run build         # Vite production bundle build
```

### Expected Quality Gate Results

| Suite | Expected |
| --- | --- |
| Backend unit tests | 118 / 118 PASS |
| Backend integration tests | Requires Docker + Testcontainers |
| Frontend Vitest tests | 24 / 24 PASS |
| TypeScript strict check | 0 errors |
| Vite production build | SUCCESS |
| Compiler warnings | 0 |
| Hardcoded secrets | 0 |

---

## Project Structure

```text
FinInsight/
├── src/
│   ├── main/java/com/fininsight/
│   │   ├── FinInsightApplication.java
│   │   ├── ai/               # AI Insights layer
│   │   │   ├── client/       # AiProviderClient, OpenAiCompatibleClient, MockAiClient
│   │   │   ├── config/       # AiConfig (provider wiring, RestClient timeouts)
│   │   │   ├── controller/   # AiInsightController
│   │   │   ├── dto/          # AiInsightResponse, AiInsightItem, FinancialContext
│   │   │   └── service/      # AiInsightService, FinancialContextBuilder,
│   │   │                     # DeterministicInsightEngine, AiRateLimiter
│   │   ├── analytics/        # Financial reporting (Cached)
│   │   ├── auth/             # JWT, refresh tokens, user session management
│   │   ├── budget/           # Budget CRUD, threshold tracking
│   │   ├── category/         # System + custom category management
│   │   ├── common/
│   │   │   ├── cache/        # CacheEvictionService (user-scoped Redis SCAN)
│   │   │   ├── dto/          # ApiResponse, ErrorResponse, PagedResponse
│   │   │   ├── event/        # Domain events, Kafka consumers, Transactional Outbox
│   │   │   └── exception/    # GlobalExceptionHandler, BusinessException
│   │   ├── config/           # SecurityConfig, RedisCacheConfig, OpenApiConfig,
│   │   │                     # CacheNames, CorrelationIdFilter, KafkaConfig
│   │   ├── dashboard/        # Composite dashboard (Cached)
│   │   ├── transaction/      # Transaction CRUD, Specification filtering
│   │   └── user/             # User entity, repository
│   ├── main/resources/
│   │   ├── application.yml
│   │   └── db/migration/     # Flyway SQL migrations
│   └── test/java/com/fininsight/
│       └── ...               # Unit tests (service, event, config, AI, cache)
│                             # Integration tests (require Docker/Testcontainers)
├── frontend/
│   ├── src/
│   │   ├── api/              # axiosClient (JWT interceptor, refresh queue), domain APIs
│   │   ├── components/       # Reusable UI components (charts, forms, feedback, common)
│   │   ├── context/          # AuthContext (JWT state management)
│   │   ├── hooks/            # useDateRange
│   │   ├── pages/            # DashboardPage, TransactionsPage, BudgetsPage, ...
│   │   ├── routes/           # ProtectedRoute, router configuration
│   │   ├── types/            # TypeScript interfaces aligned with backend DTOs
│   │   └── utils/            # currencyFormatter, dateFormatter, errorExtractor
│   ├── Dockerfile            # Multi-stage build (Node builder + Nginx runtime)
│   └── nginx.conf            # SPA routing, reverse proxy /api/, gzip, security headers
├── .github/workflows/ci.yml  # GitHub Actions CI pipeline
├── docker-compose.yml        # Full stack (PostgreSQL, Redis, Kafka, Backend, Frontend)
├── Dockerfile                # Backend multi-stage build (JDK builder + JRE Alpine runtime)
├── .env.example              # Environment variable template
└── pom.xml
```

---

## Security

- **JWT Authentication:** Stateless RS/HS-256 access tokens (15-minute TTL), rotating HttpOnly refresh tokens (7-day TTL)
- **User Isolation:** Every data query includes `userId` from `@AuthenticationPrincipal` — never from client request parameters (IDOR/BOLA protection)
- **Cache Isolation:** All Redis cache keys include authenticated `userId` (e.g., `analytics:summary::{userId}:{from}:{to}`)
- **Zero PII in AI Context:** `FinancialContextBuilder` transmits only aggregated BigDecimal financials — no userId, email, JWT, passwords, raw transaction descriptions, or database IDs
- **Prompt Injection Protection:** User-defined category names are sanitized against injection patterns before entering AI context
- **Rate Limiting:** Redis sliding-window per-user rate limit on AI endpoint (default: 10 requests/minute, returns HTTP 429)
- **Security Headers:** `X-Frame-Options: DENY`, `HSTS`, `Content-Type-Options`
- **Secret Management:** All secrets sourced from environment variables — no hard-coded credentials in source code
- **Frontend XSS:** AI output rendered as safe text nodes — no `dangerouslySetInnerHTML` or `eval()` usage
- **Correlation ID:** All requests tracked with `X-Correlation-ID` propagated through MDC → logs → error responses

---

## AI Disclaimer

> The AI Financial Insights feature generates insights from your aggregated financial activity for **informational purposes only**. These insights do not constitute financial, investment, tax, or legal advice. Always consult a qualified financial professional before making financial decisions.

---

## CI/CD

The repository includes a GitHub Actions workflow (`.github/workflows/ci.yml`) that runs on every push and pull request to `main`:

1. **Backend Build & Test** — Java 21 (Amazon Corretto), Maven, unit test suite
2. **Frontend Test** — Node.js 20, `npm ci`, Vitest test suite
3. **TypeScript Check** — `tsc --noEmit` (strict mode)
4. **Frontend Build** — Vite production bundle

> **Note:** Integration tests requiring Docker/Testcontainers (PostgreSQL, Kafka) run only when Docker is available in the CI environment.

---

## Production Considerations

Before deploying to production:

- [ ] Set a strong, unique `JWT_SECRET` (≥ 32 characters, randomly generated)
- [ ] Set `JWT_COOKIE_SECURE=true` (requires HTTPS)
- [ ] Configure `POSTGRES_PASSWORD` with a strong credential
- [ ] Configure `REDIS_PASSWORD` with a strong credential
- [ ] Set `AI_API_KEY` if AI insights are enabled
- [ ] Configure HTTPS termination (Nginx, load balancer, or reverse proxy)
- [ ] Restrict CORS `allowedOrigins` to your actual production domain in `SecurityConfig.java`
- [ ] Enable Redis authentication and TLS in production
- [ ] Configure Kafka with authentication and TLS in production
- [ ] Set up monitoring (Prometheus, Grafana, or cloud-native equivalent)
- [ ] Configure log aggregation (ELK, Loki, or cloud-native equivalent)
- [ ] Configure database backups and point-in-time recovery
- [ ] Enable outbox cleanup cron (`OUTBOX_CLEANUP_CRON`) for long-running deployments
- [ ] Rotate JWT secrets and AI API keys periodically

---

## License

This project is for educational and portfolio demonstration purposes.
