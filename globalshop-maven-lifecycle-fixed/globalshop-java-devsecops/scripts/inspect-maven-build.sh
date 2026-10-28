#!/usr/bin/env bash
set -u

# GlobalShop Maven Lifecycle: visualize exactly what Maven has generated so far.
# Run from the GlobalShop project root: bash scripts/inspect-maven-build.sh

if [[ ! -f pom.xml ]]; then
  echo "Run this script inside the globalshop-java-devsecops directory (where pom.xml exists)."
  exit 1
fi

printf '\n======================================================\n'
printf ' GlobalShop — Maven Build Output Inspector\n'
printf '======================================================\n'
printf 'Project: com.globalshop:globalshop:1.0.0-SNAPSHOT\n\n'

if [[ -d target ]]; then
  printf 'target/ exists: YES\n'
else
  printf 'target/ exists: NO  (not built yet, or mvn clean just ran)\n'
fi

printf '\n[1] COMPILE: .java -> .class\n'
if [[ -d target/classes ]]; then
  count=$(find target/classes -type f -name '*.class' | wc -l | tr -d ' ')
  printf 'Generated main .class files: %s\n' "$count"
  find target/classes -type f -name '*.class' | head -n 8 || true
  if [[ -f target/classes/static/index.html ]]; then
    printf 'Frontend resource copied: target/classes/static/index.html\n'
  fi
else
  printf 'No compiled classes yet. Try: mvn compile\n'
fi

printf '\n[2] TEST: compile + run GlobalShopFlowTest\n'
if [[ -d target/test-classes ]]; then
  count=$(find target/test-classes -type f -name '*.class' | wc -l | tr -d ' ')
  printf 'Generated test .class files: %s\n' "$count"
else
  printf 'No test .class files yet. Try: mvn test\n'
fi
if [[ -d target/surefire-reports ]]; then
  printf 'Test report files:\n'
  find target/surefire-reports -maxdepth 1 -type f -name '*.txt' -print
  grep -h '^Tests run:' target/surefire-reports/*.txt 2>/dev/null || true
else
  printf 'No test reports yet. Try: mvn test\n'
fi

printf '\n[3] PACKAGE: .jar file\n'
found=0
for f in target/globalshop-*.jar; do
  if [[ -f "$f" ]]; then
    ls -lh "$f"
    found=1
  fi
done
if [[ $found -eq 0 ]]; then
  printf 'No JAR yet. Try: mvn package\n'
fi

printf '\n[4] VERIFY: JaCoCo coverage report\n'
if [[ -f target/site/jacoco/index.html ]]; then
  printf 'Coverage HTML: target/site/jacoco/index.html\n'
  [[ -f target/site/jacoco/jacoco.xml ]] && printf 'Coverage XML:  target/site/jacoco/jacoco.xml\n'
else
  printf 'No coverage report yet. Try: mvn verify\n'
fi

printf '\n[5] INSTALL: local Maven artifact\n'
local_repo="${HOME}/.m2/repository/com/globalshop/globalshop/1.0.0-SNAPSHOT"
if [[ -d "$local_repo" ]]; then
  ls -lh "$local_repo"/* 2>/dev/null | head -n 12 || true
else
  printf 'Not installed locally yet. Try: mvn install\n'
fi

printf '\nNote: mvn clean removes target/, but not ~/.m2/repository/.\n'
