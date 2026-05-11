# E-Shop Server Infrastructure & RAM Summary

## 1. The WSL2 Memory "Scare" (Investigation)

### Initial Observation
- **Process**: `vmmemWSL`
- **Reported Memory**: 36GB – 41GB (Windows Task Manager)
- **Initial Fear**: Production would require extremely high-end servers (16GB+ RAM).

### Actual Findings
- **Real RAM Usage**: ~11GB / 24GB (Windows Performance Tab).
- **Docker Container Usage**: ~1.2GB – 1.5GB total for all backend services.
- **Root Cause**: WSL2 memory reporting includes Linux filesystem cache and reserved virtual memory. It does **not** represent actual physical RAM consumption.

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

## 3. Summary of Actions Taken
- ✅ **Verified** actual RAM usage of containers (~1.5GB).
- ✅ **Analyzed** DigitalOcean cost ($24) vs. Lean Hybrid cost ($5).
- ✅ **Confirmed** files exist on physical disk using `dir`.
- ✅ **Created** this documentation for easy access.
- ✅ **Provided** the key architecture table for copy-pasting.
