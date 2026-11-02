# DCCMP — Final Project Plan

**Project:** Digital Campus Clearance and Certificate Management Platform (DCCMP)  
**Institution:** Usharama College of Engineering and Technology  
**Build strategy:** Fresh repository; complete, integrated demonstration target in a 24-hour Codex-assisted sprint  
**Stack:** Java 21, Spring Boot 4.x, Maven, Spring Security, Spring Data JPA, MySQL 8, Flyway, React, Vite, Tailwind CSS, Spring Boot Mail, Apache PDFBox, ZXing.

> **Delivery standard:** Working software and verified integration tests, not generated code alone. Twenty-four hours is an ambitious target, not a guarantee. Record anything incomplete rather than claiming it works.

## 1. Business goal and non-negotiable workflow

1. Student enters a valid roll number and verifies the corresponding `@usharama.in` email using a real emailed OTP.
2. Student completes **Alumni Registration first**.
3. Student submits a separate **No Dues application**. Only then are 17 departmental decisions created as `PENDING`.
4. **All 17 departments act in parallel**; every student requires every department's approval, regardless of branch.
5. Rejections require remarks, block advancement, and can be reopened following Admin authorization. Existing approvals are preserved and all changes audited.
6. When **all 17** have approved, the **Principal** alone may make the final approval/rejection decision.
7. After Principal approval, generate a printable, college-format No Dues PDF with **digital approval labels, approver names and timestamps**, a unique verification ID and QR code; email the PDF to the student.
8. Student prints the PDF and visits the college with physical photograph and supporting documents.
9. **Admin performs the AO physical verification**, completes a document checklist, and records handover of the **original certificates**. No separate AO digital approval role exists.

The PDF is a **clearance/collection form**, not an original certificate. Principal approval, PDF generation, email delivery, physical verification and handover must remain separately tracked.

## 2. Authoritative official forms

Place the two supplied source PDFs in `reference-forms/` in the Codex workspace:

- `Alumni_Registration(1).pdf` — **URCET/7.22/AC 08**: name, father name, roll/hall ticket number, year of study, course/branch, permanent communication address, mobile, email, hobbies, academic achievements (GRE/TOEFL/GATE/IES), awards, placement details, **five best friends' names and phone numbers**, job details/address, future plans. The original has a recent color photograph box; **do not request an online photo upload**.
- `No_Dues(2).pdf` — **URCET/5.4/FT 01**: department name, student name, roll number, address, phone/mobile, email, HOD approval table, general-department Due/No Due table, student declaration and signature area, AO/date and Principal/date. Preserve the recognizable form and document code; adapt it to the updated 17-department list.

The official No Dues instructions specify physical enclosure of: original student ID card (or Rs. 50 lost-ID payment receipt), original certificates deposit receipt (or affidavit if lost), and photocopies of all semester mark sheets. The college's physical process also requires a photograph. Admin checks these in person; **no digital supporting-document uploads**.

## 3. Exactly 17 required departments

| Code | Department |
|---|---|
| `hod_eee` | EEE HOD |
| `hod_civil` | Civil HOD |
| `hod_ece` | ECE HOD |
| `hod_cse` | CSE HOD |
| `hod_mech` | Mechanical HOD |
| `hod_ai` | AI HOD |
| `hod_it` | IT HOD |
| `hostel` | Hostel |
| `canteen` | Canteen |
| `accounts` | Accounts |
| `scholarship` | Scholarship |
| `training_placements` | Training & Placements |
| `examinations` | Examination Section |
| `sports` | Games & Sports |
| `alumni` | Alumni |
| `library` | Library |
| `special_team` | Special Team |

Seed exactly these codes using a Flyway migration. An application has a unique `(application_id, department_id)` approval row for each required department. No shortcut for a student's own branch.

## 4. Users and permissions

**Student:** No pre-created account/password. Verify roll number and official email via OTP, submit both forms in order, track own status, download own approved PDF. Never authorize by roll number alone.

**Staff:** Login with unique staff code and password (BCrypt). Assigned department can review/approve/reject only its own requests; mandatory rejection remarks. **Alumni staff** may view all Alumni Registration details, including friends' contact information. Other staff should receive only the data necessary to process No Dues.

**Principal:** Distinct role, staff-code/password login. Can review and decide only applications cleared by all 17 departments. Mandatory rejection remarks.

**Admin:** Manages employee accounts, department assignments, monitoring, reports, audit history, reopening authorizations, physical verification and handover. **Cannot approve on behalf of Staff or Principal.** Acts as AO for physical collection only.

## 5. Identity validation and email

