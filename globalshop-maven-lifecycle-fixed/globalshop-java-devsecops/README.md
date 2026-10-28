# GlobalShop — Realistic Java DevSecOps Training Application

A complete **retail storefront + order processing + inventory dashboard**, ready for Maven, Nexus Repository, SonarQube, Semgrep (open-source SAST alternative to Checkmarx), FOSSA, and Docker. **Kubernetes is intentionally not included**; the app can be containerized and deployed there later.

> ⚠️ This is a **functioning training application**, not a production commerce platform. It does not accept actual payments, send orders to couriers, or implement real customer authentication. The admin area uses a training-only API key; for production, use OIDC/SSO, role-based authorization, rate limiting, audit trails, database migrations and observability.

## What you can actually do

- **Customer storefront**: responsive product catalog, text search, six categories, product pricing and available inventory.
- **Shopping bag**: add/remove products, change quantities, client-side localStorage persistence.
- **Order placement**: name, email, shipping address, demo cash-on-delivery checkout; writes to a real DB.
- **Order tracking**: uses **order number AND the checkout email**; protected from trivial order-number guessing.
- **Operations dashboard** (`/admin.html`): gross order value, pending orders, inventory counts, edit stock, view orders, transition order statuses.
- **Back-end controls**: Jakarta Bean Validation, locked inventory decrements to prevent overselling, error responses, transaction rollback, status workflow, cancellation restocking, API key for admin lab.
- **DevSecOps**: unit/integration tests, JaCoCo coverage, Maven build, optional Nexus upload, SonarQube analysis, Semgrep SAST, FOSSA SCA, Docker image, GitHub Actions pipeline.

## Architecture

```text
Browser: / (storefront)                   Browser: /admin.html (operations)
                |                                        |
                +----------- REST/HTTP ------------------+
                                     |
                        Java 21 / Spring Boot 4.1.2
                   /api/products  /api/orders  /api/admin/*
                                     |
                     Spring Data JPA + transactions
                                     |
                PostgreSQL 17 (Docker) or H2 file (local Maven)

Development toolchain, introduced gradually:
Git -> Maven -> JUnit + JaCoCo -> Nexus -> SonarQube -> Semgrep
    -> FOSSA -> Docker -> GitHub Actions -> [Kubernetes later]
```

## Quick start: easiest way (Java + Maven, no Docker)

Install **Java 21** and **Apache Maven 3.6.3+**. From the root directory run:

```bash
java -version
mvn -version
mvn clean verify
mvn spring-boot:run
```

Open **http://localhost:8080/** for the store and **http://localhost:8080/admin.html** for the dashboard. Default **training-only** admin key: `dev-only-change-me`.

This mode persists a small file-backed H2 database under `./data/`; the first startup seeds **12 products**. Rerunning the app does not add duplicates. To reset training data, stop the app and delete the `data/` directory, then restart.

> Customize the admin key locally with `ADMIN_API_KEY` (or with your IDE environment variables). Do not use the default value on a public host.

## Quick start: Docker + PostgreSQL

Install Docker Engine / Docker Desktop with Docker Compose v2.

```bash
cp .env.example .env
# Edit .env: replace POSTGRES_PASSWORD and ADMIN_API_KEY with your own values.
docker compose up --build -d
docker compose ps
docker compose logs -f app
```

Open **http://localhost:8080** and **http://localhost:8080/admin.html**. On Windows PowerShell replace `cp` with `Copy-Item`. To stop:

```bash
docker compose down
```

The PostgreSQL data lives in a named Docker volume and survives `down`. **`docker compose down -v` deletes the database and all orders**.

## Lab roadmap

| Lab | Tool | What you will learn | Commands / destination |
|---|---|---|---|
| 1 | Maven | POM, lifecycle, compilation, tests, JAR | `mvn clean verify`; `target/globalshop-1.0.0-SNAPSHOT.jar` |
| 2 | Nexus Repository | Hosted snapshot repository, Maven deploy | `docker compose -f compose.tools.yml --profile nexus up -d` |
| 3 | SonarQube | Quality gate, bugs, code smells, coverage | `docker compose -f compose.tools.yml --profile sonar up -d` |
| 4 | Semgrep CE | Open-source SAST alternative to Checkmarx | `semgrep scan --config p/owasp-top-ten src/main` |
| 5 | FOSSA | Dependency licenses + vulnerability policies | `fossa analyze`, `fossa test` (requires your FOSSA account/token) |
| 6 | Docker | Multi-stage image, runtime containers, PostgreSQL | `docker compose up --build -d` |
| 7 | GitHub Actions | Automated build, SAST, Docker build, optional publishing | `.github/workflows/devsecops.yml` |
| 8 (later) | Kubernetes | Deployments, ConfigMaps, Secrets, Services, probes, Ingress | Not included yet |

