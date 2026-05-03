# Environment Variables Configuration Guide

This guide explains how to configure environment variables for the eShop backend application across different deployment environments.

## 📋 Overview

Spring Boot automatically reads environment variables and maps them to configuration properties. We use **application-{profile}.properties** files with placeholders like `${VARIABLE_NAME}` that Spring Boot resolves at runtime.

---

## 🔧 Configuration by Environment

### 1. Local Development

**Using IntelliJ IDEA:**
1. Go to `Run > Edit Configurations`
2. Select your Spring Boot configuration
3. Add Environment Variables:
```
DATABASE_URL=jdbc:postgresql://localhost:5432/eshop_Dev
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=your_password
JWT_SECRET=your_jwt_secret_key
SPRING_PROFILES_ACTIVE=dev
```

**Using VS Code:**
Create `.vscode/launch.json`:
```json
{
  "configurations": [
    {
      "type": "java",
      "name": "Spring Boot",
      "request": "launch",
      "mainClass": "com.eshop.app.EshopApplication",
      "env": {
        "DATABASE_URL": "jdbc:postgresql://localhost:5432/eshop_Dev",
        "DATABASE_USERNAME": "postgres",
        "DATABASE_PASSWORD": "your_password",
        "SPRING_PROFILES_ACTIVE": "dev"
      }
    }
  ]
}
```

**Using Command Line (Windows PowerShell):**
```powershell
$env:DATABASE_URL="jdbc:postgresql://localhost:5432/eshop_Dev"
$env:DATABASE_USERNAME="postgres"
$env:DATABASE_PASSWORD="your_password"
$env:SPRING_PROFILES_ACTIVE="dev"

./gradlew bootRun
```

**Using Command Line (Linux/Mac):**
```bash
export DATABASE_URL=jdbc:postgresql://localhost:5432/eshop_Dev
export DATABASE_USERNAME=postgres
export DATABASE_PASSWORD=your_password
export SPRING_PROFILES_ACTIVE=dev

./gradlew bootRun
```

---

### 2. Docker Compose

**Edit `docker-compose.yml`:**
```yaml
services:
  app:
    image: eshop-backend:latest
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - DATABASE_URL=jdbc:postgresql://postgres:5432/eshop_db
      - DATABASE_USERNAME=postgres
      - DATABASE_PASSWORD=secure_password
      - REDIS_HOST=redis
      - REDIS_PORT=6379
      - KEYCLOAK_ISSUER_URI=http://keycloak:8080/realms/eshop
      - JWT_SECRET=your_jwt_secret_minimum_256_bits
```

**Or use .env file (Docker Compose only):**

Create `.env` file in same directory as `docker-compose.yml`:
```env
SPRING_PROFILES_ACTIVE=docker
DATABASE_URL=jdbc:postgresql://postgres:5432/eshop_db
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=secure_password
JWT_SECRET=your_jwt_secret
```

Then in `docker-compose.yml`:
```yaml
services:
  app:
    env_file:
      - .env
```

**⚠️ Important:** Add `.env` to `.gitignore`!

---

### 3. Production Deployment

#### A. AWS EC2 / Traditional Server

**Set system environment variables:**

**Linux:**
```bash
# Add to /etc/environment or ~/.bashrc
export SPRING_PROFILES_ACTIVE=prod
export DATABASE_URL=jdbc:postgresql://prod-db-host:5432/eshop_prod
export DATABASE_USERNAME=eshop_user
export DATABASE_PASSWORD=super_secure_password
export JWT_SECRET=production_jwt_secret_key_minimum_256_bits
export REDIS_HOST=prod-redis-host
export STRIPE_SECRET_KEY=sk_live_xxxx
export RAZORPAY_KEY_ID=rzp_live_xxxx
export KEYCLOAK_ISSUER_URI=https://auth.yourdomain.com/realms/eshop
```

**Run application:**
```bash
java -jar eshop-backend.jar
```

#### B. AWS Elastic Beanstalk

Add environment variables in AWS Console:
1. Go to Configuration > Software
2. Add Environment Properties:
   - `SPRING_PROFILES_ACTIVE` = `prod`
   - `DATABASE_URL` = `jdbc:postgresql://...`
   - `DATABASE_USERNAME` = `eshop_user`
   - etc.

#### C. AWS ECS / Fargate

**In Task Definition JSON:**
```json
{
  "containerDefinitions": [
    {
      "name": "eshop-backend",
      "environment": [
        {"name": "SPRING_PROFILES_ACTIVE", "value": "prod"},
        {"name": "DATABASE_URL", "value": "jdbc:postgresql://..."}
      ],
      "secrets": [
        {
          "name": "DATABASE_PASSWORD",
          "valueFrom": "arn:aws:secretsmanager:region:account:secret:db-password"
        },
        {
          "name": "JWT_SECRET",
          "valueFrom": "arn:aws:secretsmanager:region:account:secret:jwt-secret"
        }
      ]
    }
  ]
}
```

