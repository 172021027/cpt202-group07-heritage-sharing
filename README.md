# Community Heritage Resource Sharing Platform

> **CPT202 Group 07 — Software Engineering Group Project (2025/2026)**
> Xi'an Jiaotong-Liverpool University · School of Advanced Technology

A web application that lets community contributors submit, describe and
share local intangible / tangible heritage resources, and lets reviewers
moderate them before they appear publicly.

## Live demo

| Item | Value |
|------|-------|
| URL | http://118.178.124.2:8080/login.html |
| Contributor account | `test@contributor.com` / `12345678` |
| Admin account | `admin1@admin.com` / `admin1` |

> The demo server is shared with the marking team; please do not change
> the admin password during the assessment window.

## Tech stack

| Layer | Choice |
|-------|--------|
| Language / Runtime | Java 21 |
| Framework | Spring Boot 3.5.11 |
| Persistence | Spring Data JPA + MySQL (prod) / H2 (test) |
| Cache | Redis |
| Security | Spring Security + JWT |
| Build | Maven (wrapper included) |
| Frontend | Static HTML + jQuery 3.7.1 + plain CSS (no build step) |
| Test | JUnit 5 + Spring Boot Test + MockMvc |

## Repository layout

```
src/
  main/
    java/com/example/heritage_sharing_api/
      config/        # Web, Redis, Security, DataInitializer
      controller/    # REST endpoints (auth, resource, review, taxonomy, ...)
      dto/           # Request / response payloads
      entity/        # JPA entities + enums + converters
      exception/     # Domain exceptions + global handler
      repository/    # Spring Data JPA repositories
      security/      # JwtUtil, JwtAuthenticationFilter, SecurityConfig
      service/       # Business logic
    resources/
      application.properties
      html/          # Server-rendered HTML pages
      static/        # CSS / JS / images
  test/
    java/.../unit/         # Unit tests
    java/.../integration/  # Integration tests (H2 profile)
docs/                # Section 2 scaffolding + UML mermaid sources
```

## Run locally

Prerequisites: JDK 21, MySQL 8 with a database named `heritage_sharing`,
Redis running on `localhost:6379`.

```powershell
# Windows / PowerShell
.\mvnw.cmd spring-boot:run
```

The app starts on `http://localhost:8080`. Open `/login.html` and sign
in with the seed accounts created by `config/DataInitializer.java`.

## Run the test suite

```powershell
.\mvnw.cmd test
```

Tests run against the in-memory H2 profile defined in
`src/test/resources/application-test.properties`, so no local MySQL is
required.

## Functional modules (9-person team split)

| # | Module | Owner |
|---|--------|-------|
| 1 | Auth & account | teammate |
| 2 | Profile & contributor application | teammate |
| 3 | Admin contributor-request approval | teammate |
| 4 | Login session / JWT | teammate |
| 5 | **Resource creation & metadata maintenance** | **Fan Shuaifei** |
| 6 | File upload & submission action | teammate |
| 7 | Reviewer approval workflow | teammate |
| 8 | Browse / search / comments | teammate |
| 9 | Taxonomy & archive / restore | teammate |

A full file-by-file ownership map for Module 5 is in
[`docs/section2_ownership.md`](docs/section2_ownership.md).

## Documentation

| File | Purpose |
|------|---------|
| [`docs/section2_reference.md`](docs/section2_reference.md) | Outline scaffolding for the Section 2 individual report |
| [`docs/section2_ownership.md`](docs/section2_ownership.md) | Module 5 ownership map (mine vs. shared vs. others') |
| [`docs/section2_diagrams.md`](docs/section2_diagrams.md) | Mermaid sources for state / sequence / activity / ERD diagrams |
| [`docs/section2_trello_template.md`](docs/section2_trello_template.md) | Screenshot checklist for the Jira / Trello appendix |

## License

Coursework submission for CPT202 2025/2026. Not licensed for
redistribution outside the marking process.
