# GlobalShop — Maven to Docker: practical lab guide

Run commands from the **project root** unless specified. Use **Java 21**, **Maven 3.6.3+**, **Docker Compose v2** and Git. For the complete app start instructions, see the root README.

## Lab 0: Explore the application

1. `mvn spring-boot:run` (H2 file DB); open `http://localhost:8080`.
2. Add a product to Bag, complete the practice checkout, save the `GS-XXXXXXXX` order number.
3. Track the order with order number **and the same email**.
4. Open `http://localhost:8080/admin.html` and enter **`dev-only-change-me`** locally.
5. Under Recent Orders, move it through **PLACED → PACKING → SHIPPED → DELIVERED**.
6. Edit product stock under Inventory Health, click Save, refresh the storefront.
7. Create another order, mark it CANCELLED from PLACED/PACKING, observe that stock is restored.

**Realistic user story:** customer buys two headphones → inventory reserves 2 units inside a database transaction → operations can pack/ship → customer tracks their status.

## Lab 1: Maven / Java build

```bash
java -version
mvn -version
mvn validate
mvn clean compile
mvn test
mvn verify
mvn package
mvn dependency:tree
mvn help:effective-pom
```

Look at `pom.xml`: parent, groupId/artifactId/version, JAR packaging, starters (webmvc/JPA/validation/actuator), runtime DB drivers, test-scoped dependencies, JaCoCo and Maven deploy profile.

Your JAR is `target/globalshop-1.0.0-SNAPSHOT.jar`. `mvn verify` runs tests and generates JaCoCo XML/HTML under `target/site/jacoco/`. `mvn clean` removes `target/`. `mvn package -DskipTests` builds without executing tests, but prefer full `verify` in CI.

Run the JAR:

```bash
java -jar target/globalshop-1.0.0-SNAPSHOT.jar
```

Stop it before starting the Docker app, or they'll both try to bind port 8080.

## Lab 2: Sonatype Nexus Repository (artifact management)

**Purpose:** the JAR produced by Maven should be stored in Nexus for reproducible download and promotion. Nexus is **not the application registry/database**.

```bash
docker compose -f compose.tools.yml --profile nexus up -d
docker compose -f compose.tools.yml ps
docker exec globalshop-lab-tools-nexus-1 cat /nexus-data/admin.password
```

If the container name differs, inspect `docker ps` and replace it in `docker exec`. Open `http://localhost:8081`; use username **admin** and the bootstrap password above, then complete the initial setup. Confirm `maven-snapshots` and `maven-releases` hosted repositories exist (create them via Nexus UI if not).

In a Linux/macOS shell:

```bash
export NEXUS_USERNAME=admin
export NEXUS_PASSWORD='YOUR_CHANGED_NEXUS_PASSWORD'
mvn -s config/nexus-settings.example.xml -DnexusUrl=http://localhost:8081 clean deploy
```

In **PowerShell**:

```powershell
$env:NEXUS_USERNAME = 'admin'
$env:NEXUS_PASSWORD = 'YOUR_CHANGED_NEXUS_PASSWORD'
mvn -s config/nexus-settings.example.xml -DnexusUrl=http://localhost:8081 clean deploy
```

Because the project version ends in `-SNAPSHOT`, Maven publishes it to **`maven-snapshots`**. In Nexus browse:

`maven-snapshots/com/globalshop/globalshop/1.0.0-SNAPSHOT/`

Want to demonstrate a release? Change `<version>1.0.0-SNAPSHOT</version>` to `<version>1.0.0</version>` in `pom.xml`; running `mvn ... deploy` publishes to **maven-releases**. **Never commit real Nexus passwords**. Publishing requires connectivity from the runner to Nexus; `localhost` inside a GitHub Actions runner is **not your laptop**. If your Nexus is local only, run this lab locally or use a secure self-hosted runner.

## Lab 3: SonarQube Community Build (code quality)

Start SonarQube and its PostgreSQL DB independently of Nexus:

```bash
docker compose -f compose.tools.yml --profile sonar up -d
docker compose -f compose.tools.yml --profile sonar ps
```

Open `http://localhost:9000`. The initial admin credentials are usually **admin/admin**, which SonarQube requires you to change. Create a local project with key **globalshop-java**, generate a project analysis token, and then run:

**Linux/macOS:**

```bash
export SONAR_TOKEN='PASTE_YOUR_PROJECT_TOKEN'
mvn clean verify org.sonarsource.scanner.maven:sonar-maven-plugin:5.5.0.6356:sonar \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.token="$SONAR_TOKEN"
```

**PowerShell:**

```powershell
$env:SONAR_TOKEN = 'PASTE_YOUR_PROJECT_TOKEN'
mvn clean verify org.sonarsource.scanner.maven:sonar-maven-plugin:5.5.0.6356:sonar "-Dsonar.host.url=http://localhost:9000" "-Dsonar.token=$env:SONAR_TOKEN"
```

Study: bugs, vulnerabilities, security hotspots, duplicate code, coverage, maintainability, quality gates. To demonstrate coverage improvements, write tests for the order status transitions.

> On Linux, SonarQube uses embedded search components that may need higher `vm.max_map_count` / file-descriptor limits and sufficient RAM. Follow the official SonarQube Docker prerequisites if startup fails.

## Lab 4: Semgrep Community Edition (open-source SAST)

**Checkmarx** is commercial. For a free hands-on equivalent **in the SAST category**, use **Semgrep CE**. They are not feature-identical.