#### D. Azure App Service

**Using Azure CLI:**
```bash
az webapp config appsettings set --name eshop-backend \
  --resource-group eshop-rg \
  --settings \
    SPRING_PROFILES_ACTIVE=prod \
    DATABASE_URL=jdbc:postgresql://... \
    DATABASE_USERNAME=eshop_user
```

**Using Azure Portal:**
1. Go to App Service > Configuration > Application Settings
2. Add New Application Setting for each variable

#### E. Kubernetes

**Create ConfigMap:**
```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: eshop-config
data:
  SPRING_PROFILES_ACTIVE: "prod"
  DATABASE_URL: "jdbc:postgresql://postgres-service:5432/eshop_prod"
  DATABASE_USERNAME: "eshop_user"
  REDIS_HOST: "redis-service"
```

**Create Secret:**
```yaml
apiVersion: v1
kind: Secret
metadata:
  name: eshop-secrets
type: Opaque
stringData:
  DATABASE_PASSWORD: "super_secure_password"
  JWT_SECRET: "production_jwt_secret_key"
  STRIPE_SECRET_KEY: "sk_live_xxxx"
```

**Use in Deployment:**
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: eshop-backend
spec:
  template:
    spec:
      containers:
      - name: eshop-backend
        image: eshop-backend:latest
        envFrom:
        - configMapRef:
            name: eshop-config
        - secretRef:
            name: eshop-secrets
```

---

## 🔐 Secrets Management Best Practices

### 1. AWS Secrets Manager
```java
// Spring Cloud AWS automatically integrates
// Add to pom.xml: spring-cloud-starter-aws-secrets-manager-config
// Secrets are loaded at startup
```

### 2. Azure Key Vault
```java
// Add to pom.xml: azure-spring-boot-starter-keyvault-secrets
// Configure in application.properties:
// azure.keyvault.uri=https://your-vault.vault.azure.net/
```

### 3. HashiCorp Vault
```java
// Add to pom.xml: spring-cloud-starter-vault-config
// Configure in bootstrap.properties
```

---

## 📝 Required Environment Variables

### Core Application
```
SPRING_PROFILES_ACTIVE=dev|test|prod|docker
```

### Database
```
DATABASE_URL=jdbc:postgresql://host:5432/database_name
DATABASE_USERNAME=username
DATABASE_PASSWORD=password
```

### Redis Cache
```
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=optional_password
```

### Authentication (Keycloak)
```
KEYCLOAK_ISSUER_URI=http://keycloak:8080/realms/eshop
KEYCLOAK_JWK_URI=http://keycloak:8080/realms/eshop/protocol/openid-connect/certs
```

### JWT (if not using Keycloak)
```
JWT_SECRET=minimum_256_bits_random_string
JWT_EXPIRATION=3600000
JWT_REFRESH_EXPIRATION=604800000
```

### Payment Gateways (Production)
```
STRIPE_SECRET_KEY=sk_live_xxxx
STRIPE_PUBLIC_KEY=pk_live_xxxx
RAZORPAY_KEY_ID=rzp_live_xxxx
RAZORPAY_KEY_SECRET=xxxx
```

### CORS
```
CORS_ORIGINS=https://yourdomain.com,https://www.yourdomain.com
```

---

## ✅ Security Checklist

- [ ] Never commit `.env` files with real credentials to Git
- [ ] Always use `.env.example` as a template
- [ ] Use secrets managers in production (AWS Secrets Manager, Azure Key Vault)
- [ ] Rotate secrets regularly
- [ ] Use different credentials for dev/test/prod
- [ ] Restrict database access by IP/network
- [ ] Use IAM roles instead of hardcoded credentials when possible
- [ ] Enable encryption at rest and in transit
- [ ] Monitor secret access logs
- [ ] Use least privilege principle for service accounts

---

## 🔗 References

- [Spring Boot External Configuration](https://docs.spring.io/spring-boot/reference/features/external-config.html)
- [AWS Secrets Manager Integration](https://docs.awspring.io/spring-cloud-aws/docs/current/reference/html/index.html#integrating-your-spring-cloud-application-with-the-aws-secrets-manager)
- [Azure Key Vault Integration](https://learn.microsoft.com/en-us/azure/developer/java/spring-framework/configure-spring-boot-starter-java-app-with-azure-key-vault)
- [12-Factor App: Config](https://12factor.net/config)
