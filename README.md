# FixFlow AI

An AI-assisted troubleshooting tool for Java / Spring Boot errors. Paste an error message or upload a `.log` / `.txt` file, and FixFlow masks likely secrets, parses the stack trace, searches a built-in knowledge base, and produces a structured diagnosis with evidence, ordered debugging steps, and verification steps. It works without an AI key (rule-based mode) and can use any OpenAI-compatible LLM API when configured.

> **Local single-user demo.** There is no authentication. Do not expose it publicly without adding access controls and rate limiting.

## How it works

1. **Validate** the input (size, type, binary check, empty check) and normalize line endings.
2. **Redact** likely secrets (passwords, tokens, API keys, credentials in URLs, private keys) **before** anything is parsed, sent to an AI provider, or stored.
3. **Parse** the sanitized text: exception class, message, cause chain, stack frames, known error patterns.
4. **Search** the built-in knowledge base (14 guides; PostgreSQL full-text search plus signature matching).
5. **Diagnose**: if an AI provider is configured, the model receives only sanitized evidence and guide excerpts, delimited as untrusted data. Its JSON reply is validated; one repair retry is allowed. Otherwise, or on any AI failure, a deterministic rule-based diagnosis is returned and clearly labelled.
6. **Save** the sanitized input and the validated result to PostgreSQL. Users can view history, give feedback, and delete records.

**What the LLM does:** turns sanitized evidence plus retrieved guides into a structured diagnosis proposal.
**What deterministic Java does:** validation, redaction, parsing, retrieval, output validation (allowed values, real article IDs, evidence grounded in the log, no "I fixed it" claims), fallback, and persistence.
**What FixFlow never does:** run commands, change code, or claim that anything was fixed.

## Tech stack

Java 21, Spring Boot 3.5, Maven, Spring Data JPA, Flyway, PostgreSQL 16, Springdoc OpenAPI, React 18 + Vite, JUnit 5, Mockito, Testcontainers, Docker Compose.

## Quick start

Prerequisites: JDK 21, Node.js 20+, Docker Desktop.

### Option A: local development (backend on port 8081, frontend on 5173)

```bash
# 1. Start PostgreSQL (data is kept in a Docker volume)
docker compose up -d db

# 2. Start the backend (Windows: mvnw.cmd spring-boot:run)
cd Fixflow-backend
./mvnw spring-boot:run

# 3. Start the frontend (new terminal)
cd Fixflow-frontend
npm install
npm run dev
```