Install Python 3 then:

```bash
python -m pip install semgrep
semgrep scan --config p/owasp-top-ten src/main
semgrep scan --config p/security-audit src/main
semgrep scan --config p/owasp-top-ten --json --output semgrep-results.json src/main
```

The first scan fetches community rules and requires network access. Review SAST findings for actual exploitability; don't blindly assume every finding is a real bug. To make CI fail on findings, add `--error`.

**Suggested security exercise:** create a **separate disposable branch** and intentionally introduce risky string concatenation into a DB query (never deploy it). Show how static analysis detects the pattern; revert before merge. Avoid committing secrets or vulnerable changes to your main branch.

## Lab 5: FOSSA (third-party dependency and license analysis)

FOSSA is a **commercial/cloud platform** with an open-source CLI, not a fully self-hosted open-source replacement. You need a FOSSA account and API token for uploading results.

On Linux/macOS, install the FOSSA CLI using the script described in official docs (review the script before execution):

```bash
curl -H 'Cache-Control: no-cache' https://raw.githubusercontent.com/fossas/fossa-cli/master/install-latest.sh | bash
```

On Windows, use the official FOSSA CLI Windows installer/PowerShell instructions or run via WSL. Then:

```bash
mvn clean verify
export FOSSA_API_KEY='YOUR_FOSSA_TOKEN' # Linux/macOS
fossa analyze
fossa test
```

`fossa analyze` sends a dependency graph to FOSSA, and `fossa test` checks configured license/vulnerability policies. Check the FOSSA UI to see open-source dependencies, their licenses, transitive dependencies, and any security/policy findings. The CLI without a token can often be used with `fossa analyze -o` for local output, but not as a substitute for policy analysis.

### Fully open-source alternative when you have no FOSSA account

Use **OWASP Dependency-Check** as a separate SCA lab. It is not a license-compliance replacement for FOSSA, but it scans dependencies for known CVEs. A current CLI/Maven plugin and vulnerability database access are required; the first scan can be slow. Consult https://owasp.org/www-project-dependency-check/ for setup.

## Lab 6: Docker and PostgreSQL

Learn the difference between build and runtime images:

- `Dockerfile` runs Maven inside a temporary JDK container and copies only the output JAR into a Java runtime image.
- `compose.yml` starts **app + PostgreSQL**, injects DB connection parameters via environment variables, and creates a persistent DB volume.

```bash
cp .env.example .env
# edit .env with your passwords
docker compose build
docker images | grep globalshop
docker compose up -d
docker compose ps
docker compose logs --tail=80 app
curl http://localhost:8080/actuator/health
curl http://localhost:8080/api/products
```

Try one checkout through the browser; restart the app and see that orders persist:

```bash
docker compose restart app
docker compose logs --tail=25 app
docker compose down
```

**Storage warning:** do not use `down -v` unless you want to reset PostgreSQL data. On shared machines don't publish port 8080 directly to the Internet without adding proper security.

## Lab 7: GitHub Actions CI

Workflow `.github/workflows/devsecops.yml` runs on pushes to `main`, pull requests, or manual dispatch:

1. Build/test (`mvn clean verify`), upload test + coverage reports.
2. Run Semgrep OSS SAST (OWASP rules; currently fails the SAST job on a finding).
3. Build the Docker image, **without pushing it anywhere**.
4. Optionally analyze with SonarQube **when repo variable `SONAR_HOST_URL` is set** and secret `SONAR_TOKEN` is provided.
5. Optionally publish the Maven JAR to Nexus **when repo variable `NEXUS_URL` is set**, plus `NEXUS_USERNAME` / `NEXUS_PASSWORD` secrets. This requires public network access or a secure runner accessible to Nexus.
6. Optionally run FOSSA **when repo variable `ENABLE_FOSSA=true`**, plus `FOSSA_API_KEY` secret.

**Important:** GitHub-hosted runners cannot reach SonarQube/Nexus running only on `localhost` on your laptop or private LAN. For a beginner demo, run analysis/publishing locally first, and later set up a properly secured externally reachable service or private self-hosted runner. Do not expose your lab services publicly merely to make CI connect.

## Troubleshooting

| Error | Reason / resolution |
|---|---|
| `mvn: command not found` | Install Maven and put `mvn` on PATH; verify Java 21. |
| Port 8080 in use | Stop `mvn spring-boot:run` before starting Docker, or adjust Compose port mapping. |
| PostgreSQL authentication failed | `.env` may have changed after Docker volume initialized. Use original password or intentionally reset the **lab** volume. |
| SonarQube won't start | Check `docker compose -f compose.tools.yml --profile sonar logs` and Docker RAM/host limits. |
| `401` on admin route | Use `X-Admin-Key` matching `ADMIN_API_KEY` (default only for localhost lab). |
| Nexus 401 / 403 | Check changed admin password, permissions, server IDs in settings file, snapshot vs release repository. |
| Nexus SSL/HTTP issue in CI | Use HTTPS for any non-local deployment and match Maven server IDs with trusted repository configuration. |
| FOSSA 401 / 403 | Check account/API token, permissions and internet connectivity. |
| Semgrep rules fail to download | It needs internet to obtain registry rules. |

**Reference documentation:** https://maven.apache.org/ • https://help.sonatype.com/en/maven-repositories.html • https://docs.sonarsource.com/sonarqube-community-build/ • https://semgrep.dev/docs/ • https://docs.fossa.com/ • https://docs.docker.com/
