# DCCMP 2.0 — Implementation Package

This package extends the previous DCCMP clearance application. **The 17-department No Dues workflow is retained.** Certificate Services is a separate repeatable request workflow.

## Implemented in source

- Admin CSV/XLSX Student Directory import; required Roll Number, Student Name, Branch, Email, optional Batch; partial success, errors CSV, automatic updates and audit table. Uses Apache Commons CSV and Apache POI.
- Email OTP checks imported roll/email mapping first, falling back to `rollnumber@usharama.in` only when no imported mapping exists. Official-college email restriction is retained.
- Imported name/branch prefilled and read-only in Alumni and No Dues forms. Certificate routing branch remains freely selected.
- No Dues submission now requires that the student has never submitted it previously, using a student row lock for concurrent protection. Existing historical applications are not deleted.
- Fixed certificate catalogue: Study, Bonafide, LOR, Duplicate TC, Conduct.
- Independent certificate request record with one active request of each type per student, one copy per request.
- Selected HOD -> Principal; individual approval/rejection, mandatory rejection remarks; no certificate bulk approval.
- Principal approval triggers collection email. HOD/Principal rejection triggers rejection email. SMTP failures are logged without undoing the application decision; Admin may resend.
- Admin can cancel uncollected requests with reason, or verify student in person and hand over exactly one physical certificate.
- Scheduled 25-day reminder and 30-day expiration; collection checks expiry even if scheduler is delayed.
- Dedicated student certificate UI, HOD/Principal queues, Admin certificate management, statistics only (no certificate exports).
- Customizable expected originals initialized on first collection visit, partial handover across visits, quantity checks, auditable changes. Existing No Dues PDF continues to be required.
- New unit-test source, Selenium screenshot-evidence starter, and a test-case plan.

## IMPORTANT: test / migration prerequisites

**This source has undergone Java syntax parsing and React JSX parsing, not a complete application integration run in the current environment.** Maven, installed Maven artifacts, a connected MySQL 8 instance, Gmail SMTP test mailbox, and compatible Linux frontend Rollup native module were not available here. Compile and test locally before treating it as production-ready.

1. Back up your existing database (`mysqldump`) and keep an untouched copy of the old project.
2. Install JDK 21, Maven, Node.js and MySQL 8. Set backend `.env` values for DB, SMTP, sender, base URL and admin credentials. Use the original `.env.example` as a guide.
3. From `backend`, run `mvn clean test` then `mvn spring-boot:run`; Flyway will apply `V5__dccmp2_services.sql` to an existing schema after V1–V4.
4. From `frontend`, run `npm ci` then `npm run build` and `npm run dev`. Run this on Windows or Linux with the appropriate native optional dependencies installed by npm.
5. Verify login for all roles, Student Directory CSV/XLSX upload, mapped-email OTP, certificate approvals, email failures and retries, partial original handover, and old No Dues issuance.
6. Follow `docs/DCCMP2_TEST_PLAN.md`. Record real screenshots only after running tests. Add expected/actual and pass/fail to the HOD report.

### Database compatibility

V5 is **additive**: existing tables and approval history remain. The No Dues one-time invariant is enforced in the service with a student-row lock because old databases may already contain multiple historical applications; imposing a new unique index without data reconciliation would make Flyway fail or risk destroying history. Before production rollout, audit historical duplicates and decide on safe archival policies. Do not delete old approvals without institutional authorization.

### API summary

| Endpoint | Role | Purpose |
|---|---|---|
| `POST /api/admin/directory/upload` | Admin | Multipart CSV/XLSX import |
| `GET /api/admin/directory` | Admin | Search imported students |
| `GET /api/admin/directory/imports/{id}/errors` | Admin | Rejected-row CSV |
| `GET /api/certificates/types` | Student | Fixed certificate list |
| `GET /api/certificates/branches` | Student | Available HOD branches |
| `POST /api/student/certificates` | Student | Submit request |
| `GET /api/student/certificates` | Student | My requests |
| `GET /api/student/certificates/{id}` | Student | Own details and history |
| `GET /api/staff/certificates` | HOD | Branch queue |
| `PATCH /api/staff/certificates/{id}/approve|reject` | HOD | Individual decision |
| `GET /api/principal/certificates` | Principal | Certificate queue |
| `PATCH /api/principal/certificates/{id}/approve|reject` | Principal | Final decision |
| `GET /api/admin/certificates` | Admin | Monitor all requests |
| `PATCH /api/admin/certificates/{id}/cancel` | Admin | Cancel with remarks |
| `POST /api/admin/certificates/{id}/collect` | Admin | Verify and hand over one |
| `POST /api/admin/certificates/{id}/resend?kind=...` | Admin | Retry notification |
| `GET /api/admin/original-collections/{appId}` | Admin | Expected/collected/remaining |
| `POST /api/admin/original-collections/{appId}/initialize` | Admin | First-visit checklist |
| `POST /api/admin/original-collections/{appId}/visits` | Admin | Partial handover |

### Status definitions

Certificate statuses: `PENDING_HOD`, `PENDING_PRINCIPAL`, `APPROVED`, `REJECTED`, `CANCELLED`, `COLLECTED`, `EXPIRED`.

### What this module deliberately does not do

No generation of physical Study/Bonafide/LOR/TC/Conduct PDFs, no certificate uploads, no student self-cancellation, no certificate analytics exports, no representative handovers, no bulk certificate approvals. All individual certificates are physically handed over by Admin after Principal approval.
