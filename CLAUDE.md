# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

Stargazer (`xyz.ncookie.stargazer`) is a Spring Boot 4 / Java 17 REST API backend that scores how good a given
location/time is for stargazing (0–100), combining OpenWeatherMap, VIIRS light-pollution data, and astronomical
calculations (via `commons-suncalc`), plus a Google Gemini-generated comment. It also supports member auth,
observation spots, bookmarks, and a "today's recommendation" feature.

## Commands

```bash
# Run the app locally (default port 8080)
./gradlew bootRun

# Build (skip tests, matches CI)
./gradlew clean build -x test

# Run all tests
./gradlew test

# Run a single test class
./gradlew test --tests "xyz.ncookie.stargazer.StargazerApplicationTests"

# Regenerate openapi.json from a running local instance (localhost:8080)
./gradlew generateOpenApi
```

Local infra dependencies (MySQL 8 + Redis) are started via:

```bash
docker-compose -f docker-compose.dev.yml up -d
```

Required env vars (see `.env`, loaded via `application.yml`): `MYSQL_ENDPOINT/USERNAME/PASSWORD`, `REDIS_HOST/PORT`,
`JWT_KEY`, `WEATHER_API_KEY`, `GEMINI_API_KEY`, and OAuth2 client id/secret triples for Google/Kakao/Naver
(`SERVER_BASE_URL`, `CLIENT_REDIRECT_BASE_URL`) — only needed for the web OAuth2 login flow, not the mobile app API.

## Architecture

### Layering (strictly enforced — see `docs/ARCHITECTURE_GUIDE.md`)

Each `domain/{name}/` package follows a 4-layer structure with **unidirectional** dependencies:

```
Controller → Application → Domain → Repository
```

- **Controller**: HTTP binding/validation only, no business logic. Builds a `*Command` via `Command.from(...)` and
  delegates to an Application Service.
- **Application** (`{Domain}ApplicationService`): use-case orchestration, `@Transactional` boundary. This is the
  **only** layer allowed to call another domain's `{Domain}DomainService`. Simple reads/writes with no domain rules
  may call the Repository directly instead of going through the Domain Service.
- **Domain** (`{Domain}DomainService`): single-domain business rules, entity state transitions, validation
  (e.g. `findById`, `validateOwner`). Must never reference another domain's Domain Service or call up into
  Application.
- **Repository**: plain Spring Data JPA, persistence only, no business logic.

Cross-domain communication always goes through Application-layer orchestration — never Domain → Domain.

### Domains (`src/main/java/xyz/ncookie/stargazer/domain/`)

- `stargazing/` — core scoring engine. `engine/StargazingScoringEngine` computes the 0–100 score from
  `AstronomyCalculator` output, `LightPollutionDataProvider` (Bortle class from VIIRS CSV,
  `src/main/resources/light_pollution_korea.csv`), and `client/openweather` weather data; `client/gemini` generates
  the AI comment. Score model: base 100, hard cutoff to 0 if cloud cover ≥70% or sun altitude > -6° (civil
  twilight/daytime), then deductions for light pollution, cloud, moon brightness, humidity, and visibility (see
  README "별 관측 점수 산정 기준" for exact weights).
- `spot/` — observation spot lookup by lat/lng + radius, backed by MySQL spatial indexing (Hibernate Spatial).
- `member/` — auth/account (OAuth2 + JWT), profile.
- `bookmark/` — CRUD for SPOT-type or CUSTOM-type saved locations.
- `recommend/` — "today's recommendation": top-5 scored bookmarks for the logged-in member, computed via a
  dedicated executor (`global/config/RecommendExecutorConfig`).

### Global layer (`global/`)

- `security/` — JWT (access token via `Authorization` header) + OAuth2 (Google/Kakao/Naver) + Refresh Token Rotation
  stored in Redis and set as an HttpOnly cookie. See `docs/SECURITY_AUTH_GUIDE.md` for the full flow, error codes,
  and client integration requirements (mobile clients must configure a cookie jar or RT will be lost). Auth services
  are decoupled from HTTP Request/Response objects.
- `exception/` — `BaseException` + per-domain `{Domain}ErrorCode` enums implementing `ErrorCode`
  (status + message), handled centrally by `GlobalExceptionHandler`.
