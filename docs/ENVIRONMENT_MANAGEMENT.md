# Γ£¬ Environment Variable Management

This document explains how the E-Shop backend manages configuration and secrets using a dynamic, zero-hardcoding approach.

## Γ£ì The Architecture

We use a **"Gradle-First"** loading strategy. This ensures that all environment variables are available to Spring Boot before it starts validating the configuration, preventing common errors like `NumberFormatException`.

### 1. The .env File
The [**.env**](file:///g:/Project/eshop_back/.env) file is the **Single Source of Truth**. 
- It contains all database passwords, JWT secrets, and realm configurations.
- **Security**: This file is ignored by Git and should never be committed.

### 2. Gradle Integration
The [**build.gradle**](file:///g:/Project/eshop_back/build.gradle) file contains a custom configuration for the `bootRun` task:
```groovy
tasks.named('bootRun').configure {
    if (file(".env").exists()) {
        file(".env").readLines().each { line ->
            // Parses key=value pairs and injects them into the JVM
        }
    }
}
```
This script reads your `.env` file and "injects" the values directly into the running application.

### 3. Clean Property Files
Because Gradle loads the variables early, our [**application.properties**](file:///g:/Project/eshop_back/src/main/resources/application.properties) files remain clean:
- **No hardcoded passwords**: Everything uses `${VAR_NAME}`.
- **No temporary fallbacks**: No more `:86400000` mixed into the code.

---

## Γî║ How to Add New Variables

1.  **Update .env**: Add your new variable (e.g., `NEW_SETTING=example`).
2.  **Update Properties**: Reference it in the appropriate `application.properties` file using `${NEW_SETTING}`.
3.  **Use in Java**: Access it via `@Value("${NEW_SETTING}")` or the `AppProperties` class.

## ΓÜá∩╕Å Troubleshooting

- **"Property Not Found"**: Ensure the variable name in your `.env` exactly matches the placeholder in your `.properties` file (Case-Sensitive).
- **Gradle Message**: When you start the app, look for the message: `Γ£à Loaded environment variables from .env` to confirm the loading succeeded.