Open the address Vite prints (normally http://localhost:5173). Swagger UI: http://localhost:8081/swagger-ui/index.html

### Option B: everything in Docker (backend on 8080, app on 5173)

```bash
copy .env.example .env      # Windows (use cp on macOS/Linux)
docker compose up -d --build
```

Open http://localhost:5173. Swagger UI: http://localhost:8080/swagger-ui/index.html. Stop with `docker compose down` (add `-v` only if you want to delete the database).

## Configuration

`.env` is read by Docker Compose only. For local runs, set variables in your IDE run configuration or shell; the defaults match `docker compose up -d db`.

| Variable | Default | Purpose |
|---|---|---|
| `POSTGRES_DB` / `POSTGRES_USER` / `POSTGRES_PASSWORD` | `fixflow` / `fixflow_user` / `change_me_locally` | Database created by Docker Compose |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5433/fixflow` | JDBC URL (local runs) |
| `SPRING_DATASOURCE_USERNAME` / `SPRING_DATASOURCE_PASSWORD` | as above | Database credentials |
| `SERVER_PORT` | `8081` (Docker sets 8080) | Backend port |
| `AI_PROVIDER` | `disabled` | `disabled` or `openai-compatible` |
| `AI_BASE_URL` | empty | For example `https://api.openai.com/v1` |
| `AI_MODEL` | empty | Model name from your provider |
| `AI_API_KEY` | empty | Provider key. Backend only, never sent to the browser |
| `AI_TIMEOUT_SECONDS` | `30` | AI request timeout |
| `AI_MAX_OUTPUT_TOKENS` | `1500` | AI output limit |
| `APP_MAX_UPLOAD_MB` | `1` | Maximum pasted/uploaded size (also change `MAX_MB` in `FileUpload.jsx`) |
| `APP_CORS_ALLOWED_ORIGINS` | `http://localhost:5173` | Allowed browser origins |

### Enabling AI analysis

Set `AI_PROVIDER=openai-compatible`, `AI_BASE_URL`, `AI_MODEL` and `AI_API_KEY`, then restart the backend. `GET /api/v1/health` then reports `AI_CONFIGURED`. If the key is wrong, the quota is exceeded, the call times out, or the model returns invalid output twice, FixFlow falls back to the rule-based result and labels it. It never presents a failed AI call as a success.

## API

| Method and path | Purpose |
|---|---|
| `POST /api/v1/diagnoses` | Analyze pasted text (`content`, optional `title`) and save the result |
| `POST /api/v1/diagnoses/upload` | Analyze an uploaded `.log` / `.txt` file (multipart field `file`) |
| `POST /api/v1/diagnoses/preview` and `/preview/upload` | Sanitized preview and parse result, nothing saved |
| `POST /api/v1/diagnoses/rule-based-preview` | Rule-based diagnosis, nothing saved, no AI |
| `GET /api/v1/diagnoses?page=0&size=10` | Paginated history (newest first) |
| `GET /api/v1/diagnoses/{id}` | One saved diagnosis |
| `DELETE /api/v1/diagnoses/{id}` | Delete a diagnosis |
| `POST /api/v1/diagnoses/{id}/feedback` | `{"rating": "HELPFUL" or "NOT_HELPFUL", "comment": "optional"}` |
| `GET /api/v1/knowledge?query=` | List or search knowledge articles |
| `GET /api/v1/stats` | Counts for the dashboard |
| `GET /api/v1/health` | Health without secrets |

Errors use one format: `{timestamp, status, code, message, details}`. A Postman collection is in `FixFlow-AI.postman_collection.json`, and sample logs are in `sample-logs/`.

## Tests

```bash
cd Fixflow-backend
./mvnw test          # Windows: mvnw.cmd test
```

Unit tests cover parsing, redaction, input validation, knowledge search, response validation, prompt building, the orchestrator, history and feedback. Integration tests run the real endpoints against a PostgreSQL Testcontainer (Docker required) with a stub AI provider, so no paid API calls are made. Frontend: `cd Fixflow-frontend && npm run build`.

## Security and privacy

- API keys and passwords live in environment variables; only `.env.example` with placeholders is committed.
- Secrets are masked before parsing, AI calls and storage. Detection is heuristic and imperfect, so the UI shows the sanitized text for review before analysis.
- Log content and knowledge articles are sent to the model as delimited, untrusted data, and the system prompt forbids following instructions found inside them. Model output is validated before it is shown or saved.
- Raw input, prompts and credentials are not logged. Error responses never include stack traces.
- Uploads are limited in size and type, file names are sanitized and never used as paths, and all database access goes through JPA.

## Known limitations

- No authentication: single-user demo only. Add login, ownership checks and rate limiting before any public deployment.
- Redaction is regex-based and will miss some secrets and mask some harmless text.
- Rule-based mode only recognizes the 14 built-in error signatures. Other errors are reported as insufficient evidence.
- Confidence levels are explained estimates, not calibrated probabilities.
- Focused on Java / Spring Boot errors.

## Troubleshooting

| Problem | Fix |
|---|---|
| Backend: "Connection refused" to port 5433 | Start Docker Desktop, then `docker compose up -d db` |
| Dashboard shows an error | Check that the backend is running and `vite.config.js` proxies `/api` to your backend port |
| Port already in use | Stop the other process, or change `SERVER_PORT` |
| Integration tests skipped | Docker is not running |
| Docker "client version too old" in tests | Create `~/.docker-java.properties` containing `api.version=1.44` |

## Project structure

```
Fixflow-backend/    Spring Boot API (controller, service, ai, parser, entity, repository, exception)
Fixflow-frontend/   React + Vite app
sample-logs/        Sanitized demo logs
docker-compose.yml  PostgreSQL, backend and frontend
```

## Screenshots

![Dashboard](docs/screenshots/dashboard.png)
![Sanitized preview](docs/screenshots/preview.png)
![Diagnosis](docs/screenshots/diagnosis.png)
![History](docs/screenshots/history.png)