- Regular roll example `23NG1A0501`; provisional regex `^[0-9]{2}NG1A[0-9]{4}$`.
- Lateral roll example `24NG5A0501`; provisional regex `^[0-9]{2}NG5A[0-9]{4}$`.
- Normalize roll uppercase and require email to equal `lowercase(roll) + '@usharama.in'`, case-insensitively.
- Make roll formats configurable in case the institution confirms additional programs.
- OTPs: cryptographically random, hashed in storage, expiring, one-time, attempt-limited, resend cooldown, endpoint rate limiting.
- SMTP: Spring Boot Mail, credentials in environment variables, TLS as appropriate. Send real OTPs and approved PDF attachments; do not fake production delivery. Track email status and retry safely after transaction commit. An SMTP failure must **not** undo Principal approval.

## 6. Database architecture

Create MySQL database `dccmp_db`; use versioned Flyway migrations, FK constraints, indexes and unique constraints.

| Table | Core data / constraints |
|---|---|
| `student_identities` | unique normalized roll, unique official email, verified timestamp |
| `email_otps` | email, OTP hash, expiry, attempts, consumed timestamp |
| `alumni_registrations` | student FK, all official form fields, submitted timestamp |
| `alumni_friends` | alumni FK, 1–5 position, friend name, phone; exactly five at final submission |
| `no_dues_applications` | student FK, alumni FK, form fields, approval state, submission timestamp |
| `departments` | unique code, display name, active flag |
| `staff` | unique staff code, BCrypt password hash, role, assigned department, active flag |
| `department_approvals` | application FK, department FK, assigned decision actor, state, remarks, time, optimistic version; unique application+department |
| `principal_approvals` | application FK, principal actor, state, remarks, decision time; preserve revisions via audit |
| `approval_reopenings` | approval/principal target, authorizing admin, reason, timestamp |
| `audit_events` | actor, action, entity, old/new state, timestamp; append-only via application logic |
| `generated_forms` | application, unpredictable unique verification ID, protected file location, issue time, active/revoked state |
| `email_deliveries` | message type, recipient, delivery state, attempt count, last error, sent time |
| `certificate_handovers` | application, verifying admin, verification/handover timestamps, remarks; prevent duplicates |
| `handover_checklist_items` | handover FK, document type, verified flag, alternate-document reason |

### State modeling

Use separate dimensions rather than a single misleading status:

- **Application approval:** `PENDING_DEPARTMENTS`, `ACTION_REQUIRED`, `PENDING_PRINCIPAL`, `PRINCIPAL_ACTION_REQUIRED`, `APPROVED`.
- **Department:** `PENDING`, `APPROVED`, `REJECTED`.
- **Principal:** `PENDING`, `APPROVED`, `REJECTED`.
- **Delivery:** `NOT_GENERATED`, `GENERATED`, `EMAIL_PENDING`, `EMAIL_SENT`, `EMAIL_FAILED`.
- **Handover:** `NOT_COLLECTED`, `VERIFICATION_PENDING`, `HANDED_OVER`.

Use transactions and optimistic locking to avoid duplicate/concurrent decisions. A rejection preserves all other departments' approvals. Reopening requires Admin authorization; the original department/Principal redecides.

## 7. API architecture

Base package `com.dccmp.backend`, organized into `config`, `security`, `entity`, `repository`, `dto`, `service`, `controller`, `exception`, `mapper`, `util`. DTOs only at API boundaries; validation and centralized error handling.

| Endpoint family | Key operations |
|---|---|
| `/api/public/otp` | send, verify; rate-limited |
| `/api/student/alumni` | create/read own Alumni Registration |
| `/api/student/no-dues` | submit and track own application |
| `/api/student/approved-form` | authenticated PDF download |
| `/api/auth` | employee login, logout, profile |
| `/api/staff/approvals` | department queue, approve, reject, history |
| `/api/staff/alumni` | Alumni staff-only detailed registration view |
| `/api/principal/applications` | ready queue, approve/reject, history |
| `/api/admin/staff` | employee management |
| `/api/admin/departments` | department management, without silently changing the 17-required snapshot |
| `/api/admin/reopenings` | authorize corrections |
| `/api/admin/handovers` | physical checklist and handover |
| `/api/admin/reports` | counts, audit, PDF/email status |
| `/api/public/verify/{verificationId}` | minimal public authenticity result, no sensitive personal details |

Implement server-side role/department/record-level authorization on every endpoint. Prefer secure HttpOnly cookie-based sessions, appropriate CSRF defenses, and HTTPS for deployment. Passwords and secrets never in the repository.

## 8. Frontend modules

