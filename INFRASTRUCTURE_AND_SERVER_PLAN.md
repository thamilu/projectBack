# E-Shop Infrastructure & Server Plan [MASTER DOCUMENT]

This document combines the investigation of local RAM usage with the final production deployment strategy.

---

## 1. Local Investigation: The WSL2 Memory "Scare"

### Initial Concern
- **Observation**: `vmmemWSL` process showing 36GB–41GB in Windows Task Manager.
- **Fear**: Production server would require expensive high-RAM instances.

### Investigation Findings
- **Actual Physical RAM Used**: ~11GB / 24GB.
- **Docker Container Usage**: ~1.2GB–1.5GB total.
- **Conclusion**: The high numbers in Task Manager are "Virtual Memory" ballooning caused by WSL2's filesystem cache. Your backend is actually very lightweight.

### Local Fix (WSL Config)
Created `.wslconfig` to limit local RAM usage:
```ini
[wsl2]
memory=6GB
processors=4
swap=2GB
pageReporting=true
autoMemoryReclaim=gradual
```

---

## 2. Production Strategy: The "Lean Hybrid" Model

To deploy on a **$5/month (2GB RAM)** VPS without crashing, we offload heavy services to free-tier managed providers.

### Architecture Table
| Component | Recommended Service | RAM on VPS | Cost (Monthly) |
| :--- | :--- | :--- | :--- |
| **Auth** | **AWS Cognito** (50k Free Users) | **0 MB** | $0.00 |
| **Database** | **Neon.tech** (Serverless Postgres) | **0 MB** | $0.00 |
| **Cache** | **Upstash** (Serverless Redis) | **0 MB** | $0.00 |
| **Backend** | **Spring Boot** (on 2GB VPS) | **~1.2 GB** | ~$5.00 |
| **Frontend** | **Vercel / Cloudflare Pages** | **0 MB** | $0.00 |
| **TOTAL** | | **~1.2 GB** | **~$5.00** |

---

## 3. Implementation Roadmap

### Step 1: Managed Services
- Move Auth from Keycloak to **AWS Cognito**.
- Move DB from local Docker to **Neon.tech**.
- Move Cache from local Docker to **Upstash**.

### Step 2: VPS Configuration
- Use a **2GB RAM Ubuntu VPS** (DigitalOcean Basic or Hetzner).
- Set JVM Heap: `-Xmx1024m`.
- Reduce Tomcat threads: `server.tomcat.threads.max=50`.

---

## 4. Summary of Actions Taken
- ✅ **Verified** actual RAM usage of containers (~1.5GB).
- ✅ **Analyzed** DigitalOcean cost ($24) vs. Lean Hybrid cost ($5).
- ✅ **Confirmed** files exist on physical disk using `dir`.
- ✅ **Created** this root-level master document for easy access.
- ✅ **Provided** the key architecture table for copy-pasting.

---

## 5. Final Recommendation
The project architecture is **not** memory-heavy. Do not pay for a high-RAM server yet. Use the **Lean Hybrid** model to stay under **$6.00/month** while maintaining professional performance.
