Run & Deploy
===========

Overview
--------
This document explains how to run the application locally for development, testing, and production-like runs on Windows (PowerShell). It also includes common troubleshooting steps (e.g. missing Spring Security JOSE dependency).

Profiles and properties
-----------------------
Spring Boot picks up `application-<profile>.properties` automatically when you set `spring.profiles.active`.
This project provides:
- `application-dev.properties`
- `application-test.properties`
- `application-prod.properties`

Quick run (bootRun)
--------------------
Use `bootRun` for quick local runs (development):

PowerShell examples

Run with `dev` profile:
```powershell
./gradlew.bat bootRun --args='--spring.profiles.active=dev'
```

Run with `prod` profile (development runner only — prefer jar in real prod):
```powershell
./gradlew.bat bootRun --args='--spring.profiles.active=prod'
```

Run with `test` profile:
```powershell
./gradlew.bat bootRun --args='--spring.profiles.active=test'
```

Alternative ways to set the active profile
------------------------------------------
Set environment variable (PowerShell):
```powershell
$env:SPRING_PROFILES_ACTIVE='prod'
./gradlew.bat bootRun
```

Use JVM system property (Gradle/Different invocation):
```powershell
./gradlew.bat bootRun -Dspring.profiles.active=prod
```

Recommended production flow (jar)
---------------------------------
1. Build an executable jar (preferred for prod):
```powershell
./gradlew.bat clean bootJar
```
2. Run the produced jar from `build/libs`:
```powershell
java -jar build\libs\<your-app>-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```
Replace `<your-app>-0.0.1-SNAPSHOT.jar` with the actual artifact name you find in `build/libs`.

Running tests with a profile
----------------------------
Pass a profile via system property or env var:
```powershell
./gradlew.bat test -Dspring.profiles.active=test
# or
$env:SPRING_PROFILES_ACTIVE='test'
./gradlew.bat test
```

Troubleshooting: missing OAuth2 JOSE classes
-------------------------------------------
Problem: compile errors like
```
cannot find symbol: class OAuth2TokenValidator
```
Cause: `spring-security-oauth2-jose` is not on the classpath. That module provides JWT validation APIs used by custom validators (e.g. `JwtAudienceValidatorConfig`).

Recommended fix (Gradle)
Add the resource-server starter which pulls in the JOSE module transitively:

In `build.gradle` (dependencies block):
```gradle
implementation 'org.springframework.boot:spring-boot-starter-security'
implementation 'org.springframework.boot:spring-boot-starter-oauth2-resource-server'
```

Alternative (add only JOSE):
```gradle
implementation 'org.springframework.security:spring-security-oauth2-jose'
```

After adding dependency run:
```powershell
./gradlew.bat clean build
```

If the build still fails:
- Check for dependency exclusions in `build.gradle` or `dependencyManagement` overrides.
- Ensure Spring Security versions are compatible with Spring Boot 4's managed versions.

Notes & Recommendations
-----------------------
- `bootRun` is convenient for local development but not for production. Use `bootJar` + `java -jar` for production-like runs.
- For CI, run `./gradlew.bat clean build` and run integration tests in a container or test environment with `--spring.profiles.active=test`.
- To see verbose Spring Boot startup logs, add `--debug` or set `logging.level.root=DEBUG` in appropriate `application-*.properties`.

Want me to:
- build the `prod` jar now and run it, or
- run `./gradlew.bat clean build` to verify fixes on this machine?