**Interactive Maven classroom visual:** [docs/MAVEN-VISUAL-LAB.html](docs/MAVEN-VISUAL-LAB.html) — offline HTML simulator for Maven lifecycle, JAR internals, `pom.xml`, `settings.xml`, Nexus, and a student quiz. Open the file in a browser; the simulation does not run actual Maven commands.

**Full hands-on commands:** [docs/DEVSECOPS-LABS.md](docs/DEVSECOPS-LABS.md)  
**Architecture/API reference:** [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)

## Project layout

```text
globalshop-java-devsecops/
├── pom.xml                          # Maven parent, Java 21, tests, JaCoCo, Nexus deployment profile
├── src/
│   ├── main/
│   │   ├── java/com/globalshop/
│   │   │   ├── GlobalShopApplication.java
│   │   │   ├── api/                    # REST controllers, requests/responses, errors
│   │   │   ├── model/                  # Product, Order, OrderItem, OrderStatus
│   │   │   ├── repository/             # Spring Data JPA repositories
│   │   │   ├── service/                # Checkout, tracking, inventory, workflow
│   │   │   └── config/DemoData.java    # Initial seed data
│   │   └── resources/
│   │       ├── application.yml
│   │       └── static/                # Full customer UI and admin UI
│   └── test/                          # H2 integration tests
├── Dockerfile                       # Maven/JRE multi-stage image
├── compose.yml                      # Working app + PostgreSQL
├── compose.tools.yml                # Optional Nexus / SonarQube services
├── config/nexus-settings.example.xml
├── .github/workflows/devsecops.yml  # CI pipeline (no Kubernetes)
├── .env.example                     # Sample local config; never commit .env
├── postman/GlobalShop.postman_collection.json  # Ready-to-import REST collection
├── scripts/smoke-test.sh
└── docs/
    ├── DEVSECOPS-LABS.md
    └── ARCHITECTURE.md
```

## Postman API demos

Import `postman/GlobalShop.postman_collection.json`, update `baseUrl` / `adminKey` as needed, and try the customer and admin requests. After placing an order, copy the returned number into the `orderNumber` collection variable.

## Quick API test

```bash
curl http://localhost:8080/actuator/health
curl http://localhost:8080/api/products
curl http://localhost:8080/api/products?category=Audio
```

See the architecture document for order JSON and admin API examples.

## Push this folder to your own GitHub repository

This package is a ZIP **to become your own Git repository**. It is not hosted on GitHub yet. After unzipping, create an **empty** repository on GitHub (without initializing its README) and run:

```bash
cd globalshop-java-devsecops
git init
git branch -M main
git add .
git commit -m "Initial GlobalShop Java DevSecOps application"
git remote add origin https://github.com/YOUR_USERNAME/globalshop-java-devsecops.git
git push -u origin main
```

Then students can `git clone https://github.com/YOUR_USERNAME/globalshop-java-devsecops.git` and work from their own repository. Prefer SSH or Git Credential Manager; do not put a personal access token into the remote URL.

## Important security and operational notes

- No real personal information or payments should be entered into this practice app.
- Do **not** expose `/admin.html` or the demo API key to the public Internet; `/admin.html` is a UI, not authentication. The API is key-protected for training only.
- The default dev passwords in the compose files are for localhost learning; replace them for any non-local setup.
- `spring.jpa.hibernate.ddl-auto=update` is for the lab. Use versioned database migrations (Flyway/Liquibase) for a real deployment.
- Local H2 is a convenience; **PostgreSQL is the intended service database** for Docker and Kubernetes.
- SonarQube, Nexus and PostgreSQL need memory/storage. Start tools separately on a low-resource laptop.
- This ZIP was statically inspected but **not compiled/run inside the generation environment**, because Maven and network downloads were unavailable there. Run `mvn clean verify` first and report any environment-specific errors.


## Maven Lifecycle classroom demonstration (iximiuz Ubuntu)

See [docs/GLOBALSHOP-MAVEN-LIVE-LAB.md](docs/GLOBALSHOP-MAVEN-LIVE-LAB.md) and run `bash scripts/inspect-maven-build.sh` after each Maven command to see exactly what changed.
