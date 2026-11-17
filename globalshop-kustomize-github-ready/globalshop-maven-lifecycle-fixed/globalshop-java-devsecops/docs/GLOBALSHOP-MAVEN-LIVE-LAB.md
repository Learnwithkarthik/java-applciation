# GlobalShop Maven Lifecycle — Real Ubuntu Live Demo (iximiuz)

This lab uses the **actual GlobalShop Spring Boot Java 21 project**, not a MiniShop / Hello World example.

**Main file:** `pom.xml`  
**Real Java application:** `src/main/java/com/globalshop/GlobalShopApplication.java`  
**Real business code:** `src/main/java/com/globalshop/service/ShopService.java`  
**Real integration tests:** `src/test/java/com/globalshop/GlobalShopFlowTest.java`  
**Artifact:** `target/globalshop-1.0.0-SNAPSHOT.jar`  
**Optional Maven observer:** `bash scripts/inspect-maven-build.sh`

## 0. Install on Ubuntu

Ubuntu lab sessions may have `root` instead of a sudo-enabled user. If the prompt ends in `#`, remove `sudo` from the commands below.

```bash
sudo apt update
sudo apt install -y openjdk-21-jdk maven git tree unzip curl
java -version
javac -version
mvn -version
```

Java must show version 21. Maven should be 3.6.3 or newer (3.9.x is recommended). Spring Boot 4.1.2 supports Maven 3.6.3+ and Java 17+; this app is configured for Java 21.

## 1. Get the real project into iximiuz

**Option A: cloned from your GitHub repository (recommended for your DevOps training):**

1. Download the project ZIP, extract it on your laptop.
2. Upload the **contents** of `globalshop-java-devsecops/` to your GitHub repository, including `pom.xml`, `src/`, `scripts/`, `config/`, and `docs/`.
3. In iximiuz Ubuntu:

```bash
git clone https://github.com/YOUR-USER/globalshop-java-devsecops.git
cd globalshop-java-devsecops
```

**Option B: if the lab provides a file upload:** transfer this ZIP, then use `unzip globalshop-maven-lifecycle-live-lab.zip` and `cd globalshop-java-devsecops`.

Check project files:

```bash
pwd
ls -la
sed -n '1,95p' pom.xml
find src/main/java/com/globalshop -name '*.java' | sort
find src/test/java -name '*.java'
```

### 1A. Explain the real POM

The actual `pom.xml` defines:

- `groupId`: `com.globalshop`
- `artifactId`: `globalshop`
- `version`: `1.0.0-SNAPSHOT`
- `java.version`: `21`
- Spring Boot web/JPA/validation/test dependencies
- Spring Boot Maven plugin (creates executable JAR)
- JaCoCo plugin (generates coverage in `verify`)
- `nexus-deploy` profile (for a later session, not required here)

Point out that `pom.xml` is in Git and defines the project, while `~/.m2/settings.xml` is **optional user / CI configuration** for mirrors, credentials, and repository access. Do **not** put Nexus passwords inside `pom.xml`.

## 2. Initial picture (before running Maven)

```bash
ls -la target 2>/dev/null || echo 'No target directory (fresh source code)'
bash scripts/inspect-maven-build.sh
```

### 2A. `mvn clean`

```bash
mvn clean
ls -ld target || echo 'target/ has been removed'
bash scripts/inspect-maven-build.sh
```

Say: **clean deletes the output directory (`target/`) from old builds.** `clean` is its own lifecycle, not a stage inside the default lifecycle.

### 2B. `mvn validate`

```bash
mvn validate
ls -ld target || echo 'No compiled output yet'
```

Say: **Maven reads and validates project metadata in `pom.xml`.** No `.class` / JAR yet.

### 2C. `mvn compile`

Show source code first:

```bash
sed -n '1,100p' src/main/java/com/globalshop/GlobalShopApplication.java
sed -n '1,100p' src/main/java/com/globalshop/model/Product.java
```

Then compile:

```bash
mvn compile
bash scripts/inspect-maven-build.sh
find target/classes -type f -name '*.class' | sort | head -n 25
ls -l target/classes/static/index.html
```

Say: **Maven + compiler plugin turns `.java` into `.class` bytecode** and also copies application resources (HTML, CSS, JS, YAML). In `target/classes` you can show both `.class` files and static files.

### 2D. `mvn test`

Show the existing tests:

```bash
sed -n '1,170p' src/test/java/com/globalshop/GlobalShopFlowTest.java
mvn test
bash scripts/inspect-maven-build.sh
ls target/surefire-reports
cat target/surefire-reports/com.globalshop.GlobalShopFlowTest.txt
```

The 4 GlobalShop tests check catalog data, checkout & tracking, invalid requests/admin authorization, and stock limitations. Tests use H2 in-memory database and `application-test.yml`.

Say: **Maven test phase compiles test classes and asks Surefire/JUnit to execute them.** Maven stops the build when a test fails.

### 2E. Show a REAL failed build (optional but valuable)

This safely changes a test expectation to intentionally make it fail. It does **not** break production Java source. Do this after a successful test run.

