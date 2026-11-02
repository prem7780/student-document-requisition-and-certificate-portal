> **DCCMP 2.0 implementation:** The source now includes Student Directory CSV/Excel import, independent certificate services, multi-visit originals handover and related test cases. Read [README_DCCMP2.md](README_DCCMP2.md) for updated behavior, setup and verification status. The rest of this file documents the original No Dues workflow.

# DCCMP — Implemented Staff and Principal Dashboard Update

**Source edition: October 2026.** This is an update of the supplied `DCCMP.zip`, not a clean-room replacement.

## New Staff Dashboard

Five pages (Overview, Pending Approvals, All Requests, Decision History, Student Search); responsive collapsible mobile navigation; scoped student-detail drawers; selectable transactional bulk approval (1–10 records); confirmation for individual approval/rejection; optional approval and mandatory rejection remarks; server-paginated 10-row queues; manual refresh; department-only fields; contact Copy controls; read-only analytics charts; historical decision cycles and Admin reopening history; IST display and analytics grouping; 30-minute inactivity handling; image CAPTCHA after repeated login failures; shared-account session invalidation; account password changes.

## New Principal Dashboard

Six pages (Overview, Applications, Department Analytics, Student Search, Reports, Approval History), all 17 mandatory department summaries, aggregate report exports (PDF, Excel and numerical CSV), read-only completed application views, and final approval only after all departmental approvals.

## Security and migrations

- Staff request/search/detail endpoints return **only the requesting department's** fields; the legacy broad staff queue and staff Alumni access endpoints have been removed.
- Student-facing approval view omits employee names and identifiers.
- V3 creates immutable department-decision events and account session keys; V4 backfills older recorded audit decisions. **Back up your existing MySQL database before startup and test migrations against a copy.**
- A new account login invalidates earlier account sessions; password changes and resets invalidate active sessions. Multiple tabs in the same browser login continue sharing the same session.
- Export endpoints are restricted to Principal and Admin. Shared department accounts cannot export records.

## Verification status and limitations

Java source was parsed with the JDK 21 compiler parser and React files were parsed with Babel. **This is source-level verification only.** A full Maven compile, Vite production build, MySQL migration, SMTP/PDF workflow, and live integration tests could not be executed in the supplied environment (Maven and compatible Linux npm native packages were unavailable). Do not treat this bundle as deployment-verified.

The spreadsheet export contains valid OOXML numerical tables but does not yet provide a native embedded Excel chart; PDF exports contain a compact activity visualization. Existing MySQL `DATETIME` records use explicit IST interpretation rather than an offset-bearing storage column; migrating historical timestamps to UTC instants would require a carefully planned data migration. The frontend and database integration still need to be tested with real data. The server-side analytics service uses aggregation over existing department data and should be profiled at campus scale.

## Run and verify

Use `docker compose up -d`, start the backend with Java 21/Maven (`cd backend && mvn spring-boot:run`) and the frontend with Node 22 (`cd frontend && npm ci && npm run dev`). All backend endpoints are under `/api`. See the original setup below for environment configuration.

---

# DCCMP — Digital Campus Clearance and Certificate Management Platform

A full-stack source project for **Usharama College of Engineering and Technology** using Java 21, Spring Boot 4, Spring Security, Spring Data JPA, Flyway, MySQL 8, React + Vite + Tailwind CSS, Spring Mail, PDFBox, and ZXing.

> **Verification status:** This source bundle was generated in an environment without npm-registry connectivity, Maven, or Docker. Automated source and consistency checks can run here; a full build, live MySQL migration, email delivery, and end-to-end walkthrough **must be executed on your PC**. See `PROGRESS.md`. Do not deploy to real students before testing, authorization review, and SMTP configuration.

## Features implemented in the source

- Student roll number ↔ official `@usharama.in` email validation, single-use expiring BCrypt-hashed OTP, rate-limited resend, session-based student authorization.
- Official Alumni Registration (includes five friends) before No Dues application.
- All **17 departments** approve in parallel, each with department-scoped staff authorization.
- Rejections with required remarks, administrator-authorized reopening, audit trail and preserving other approvals.
- Principal final approval only after 17/17, with Principal rejection and reopening.
- Official No Dues PDF template included; overlays student details and appends a second verification page showing all 17 digital approval labels, names, dates, Principal approval and QR verification ID. No fake signatures.
- SMTP OTP and PDF attachment delivery; Admin can retry failed delivery without undoing approval.
- Student, department staff, Principal and Admin React dashboards.
- Admin accounts, reports, certificate collection checklist, physical verification ID match and one-time handover record.
- Local container setup for MySQL 8 + Mailpit.

## Requirements (Windows 10/11)

1. Install **Java 21** and check `java -version`.
2. Install **Apache Maven 3.9+** and check `mvn -version`.
3. Install **Node.js 22+** and check `node -v`.
4. Install **Docker Desktop** (recommended for local MySQL + Mailpit) or your own MySQL 8 server.
5. Set strong administrator credentials and connect authorized SMTP for live college use.

## Quick start (three terminals)

From the **root project folder**, start local MySQL + Mailpit:

```powershell
docker compose up -d
```

Set credentials in **PowerShell** (change passwords, demo mode is optional):

