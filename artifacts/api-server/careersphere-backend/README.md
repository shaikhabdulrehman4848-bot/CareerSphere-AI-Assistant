# CareerSphere API

CareerSphere is a Java 17 / Spring Boot REST API for resume assessment, job-role matching, interview practice, and a personalized study roadmap. The web client is a native JavaScript ES-module SPA in `artifacts/careersphere`.

## Run locally

From the workspace root:

```bash
pnpm --filter @workspace/api-server run dev
```

The development profile uses a persistent H2 database in `data/careersphere`, seeds the job-role/skill catalog, and runs with the browser cookie marked `Secure=false` for local HTTP development. Set `SESSION_SECRET` or `JWT_SECRET` in the Replit Secrets panel; the API refuses to start without a signing secret. Do not commit secrets or put them in the web client.

The Spring servlet context is `/api`. The SPA calls the API on the same origin and keeps the signed JWT in the `HttpOnly` `CAREERSPHERE_SESSION` cookie. Browser write requests use Spring Security's CSRF cookie/header protection. Bearer JWTs are also accepted by the REST API for non-browser clients.

## Optional live AI

Set `GEMINI_API_KEY` in the server's environment to enable Gemini-backed resume assessments, section improvements, interview questions, and answer feedback. If it is absent, CareerSphere uses built-in assessments and question/evaluation templates labeled **DEMO**; these results are not employer ATS results or a substitute for professional feedback. AI errors and rate limits fall back to clearly marked demo output.

## MySQL production profile

Provide a reachable MySQL 8 database and configure these server-side environment variables:

```text
SPRING_PROFILES_ACTIVE=production
MYSQL_URL=jdbc:mysql://db-host:3306/careersphere?useSSL=true&serverTimezone=UTC
MYSQL_USERNAME=...
MYSQL_PASSWORD=...
JWT_SECRET=...
```

The production profile applies the versioned schema in `src/main/resources/db/migration` with Flyway and uses Hibernate schema validation (`ddl-auto=validate`); it does not silently create or mutate production tables. Use Replit Secrets for passwords and signing keys. The included local H2 database is for development, not production data.

## API areas

- `/api/auth/*` — registration, login, logout, CSRF token, and password change
- `/api/users/me` — authenticated profile and skill management
- `/api/dashboard` — account-level resume and interview summary
- `/api/resumes/*` — PDF upload, stored assessment, and resume-section improvement
- `/api/skills`, `/api/job-roles`, `/api/job-matches` — role requirements and deterministic skills comparison
- `/api/interviews/*`, `/api/analytics` — sequential interview sessions, evaluations, history, and trends
- `/api/study-plan` — persisted roadmap and topic completion

Controllers validate request DTOs and delegate to service classes. JPA entities stay behind the API boundary, user-owned records are scoped by the authenticated account, and API errors return concise JSON without stack traces.