- Public landing and employee login.
- Student roll/email entry, OTP verification, Alumni Registration, No Dues form, status timeline, approved PDF and physical collection instructions.
- Staff dashboard with assigned-department pending/decided lists, review details, approve/reject with remarks; Alumni staff has extended Alumni form access.
- Principal dashboard with only fully cleared applications, full decision history and final approve/reject.
- Admin dashboard with statistics, applications, employee/department management, reopenings, email/PDF status, physical verification checklist, handover, reports and audit history.
- Public QR verification page with minimal information.

Use React Router, accessible forms, responsive layouts, loading/error states, clear status badges, search/filter/pagination. No fake production data.

## 9. PDF and QR strategy

Use PDFBox to populate or reproduce the supplied No Dues form while maintaining its official code and appearance. Show **all 17 departments**, digital approval label, approver name, timestamp and Principal approval. Do **not** generate fabricated signatures or stamps. Include an unpredictable verification ID and ZXing QR linking to a minimal verification page. Leave AO physical-verification area pending until collection; physical verification is recorded by Admin in the system. Ensure PDF is generated **only** after Principal approval and is accessible only to authorized users. Generation should be idempotent and file storage protected.

## 10. Physical collection checklist

Admin must verify the printed clearance form and verification ID, student identity, original student ID **or** accepted lost-ID payment receipt, original certificate deposit receipt **or** affidavit, photocopies of all semester mark sheets, and physical photograph requirement. Track accepted alternatives and reasons. Block handover until required checks pass; store admin identity and handover time; prevent double handover.

## 11. One-day milestone schedule

| Hours | Deliverable and exit gate |
|---|---|
| 0–1.5 | Repository, backend/frontend skeleton, MySQL/Flyway; both builds start |
| 1.5–3.5 | Employee auth, roles, secure student OTP; auth tests pass |
| 3.5–5.5 | Alumni and No Dues forms; 17 approvals created transactionally |
| 5.5–8.5 | Staff queue, approve/reject, reopening and audit; permission tests pass |
| 8.5–10 | Principal final review and state machine; premature approval blocked |
| 10–12 | Official-format PDF, QR verification, SMTP delivery and retry |
| 12–15 | React student and staff pages integrated with backend |
| 15–17 | Principal and Admin pages, physical checklist and handover |
| 17–19 | Reports, status handling, error states, integration fixes |
| 19–21 | Automated and manual end-to-end tests |
| 21–24 | Bug fixes, README, architecture, demo instructions and actual results |

Prioritize the end-to-end path; do not mark features complete until built and verified. If time is insufficient, document gaps rather than silently bypassing authorization, email, or approval gates.

## 12. Integration acceptance tests

1. Regular and lateral roll formats accepted; invalid/mismatched email rejected.
2. OTP delivered, expires, is attempt-limited and cannot be reused.
3. Alumni form stores all official fields and five friends.
4. No Dues submission before Alumni Registration is rejected.
5. Submitting No Dues creates exactly 17 pending approvals.
6. Employee login works; staff cannot act on other departments.
7. Rejection requires remarks and blocks Principal stage.
8. Admin-authorized reopening preserves prior approvals and audit events.
9. Principal queue appears only when all 17 approve.
10. Admin cannot impersonate Staff or Principal approval.
11. Principal rejection blocks PDF issuance.
12. Principal approval generates correct PDF with 17 approvals, timestamp labels and unique QR/verification ID.
13. PDF email is delivered with real SMTP; failure/retry does not reverse approval or duplicate issuance.
14. Student cannot read another student's PDF or application.
15. Public QR verification exposes minimal non-sensitive information.
16. Admin checklist enforces required physical documents and accepted alternatives.
17. Duplicate certificate handover is blocked.
18. Backend test suite, frontend production build, migrations and complete UI walkthrough pass.

## 13. Delivery artifacts

- Complete `backend/` and `frontend/` sources; database migrations.
- `reference-forms/` with both official PDFs supplied by the project owner.
- `.env.example`, safe admin bootstrap instructions, `README.md` and local run instructions for Windows/IntelliJ.
- OpenAPI documentation, ER diagram, state diagram, screenshots/demo steps.
- Automated tests and `PROGRESS.md` recording **actual** pass/fail results and known limitations.

## 14. Environment prerequisites

Java 21, Maven (or wrapper), Node/npm, MySQL 8, a configured database user, and authorized SMTP credentials are required for real email testing. Configure `APP_BASE_URL`, DB connection and mail credentials through environment variables. Do not commit `.env` secrets. Codex needs the two reference PDFs inside its repository; ChatGPT attachments are not automatically visible in an external Codex workspace.
