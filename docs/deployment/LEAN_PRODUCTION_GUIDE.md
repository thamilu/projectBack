# Lean Production Deployment Guide [HARDEN]

This guide provides a step-by-step roadmap for deploying the E-Shop backend to a low-cost VPS (2GB RAM) using the **Lean Hybrid Architecture**.

---

## 1. Architecture Overview

To fit within a 2GB VPS, we offload the heaviest components to external managed services.

- **VPS (2GB)**: Spring Boot API + Nginx + SSL.
- **Auth**: AWS Cognito (Managed).
- **Database**: Neon.tech (Managed Postgres).
- **Cache**: Upstash (Managed Redis).
- **Images**: Cloudflare R2 (Already configured).
- **Frontend**: Vercel / Cloudflare Pages.

---

## 2. Infrastructure Setup

### A. Managed Services (Free Tiers)
1.  **Neon.tech**:
    - Create a project at [neon.tech](https://neon.tech).
    - Get your Connection String (JDBC).
2.  **Upstash**:
    - Create a Redis database at [upstash.com](https://upstash.com).
    - Get your Redis Host, Port, and Password.
3.  **AWS Cognito**:
    - Create a User Pool in the AWS Console.
    - Set up an App Client (Disable "Generate client secret" for simple usage).
    - Note the `Issuer URI`.

### B. VPS Setup (Ubuntu 22.04+)
1.  **Install Docker & Compose**:
    ```bash
    sudo apt update
    sudo apt install docker.io docker-compose -y
    ```
2.  **Configure Nginx**:
    - Use Nginx as a reverse proxy to handle SSL (Let's Encrypt).

---

## 3. Production Configuration

### Optimized `JAVA_OPTS` for 2GB VPS
In your `docker-compose.yml` on the server, use these specific flags:

```yaml
environment:
  JAVA_OPTS: >
    -Xms512m
    -Xmx1024m
    -XX:+UseG1GC
    -XX:+ExitOnOutOfMemoryError
    -Dspring.profiles.active=prod
```

### Essential `application-prod.properties`
```properties
# Tomcat Memory Tuning
server.tomcat.threads.max=50
server.tomcat.threads.min-spare=10

# External Service Integration
spring.datasource.url=jdbc:postgresql://your-neon-host:5432/neondb?sslmode=require
spring.data.redis.host=your-upstash-host
spring.data.redis.port=6379
spring.data.redis.password=your-password

# OAuth2 (AWS Cognito)
spring.security.oauth2.resourceserver.jwt.issuer-uri=https://cognito-idp.[region].amazonaws.com/[user-pool-id]
```

---

## 4. Deployment Workflow

1.  **Build JAR Locally**:
    ```bash
    ./gradlew clean bootJar
    ```
2.  **Transfer to VPS**:
    ```bash
    scp build/libs/app.jar user@vps-ip:/app/
    ```
3.  **Run with Docker**:
    ```bash
    docker-compose -f docker-compose.lean.yml up -d
    ```

---

## 5. Cost Analysis (Monthly)

| Service | Plan | Cost |
| :--- | :--- | :--- |
| **DigitalOcean Droplet** | 1 vCPU / 2GB RAM | ~$6.00 |
| **AWS Cognito** | First 50,000 Users | $0.00 |
| **Neon Postgres** | Shared Tier | $0.00 |
| **Upstash Redis** | Free Tier | $0.00 |
| **Cloudflare R2** | First 10GB | $0.00 |
| **TOTAL** | | **$6.00** |

---

## 6. Security Hardening
-   **UFW**: Only allow ports 80, 443, and 22 (SSH).
-   **SSL**: Use `certbot` for automatic Let's Encrypt certificates.
-   **Non-Root**: Ensure Docker containers run as the `eshop` non-root user (configured in Dockerfile).