```powershell
$env:DB_URL = 'jdbc:mysql://localhost:3306/dccmp_db?createDatabaseIfNotExist=true'
$env:DB_USER = 'dccmp'
$env:DB_PASSWORD = 'dccmp_local_only'
$env:ADMIN_CODE = 'ADMIN001'
$env:ADMIN_PASSWORD = 'ReplaceWithUniqueStrongAdminPassword_2026'
$env:APP_BASE_URL = 'http://localhost:5173'
$env:SMTP_HOST = 'localhost'
$env:SMTP_PORT = '1025'
$env:SMTP_AUTH = 'false'
$env:SMTP_STARTTLS = 'false'
# OPTIONAL for local demonstrations: automatically provision 17 staff + Principal.
$env:DEMO_SEED_STAFF = 'true'
$env:DEMO_STAFF_PASSWORD = 'ReplaceWithUniqueStrongDemoPassword_2026'
cd backend
mvn spring-boot:run
```

In another terminal:

```powershell
cd frontend
npm install
npm run dev
```

Open:
- **Student portal:** http://localhost:5173/student
- **Employee login:** http://localhost:5173/employee
- **Mailpit local test inbox:** http://localhost:8025
- **Health:** http://localhost:5173/api/public/health
- **Swagger/OpenAPI:** http://localhost:8080/swagger-ui.html

Login as `ADMIN001` using the `ADMIN_PASSWORD` value. In optional local seeding mode the shared department accounts are named after the department code in uppercase, e.g. `HOD_CSE` and `LIBRARY`; the Principal is `PRINCIPAL001`. The shared accounts use `DEMO_STAFF_PASSWORD` initially. **For real deployments disable automatic seeding, reset these passwords through Admin, and retain the 17 permanent active department accounts.**

### SMTP note

Mailpit receives real SMTP messages, but it does not deliver to a live `@usharama.in` mailbox. During local testing, the student receives their six-digit OTP in Mailpit's web inbox. For genuine campus delivery, replace `SMTP_HOST`, `SMTP_PORT`, `SMTP_USER`, `SMTP_PASSWORD`, `SMTP_AUTH`, `SMTP_STARTTLS`, and `MAIL_FROM` with *authorized* SMTP credentials and test with the campus IT team. SMTP relay acceptance is not a guarantee of end-user inbox delivery.

### Essential development commands

```powershell
# Backend
cd backend
mvn test
mvn clean package

# Frontend
cd frontend
npm install
npm run check
npm run build
```

## Demonstration checklist

1. Start database, Mailpit, backend and frontend.
2. Request OTP with fictional test roll `23NG1A9999` and email `23ng1a9999@usharama.in`; read code in Mailpit.
3. Verify OTP and complete Alumni Registration with exactly five friends.
4. Submit No Dues and confirm exactly 17 PENDING approvals in student tracking.
5. Log in separately as each of the 17 department staff, approve from each department dashboard (or try rejection, reopening and reapproval first).
6. Principal sees the application only after 17/17; approves.
7. Confirm download of the issued official-format PDF with second-page approval annex, verification ID and QR.
8. Check Mailpit PDF attachment; inspect public `/verify/<verification-id>` page.
9. Admin opens the application, verifies each physical document, types the verification ID from the printed form and records certificate handover.
10. Confirm duplicate handover is blocked and student tracking shows handed-over.

## Notes on official templates

`reference-forms/Alumni_Registration(1).pdf` and `reference-forms/No_Dues(2).pdf` are provided in the ZIP. The No Dues original form predates the updated internal workflow; PDF generation preserves page one of the official form and adds a detailed 17-approval annex. Student signature remains physically provided. AO sign-off is a physical Admin action only.

## Security considerations before real deployment

The project uses session attributes checked server-side and Spring Security CSRF protection; frontend uses a same-origin Vite proxy. Use HTTPS (`SESSION_COOKIE_SECURE=true`), restrict access to generated PDF files, use real campus SMTP credentials, disable demo accounts, implement production monitoring/backups, review PII handling/retention, add stronger distributed OTP/login throttling, and conduct penetration/integration tests before real deployment. The public verification endpoint intentionally exposes only issuance and collection metadata.

## Detailed documentation

- `DCCMP_Project_Plan.md` — authoritative functional specification
- `DCCMP_Codex_Master_Prompt.md` — original agent prompt
- `docs/API.md` — endpoint and request examples
- `docs/ARCHITECTURE.md` — data model/workflow
- `PROGRESS.md` — test verification and known limitations


## Backend package organization

The backend follows Spring Boot's layered package structure under `backend/src/main/java/com/dccmp/backend/`:

```text
com.dccmp.backend/
├── DccmpApplication.java           # Spring Boot entry point
├── config/                         # Security setup and initialization
├── controller/                     # REST endpoints grouped by user role
│   ├── PublicController.java
│   ├── AuthController.java
│   ├── StudentController.java
│   ├── StaffController.java
│   ├── PrincipalController.java
│   └── AdminController.java
├── dto/                            # API request and response records
├── entity/                         # JPA database entities
├── repository/                     # Spring Data JPA repositories
├── service/                        # Business rules, email, PDF, verification
├── security/                       # Session-based role checks
└── exception/                      # Global API exception handling
```

Controllers handle HTTP requests, services implement business logic, repositories query the database, entities map tables, and DTOs represent request/response objects. This refactor keeps the existing API URLs and React frontend unchanged.