- `advice/ResponseBodyWrapper` — a `ResponseBodyAdvice` that wraps every `@RestController` JSON response (except
  `/v3/api-docs` and `/swagger-ui`) in a common `CommonResponse` envelope (`success`, `status`, `message`, `data`);
  skip wrapping by returning a `CommonResponse` directly. Success messages come from the `@ResponseMessage`
  annotation on the controller method, defaulting to a generic Korean success message if absent.
- `cache/`, `config/` — Caffeine + Redis cache config, Bucket4j rate limiting for the OpenWeather client
  (`OpenWeatherRateLimitConfig`), RestTemplate, Swagger/OpenAPI setup.
- `infra/lightpollution` — light pollution data loading infrastructure.

### API conventions

- Base path `/api/v1`, versioned OpenAPI spec exported to `openapi.json` via `./gradlew generateOpenApi`.
- All responses go through the `CommonResponse` envelope described above.
- DTO naming: `{Action}{Domain}Request`/`Response` in `dto/request`, `dto/response`; Application-layer input is a
  `{Action}{Domain}Command` record with a static `from(...)` factory converting from the Request DTO.

### Infrastructure (`infra/`)

The project is mid-migration from AWS (`infra/aws/`, EC2 + Terraform, being removed) to Oracle Cloud Infrastructure
(`infra/oci/`, Terraform) — current branch is `feature/oci-migration`. `.github/workflows/deploy.yml` currently
deploys to EC2 via SSH/Docker on push to `dev`; treat this as the AWS-era pipeline that will need updating once the
OCI migration lands. `*.pem`, `*.tfstate*`, and `*.tfvars` under `infra/` are gitignored — never commit these even
if they appear as untracked local files.

## Conventions

- All chat responses and git commit messages should be written in Korean, using Conventional Commits
  (`feat:`, `fix:`, `refactor:`, `docs:`, etc. — see `.cursorrules`).
- **Never run `git commit` — the user commits everything themselves.** This is deliberate: delegating commits
  cost the user their own understanding of the codebase, so the commit step is where they read the diff.
  Instead of committing, propose a commit plan: which files go in which commit, why they are grouped that way,
  and the Korean Conventional Commits message for each. Include a short plain-language summary of what actually
  changed in each commit so the user can review the diff without re-deriving it. The same applies to
  `git push`, `gh pr create`, and anything else that publishes work — propose, don't execute.
  Staging (`git add`) also waits for the user unless they ask otherwise.
- **Domain-science values require a citation.** Any numeric threshold, coefficient, or classification table derived
  from astronomy/photometry domain knowledge (e.g. Bortle class boundaries, VIIRS radiance→Bortle mapping, scoring
  deduction weights such as `(Bortle-1)*6.25` or the moon-brightness `×30` factor, twilight altitude cutoffs) must
  not be added or changed without a traceable source:
  - Find a concrete reference first (paper, official standard, dataset/tool documentation) that justifies the number
    — don't invent a plausible-looking value.
  - Add a code comment at the point of use citing that reference (author/title/year or URL), not just a description
    of what the number does.
  - If no reference can be found, say so explicitly instead of guessing — an honest "unknown" is better than a
    fabricated citation.
  - This rule exists because the current Bortle-related coefficients (the `radiance_to_bortle` thresholds in the
    external VIIRS conversion script, and the `(Bortle-1)*6.25` / moon `×30` weights in `StargazingScoringEngine`)
    have no recoverable source — the original reasoning was lost during earlier ad-hoc AI-assisted iteration and
    needs to be re-derived from a real reference before being trusted or changed further.
- Further docs: [docs/ARCHITECTURE_GUIDE.md](docs/ARCHITECTURE_GUIDE.md) (layering rules, full checklist),
  [docs/SECURITY_AUTH_GUIDE.md](docs/SECURITY_AUTH_GUIDE.md) (auth flow),
  [docs/MOBILE_DEVELOPER_GUIDE.md](docs/MOBILE_DEVELOPER_GUIDE.md), [docs/SWAGGER_GUIDE.md](docs/SWAGGER_GUIDE.md),
  [docs/MONITORING_AND_ALERTS.md](docs/MONITORING_AND_ALERTS.md).
