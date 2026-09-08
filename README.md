# HealthCare E-Commerce & Smart Assistance Platform

A full-stack platform combining healthcare shopping, AI-assisted conversations, prescriptions, consultations, professional discovery, events, inventory, fulfilment, payments, and notifications in role-specific workspaces.

Built with Vue 3, Spring Boot, MySQL, fully working RabbitMQ messaging, MailHog, Docker Compose, JUnit, Postman, and Playwright.

> Educational software only—not a substitute for medical advice, diagnosis, or emergency care.

## Features by role

- **Public:** animated homepage, About, Services, FAQ, Contact, products, events, professionals, login, role-first registration, and email verification.
- **Patient:** AI chat, recommendations, patient-only cart/checkout/orders/payments, prescription upload and tracking, consultations, addresses, health profile, events, and notifications.
- **Doctor:** dashboard, dedicated profile, consultation queue and scheduling, patient-linked chat, professional listing, and event management.
- **Pharmacist:** dashboard, dedicated profile, prescription queue/search, OCR review, approval/rejection, and medication selection.
- **Vendor:** dashboard, dedicated profile, product and inventory management, order fulfilment, and shipment tracking.
- **Administrator:** dashboard, dedicated profile, users, roles, moderation, catalog, categories, events, inventory, orders, discounts, and notification logs.

Ordering is restricted to the exact `PATIENT` role; inherited role permissions cannot grant ordering to doctors, pharmacists, vendors, or administrators.

## Architecture

```text
Browser → Vue/Pinia/Router → Nginx → Spring Boot → MySQL
                                  │            └→ upload volumes
                                  └──────────────→ RabbitMQ
                                                    ├→ in-app notifications
                                                    ├→ email → SMTP/MailHog
                                                    ├→ simulated SMS logs
                                                    └→ inventory events
```

| Layer | Technology |
|---|---|
| Frontend | Vue 3, Pinia, Vue Router, Axios, Leaflet, Vite |
| Backend | Java 17, Spring Boot 4, Security, JPA, MyBatis |
| Data | MySQL 8; H2 in tests |
| Messaging | RabbitMQ topic exchange, durable queues, retries, DLQ |
| AI | OpenRouter or deterministic mock provider |
| Delivery | Docker Compose, multi-stage images, Nginx |
| Tests | JUnit, MockMvc, Mockito, Postman, Playwright |

## RabbitMQ is fully working

The application publishes real domain events through `RabbitTemplate`. Spring declares the durable exchange, queues, bindings, retry policy, JSON converter, and listeners automatically. Notification, email, SMS, and inventory consumers are enabled. Failed messages retry three times and then reach the fanout dead-letter exchange and `healthcare.dlq`, regardless of their original routing key.

| Resource | Name |
|---|---|
| Topic exchange | `healthcare.events` |
| Notification queue | `healthcare.notification.queue` |
| Inventory queue | `healthcare.inventory.queue` |
| Email queue | `healthcare.email.queue` |
| SMS queue | `healthcare.sms.queue` |
| Dead-letter exchange/queue | `healthcare.dlx` / `healthcare.dlq` |

Routing keys are `user.registered`, `order.created`, `payment.success`, `prescription.approved`, `event.registration`, and `consultation.scheduled`.

## Docker quick start

Requirements: Docker Engine/Desktop and Docker Compose v2.

Copy the supplied template and replace its placeholder values:

```bash
cp .env.example .env
```

```bash
docker compose up --build -d
docker compose ps
docker compose logs -f backend rabbitmq
```

Compose waits for MySQL and RabbitMQ, then for the backend readiness endpoint, before starting the frontend. Every service has bounded JSON logs, restart behavior, memory/CPU limits, and a health check where supported. Infrastructure and debugging ports bind to `127.0.0.1`; only the web port is network-facing.

| Service | Address |
|---|---|
| Web app | http://localhost:5173 |
| API | http://localhost:8080 |
| RabbitMQ / management | localhost:5672 / http://localhost:15672 |
| MailHog SMTP / inbox | localhost:1025 / http://localhost:8025 |
| MySQL | localhost:3306 |

MySQL, RabbitMQ, avatars, prescriptions, and logs use persistent named volumes. `docker compose down` retains them; `docker compose down --volumes` removes them.

For a deployment with mandatory secrets and port 80:

```bash
DB_PASSWORD='...' DB_ROOT_PASSWORD='...' \
RABBITMQ_DEFAULT_PASS='...' JWT_SECRET='...' \
docker compose -f docker-compose.yml -f compose.production.yaml up --build -d
```

The production overlay refuses to render when a required secret is absent. Place a TLS reverse proxy or load balancer in front of the frontend. The API, MySQL, RabbitMQ, and MailHog remain loopback-only on the host while containers communicate over Compose's private network.

Health endpoints:

- Frontend: `GET /healthz`
- Backend liveness: `GET /actuator/health/liveness`
- Backend readiness: `GET /actuator/health/readiness`

## Local development

Backend requires Java 17+, MySQL, and RabbitMQ:

```bash
cd Backend
export RABBITMQ_HOST=localhost
export RABBITMQ_DEFAULT_USER=healthcare
export RABBITMQ_DEFAULT_PASS='your-password'
./mvnw spring-boot:run
```

Frontend requires Node.js 20+:

```bash
cd Frontend
npm ci
npm run dev
```

Vite proxies `/api` and `/uploads` to port 8080. Set `VITE_API_BASE_URL` to override the API origin.

Important configuration variables include `SPRING_DATASOURCE_URL`, `DB_USER`, `DB_PASSWORD`, `JWT_SECRET`, `RABBITMQ_HOST`, `RABBITMQ_PORT`, `RABBITMQ_DEFAULT_USER`, `RABBITMQ_DEFAULT_PASS`, `MAIL_HOST`, `MAIL_PORT`, `AI_PROVIDER`, `OPENROUTER_API_KEY`, `AI_OCR_ENABLED`, `PRESCRIPTION_STORAGE_ROOT`, `AVATAR_STORAGE_ROOT`, and `LOG_PATH`.

## API surface

There are 178 live mappings covering `/api/auth`, `/api/users`, `/api/doctor`, `/api/pharmacist`, `/api/vendor`, `/api/products`, `/api/categories`, `/api/cart`, `/api/orders`, `/api/payments`, `/api/discounts`, `/api/prescriptions`, `/api/consultations`, `/api/professionals`, `/api/geocode`, `/api/events`, `/api/event-registrations`, `/api/inventory`, `/api/notifications`, `/api/admin`, `/api/chat`, and `/api/recommendations`.

## Testing

Backend—1,230 passing tests, including all 178 mappings through Spring MVC and Security plus public liveness/readiness checks:

```bash
cd Backend && ./mvnw test
```

Frontend—148 route, navigation, API-linkage, and role contracts:

```bash
cd Frontend && npm test
```

Playwright—desktop and mobile browser coverage for public navigation, login persistence, dropdowns, role profiles, patient-only checkout, and sign-out:

```bash
cd Frontend
npm run test:e2e:install
npm run test:e2e
npm run test:e2e:ui
```

With Compose running, test Nginx → backend linkage:

```bash
E2E_BASE_URL=http://localhost:5173 npm run test:e2e:live
```

Postman is at `postman/Healthcare-Platform.postman_collection.json`: 188 requests and 762 assertions, including every backend mapping. Set the role-token variables before authenticated runs. Regenerate after controller changes with:

```bash
node postman/generate-endpoint-matrix.mjs
```

## Project layout

```text
Backend/                 Spring modules, resources, tests, Dockerfile
Frontend/src/pages/      public and role-specific pages
Frontend/src/services/   API clients
Frontend/src/stores/     Pinia state
Frontend/src/styles/     centralized warm design system
Frontend/e2e/            Playwright UI and live linkage tests
postman/                 complete collection and generator
docker-compose.yml       frontend, backend, MySQL, RabbitMQ, MailHog
compose.production.yaml  fail-fast production secret/port overlay
.env.example             documented non-secret environment template
```

## Production checklist

- Copy `.env.example`; replace all development passwords and JWT secrets.
- Configure production SMTP and optional OpenRouter credentials.
- Back up MySQL, RabbitMQ, avatar, and prescription volumes.
- Put TLS termination in front of Nginx.
- Do not publicly expose database or broker ports.
- Prefer controlled database migrations over automatic schema updates.

The container plumbing is complete for local, demo, and single-host deployment. Before a multi-instance or regulated production rollout, add versioned database migrations, an external secrets manager, off-host backups, TLS, monitoring/alerts, and immutable image digests in the deployment environment.