```bash
TEST=src/test/java/com/globalshop/GlobalShopFlowTest.java
cp "$TEST" /tmp/GlobalShopFlowTest.java.backup
sed -i 's/hasSize(12)/hasSize(99)/' "$TEST"
grep 'hasSize(' "$TEST"
mvn clean package  # EXPECTED: BUILD FAILURE at test phase
ls target/*.jar 2>/dev/null || echo 'No new JAR: tests stopped packaging'
cp /tmp/GlobalShopFlowTest.java.backup "$TEST"   # restore the original test
mvn test    # EXPECTED: BUILD SUCCESS
```

Explain: **testing is a quality gate in CI; a failing test blocks the artifact.**

### 2F. `mvn package`

```bash
mvn package
bash scripts/inspect-maven-build.sh
ls -lh target/*.jar
jar tf target/globalshop-1.0.0-SNAPSHOT.jar | head -n 40
jar tf target/globalshop-1.0.0-SNAPSHOT.jar | grep -E 'BOOT-INF/classes/com/globalshop|BOOT-INF/lib' | head -n 20
```

Expected: `target/globalshop-1.0.0-SNAPSHOT.jar`. The Spring Boot plugin repackages an executable JAR, including application classes, HTML/CSS/JS and runtime libraries under `BOOT-INF/`.

Explain: **JAR = packaged Java application that can be moved and run in another environment with a compatible Java runtime.** The JAR is **not** source code and **not** a Docker image.

### 2G. Run the REAL GlobalShop web application

Run in first terminal:

```bash
java -jar target/globalshop-1.0.0-SNAPSHOT.jar
```

From a **second terminal in the same lab** (or stop with `Ctrl+C` after viewing server startup, then run it in the background):

```bash
curl -s http://localhost:8080/actuator/health
curl -s http://localhost:8080/api/products | head -c 700
```

The server should respond at `/`, `/admin.html`, and `/api/products`. To see the website in a browser outside the lab, use the port exposure / forwarding facility provided by your iximiuz lab. Local `localhost` is inside the lab, not your laptop.

If you want only one terminal instead:

```bash
java -jar target/globalshop-1.0.0-SNAPSHOT.jar > /tmp/globalshop.log 2>&1 &
echo $! > /tmp/globalshop.pid
sleep 12
curl -s http://localhost:8080/actuator/health
curl -s http://localhost:8080/api/products | head -c 300
kill "$(cat /tmp/globalshop.pid)"
```

### 2H. `mvn verify`

```bash
mvn verify
bash scripts/inspect-maven-build.sh
ls -lh target/site/jacoco/index.html
ls -lh target/site/jacoco/jacoco.xml
```

Explain: **verify runs checks configured for this build.** GlobalShop's `pom.xml` binds JaCoCo's report goal to `verify`; that's why an HTML/XML coverage report appears. JaCoCo is a plugin, **not** a built-in Maven report.

### 2I. `mvn install`

```bash
mvn install
bash scripts/inspect-maven-build.sh
ls -lh ~/.m2/repository/com/globalshop/globalshop/1.0.0-SNAPSHOT/
```

Explain: **install copies this JAR + POM into the local Maven repository**, so other builds on that computer can reference it.

### 2J. `mvn deploy` — do not run until Nexus is configured

`mvn deploy` publishes to a remote artifact repository, **not** to a Kubernetes cluster or application server.

This app's POM contains an optional `nexus-deploy` profile, activated with `-DnexusUrl=...`; the sample `config/nexus-settings.example.xml` shows how credentials relate to server IDs. After setting up Nexus, you can publish with a command such as:

```bash
mvn -s /path/to/private-settings.xml -DnexusUrl=http://YOUR-NEXUS:8081 clean deploy
```

This is a **future lab step**. Do not use sample passwords in real credentials files, and do not commit them to GitHub.

## Quick memory table

| Command | What changes on disk in GlobalShop? |
| --- | --- |
| `mvn clean` | Deletes `target/` |
| `mvn validate` | Validates POM metadata; no JAR |
| `mvn compile` | Creates `.class` files and resources in `target/classes/` |
| `mvn test` | Creates test bytecode in `target/test-classes/` and Surefire reports |
| `mvn package` | Creates `target/globalshop-1.0.0-SNAPSHOT.jar` |
| `mvn verify` | Creates JaCoCo coverage report because the project configures it |
| `mvn install` | Copies artifact to `~/.m2/repository/com/globalshop/globalshop/1.0.0-SNAPSHOT/` |
| `mvn deploy` | Uploads artifact to configured remote repository (Nexus later) |

## One-line summary

`.java` -> (`mvn compile`) `.class` -> (`mvn test`) validated code -> (`mvn package`) executable GlobalShop JAR -> (`mvn install`) local Maven repo -> (`mvn deploy`) Nexus.

**Key behavior:** Maven always executes preceding phases in the default lifecycle when you call a later phase; e.g., `mvn package` includes compile + test. You run phases separately in class only to **show what each step generates**.

References: https://maven.apache.org/guides/introduction/introduction-to-the-lifecycle.html and https://docs.spring.io/spring-boot/system-requirements.html
