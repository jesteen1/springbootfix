# Kumar Electrical Contractor — Spring Boot + PostgreSQL Backend

Java 17, Spring Boot 3.4, Maven, Spring Security, JPA/Hibernate, and **PostgreSQL**. Ready for 1-click cloud deployment on **Render** with managed PostgreSQL.

---

## 🚀 Quick Deployment on Render with PostgreSQL

You have two simple options to deploy to Render:

### Option A: 1-Click Render Blueprint (Recommended)
1. Push your repository to GitHub or GitLab.
2. Log in to [Render Dashboard](https://dashboard.render.com).
3. Click **New +** → **Blueprint**.
4. Connect your repository.
5. Render will automatically detect [`render.yaml`](render.yaml) and create:
   - **PostgreSQL Database** (`kumar-electrical-db`)
   - **Web Service** (`kumar-electrical-backend`)
   - Auto-link all database credentials and environment variables!
6. Click **Apply**. Render will build the Docker container and start your service.

---

### Option B: Manual Setup on Render Dashboard

#### Step 1: Create PostgreSQL Database on Render
1. In Render Dashboard, click **New +** → **PostgreSQL**.
2. Give it a name (e.g., `kumar-electrical-db`), database name `kumar_electrical`, and user `kumar_admin`.
3. Choose the **Free** plan and select your preferred region (e.g., Singapore / Oregon).
4. Click **Create Database**.
5. Once created, copy the **Internal Database URL** (e.g., `postgres://kumar_admin:password@dpg-...:5432/kumar_electrical`).

#### Step 2: Create Web Service on Render
1. In Render Dashboard, click **New +** → **Web Service**.
2. Connect your repository.
3. Select **Docker** as the runtime (Render will automatically use the included [`Dockerfile`](Dockerfile)).
4. Choose the **Free** plan in the **same region** as your database.
5. Under **Environment Variables**, add:
   - `DATABASE_URL`: Paste the Internal Database URL (or connection string) from Step 1.
   - `CORS_ALLOWED_ORIGINS`: `*` (or your frontend URL, e.g. `https://your-frontend.onrender.com`).
   - `ADMIN_USERNAME`: `ADMIN`
   - `ADMIN_PASSWORD`: Your chosen secure admin password.
   - `EMPLOYEE_DEFAULT_PASSWORD`: Your chosen default employee password (e.g. `emp123`).
6. Click **Create Web Service**.

Your backend will now build and start. Health check endpoint: `https://your-service.onrender.com/api/health`.

---

## 💻 Local Development Setup

### 1. Requirements
- JDK 17 or newer
- Maven 3.9+
- PostgreSQL 14+ installed locally (or access to Render external PostgreSQL URL)

### 2. Environment Variables (.env)
Copy [`.env.example`](.env.example) to `.env`:

```bash
# For local PostgreSQL:
DATABASE_URL=jdbc:postgresql://localhost:5432/kumar_electrical
DB_USER=postgres
DB_PASSWORD=postgres

# Server & CORS:
PORT=8080
CORS_ALLOWED_ORIGINS=*
```

> **Note on Render External Connections:**  
> If you connect to Render PostgreSQL from your local computer, use the **External Database URL** (which ends with `.render.com` and includes `?sslmode=require`). The internal hostname (`dpg-xxxxxx-a`) only resolves from within Render's private network.

### 3. Run Backend
```bash
mvn spring-boot:run
```
Check health: `http://localhost:8080/api/health` should return `{"status":"UP", "service":"Kumar Electrical Contractor API"}`.

---

## 🔑 Authentication & Demo Credentials
- **Admin**: `ADMIN` / `admin123` (or whatever `ADMIN_PASSWORD` is configured)
- **Employee**: `EMP001`, `EMP002`, or `EMP003` / `emp123` (or `EMPLOYEE_DEFAULT_PASSWORD`)

---

## 📡 REST Endpoints

| Method | Endpoint | Purpose |
|---|---|---|
| GET | `/api/health` | Health check |
| POST | `/api/auth/login` | Validate credentials and issue bearer token |
| POST | `/api/auth/logout` | Revoke token |
| GET | `/api/auth/me` | Current authenticated user |
| GET | `/api/state` | Load full state for admin or employee-filtered state |
| PUT | `/api/state` | Admin-only persist state (employees, sites, attendance, etc.) |
| GET | `/api/employees` | Employee list |
| GET | `/api/sites` | Worksites visible to user |
| GET | `/api/attendance` | Attendance records |
| GET | `/api/assignments` | Site assignments |
| GET | `/api/projects` | Projects list |
| GET | `/api/payroll` | Payroll records (admin only) |
| POST | `/api/attendance/verify-location` | Geo-fence distance verification |
