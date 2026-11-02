# DCCMP — CODEX MASTER IMPLEMENTATION PROMPT

You are the lead architect, senior Java/Spring engineer, React engineer, database designer, security reviewer and integration-test engineer. **Implement**, build and test a fresh full-stack project named **Digital Campus Clearance and Certificate Management Platform (DCCMP)** for **Usharama College of Engineering and Technology**.

## Operating rules

1. Work in the current workspace; create the actual files, not merely a plan. Start from scratch. Do not assume any previous implementation exists.
2. Read `DCCMP_Project_Plan.md` **in full** before coding; it is the definitive detailed specification and acceptance checklist. Read both official PDFs from `reference-forms/` as the authoritative forms. If the PDFs are absent, ask for them and continue on independent modules without pretending to have inspected them.
3. Target a complete, demonstration-ready implementation in a **24-hour sprint**. Prioritize the end-to-end flow. Time is a target, not permission to skip security or invent test results.
4. Execute work in milestones. After each milestone, run the relevant build/tests and update `PROGRESS.md` with what actually passed, failed or is blocked. Continue autonomously on routine tasks. Ask only for missing credentials/assets or consequential decisions.
5. Do not use mock data, simulated OTP delivery, fake signatures, hardcoded credentials, bypassed approvals or placeholder APIs in the final product. If real SMTP credentials are unavailable, implement the SMTP integration and a documented local test mode, and explicitly flag real delivery as unverified.
6. Keep source readable, modular and secure. Never commit passwords, tokens, student personal data or `.env` secrets.

## Stack — mandatory

- Java 21, compatible Spring Boot 4.x, Maven.
- Spring Security, BCrypt, Spring Data JPA, Hibernate, Jakarta Validation, Spring Boot Mail.
- MySQL 8, Flyway migrations.
- React, Vite, Tailwind CSS, React Router.
- Apache PDFBox for PDF generation/overlay; ZXing for QR codes.
- JUnit/Mockito and real integration testing, OpenAPI.
- Windows/IntelliJ-friendly setup instructions.

Create `backend/`, `frontend/`, `database/`, `reference-forms/`, `docs/`, `README.md`, `.env.example`, `.gitignore`, and `PROGRESS.md`. Use the database `dccmp_db`. Verify dependency compatibility with Spring Boot 4.x rather than copying outdated configuration.

## Official forms — mandatory

1. **Alumni Registration**, document code `URCET/7.22/AC 08`, from `reference-forms/Alumni_Registration(1).pdf`. Fields: name, father name, roll/hall ticket number, year of study, course and branch, permanent address, mobile, email, hobbies, GRE/TOEFL/GATE/IES academic achievements, awards, placement details, five best friends' names and phone numbers, job details with address, future plans. Photograph is provided physically, **not uploaded**.
2. **No Dues**, document code `URCET/5.4/FT 01`, from `reference-forms/No_Dues(2).pdf`. Fields: department/branch, student name, roll, address, mobile, email, student declaration; HOD and departmental Due/No Due areas, AO/date and Principal/date. Preserve the official appearance while adapting it to all **17** required departments. Do not forge signatures or seals.

Students do **not** upload supporting documents. Original student ID or allowed lost-ID payment receipt, certificate deposit receipt or affidavit, semester mark-sheet photocopies, photograph and printed clearance form are physically checked by Admin.

## Exactly 17 departments — all mandatory for every student, in parallel

`hod_eee`, `hod_civil`, `hod_ece`, `hod_cse`, `hod_mech`, `hod_ai`, `hod_it`, `hostel`, `canteen`, `accounts`, `scholarship`, `training_placements`, `examinations`, `sports`, `alumni`, `library`, `special_team`.

Display names respectively: EEE HOD, Civil HOD, ECE HOD, CSE HOD, Mechanical HOD, AI HOD, IT HOD, Hostel, Canteen, Accounts, Scholarship, Training & Placements, Examination Section, Games & Sports, Alumni, Library, Special Team.

Seed them with Flyway. Upon valid No Dues submission create **exactly 17** `PENDING` approvals transactionally, with a unique `(application_id, department_id)` constraint. No branch-specific exemptions.

## Student identity and submission

- No pre-created student passwords/accounts. Verify official college email with a real OTP.
- Regular roll `23NG1A0501` (`^[0-9]{2}NG1A[0-9]{4}$`), lateral `24NG5A0501` (`^[0-9]{2}NG5A[0-9]{4}$`); normalize uppercase; allow regex configuration for institutional changes.
- Email must equal `lowercase(roll) + '@usharama.in'`, ignoring case.
- OTP must be securely generated, stored hashed, expire, be single-use, limit attempts, enforce resend cooldown and rate limits. Student receives a secure session after verification. Roll number alone is never authentication.
- Two separate forms, **Alumni Registration first**, then **No Dues**. Link both to the same verified student identity. No departmental workflow before both forms are submitted.
- Student can track own application and download only own approved PDF.

## Roles and authorization

**STAFF:** staff-code/password login, BCrypt, assigned to one department; can only see and approve/reject that department's requests. Rejection requires remarks. Alumni department staff can see all alumni form fields including five friends; other staff receive only necessary No Dues details.

**PRINCIPAL:** separate role/login; may approve/reject only after **all 17** department decisions are `APPROVED`. Rejection requires remarks.

**ADMIN:** employee account management, department assignment, monitoring, reports, audit logs, authorize reopening, physical AO verification and certificate handover. **Cannot approve on behalf of staff or Principal.** No separate AO digital login/approval stage.

Use server-side role, department and record-level authorization, secure HttpOnly session cookies, appropriate CSRF protection, production HTTPS configuration, safe CORS/proxy, secure admin bootstrap through environment variables.

## Rejection, reopening and audit

Departments and Principal can reject with mandatory remarks. Rejection blocks advancement. An Admin can authorize reopening after correction; only the corresponding Staff/Principal makes the replacement decision. **Never reset already approved departments** merely because one rejects. Keep append-only audit events for decisions, rejection remarks, reopening authorization, actor and timestamp. Use transactions and optimistic locking to avoid race conditions.

## Principal, PDF, QR and email

After all 17 departments approve, set `PENDING_PRINCIPAL`. Only Principal can finalize. After Principal approval:

1. Generate an official-format No Dues clearance/collection PDF with student details and **all 17 digital approval labels**, approver names and timestamps, and Principal label/date.
2. Do **not** fabricate handwritten signatures or stamps. Leave AO physical verification as a later collection step.
3. Add a cryptographically unpredictable verification ID and ZXing QR code pointing to a minimal public authenticity page. Do not reveal sensitive student details on that page.
4. Store PDF securely, generate idempotently and prevent issuance before Principal approval.
5. Email the approved PDF to the verified student via **Spring Boot Mail with configurable real SMTP credentials**. Store delivery status and retry after failure without reversing approval or duplicating forms.

The PDF is **not** an original academic certificate. Admin physically hands over originals only after document verification.

## Physical collection and Admin verification

Student prints the approved PDF and brings the photograph, original ID or approved lost-ID payment receipt, original certificate deposit receipt or affidavit, photocopies of all semester mark sheets, and any required physical identification. Admin scans/checks QR or verification ID, checks documents using a persisted checklist, records accepted alternatives and remarks, and records final handover with timestamp and Admin identity. Block handover if checks are incomplete or if handover was already recorded. AO responsibility is handled by Admin at this stage.

## Database entities and states

Use Flyway and constraints for: `student_identities`, `email_otps`, `alumni_registrations`, `alumni_friends`, `no_dues_applications`, `departments`, `staff`, `department_approvals`, `principal_approvals`, `approval_reopenings`, `audit_events`, `generated_forms`, `email_deliveries`, `certificate_handovers`, `handover_checklist_items`.

Keep approval, document/email and handover states **separate**:

- Application: `PENDING_DEPARTMENTS`, `ACTION_REQUIRED`, `PENDING_PRINCIPAL`, `PRINCIPAL_ACTION_REQUIRED`, `APPROVED`.
- Department: `PENDING`, `APPROVED`, `REJECTED`.
- Principal: `PENDING`, `APPROVED`, `REJECTED`.
- Delivery: `NOT_GENERATED`, `GENERATED`, `EMAIL_PENDING`, `EMAIL_SENT`, `EMAIL_FAILED`.
- Handover: `NOT_COLLECTED`, `VERIFICATION_PENDING`, `HANDED_OVER`.

## Backend and frontend implementation

Backend packages under `com.dccmp.backend`: config, security, entity, repository, dto, service, controller, exception, mapper, util. Thin controllers, DTOs, service-layer transactions, global error handling, pagination, OpenAPI. Implement student OTP, both forms, tracking, staff approvals, alumni staff views, Principal queue and decisions, Admin account management/reopenings/reports/handover, and public minimal QR verification.

Frontend: responsive, accessible React/Tailwind portal with landing page; student email/OTP, Alumni Registration, No Dues, progress and approved PDF; employee login; department Staff dashboard; Alumni records viewer; Principal dashboard; Admin management/monitoring/audit/checklist/handover; public verification page. Integrate with real APIs. Include loading/error/empty states, validation, navigation, filters and confirmation dialogs.

## Implementation milestones and mandatory gates

1. Scaffold project, DB/Flyway and 17 department seeds; backend/frontend build.
2. Employee authentication, roles and student OTP; verify security tests.
3. Alumni Registration and No Dues sequence; verify 17 approvals created exactly once.
4. Staff parallel decisions, rejection/reopening and audit; verify department isolation.
5. Principal decision gate; verify no premature approval.
6. PDFBox official-form output, digital approval labels, QR authenticity, SMTP attachment and retries.
7. React student and Staff UI fully connected.
8. Principal and Admin UI, physical checklist and handover.
9. Reports, UX/error handling, end-to-end integration tests, documentation.

After each milestone run tests/builds and update `PROGRESS.md`. Do not stop at a planning response. Continue writing and running code.

## Acceptance tests — actually run and report

- Regular/lateral roll validation and email mismatch rejection.
- OTP delivery, expiry, one-time use, attempt limits.
- Alumni must precede No Dues; all official fields and five friends persisted.
- Exactly 17 department approvals per application.
- Staff cannot read/decide another department's approval.
- Mandatory rejection remarks, preserved approvals, Admin-authorized reopening and full audit.
- Principal queue blocked until 17/17 approved; Admin cannot override approvals.
- PDF blocked until Principal approval; PDF includes correct details, 17 labels and QR.
- SMTP sends approved PDF attachment; failure/retry preserves approval and idempotency.
- Student data isolation and minimal public verification disclosure.
- Physical document checklist blocks invalid handover; duplicate handover prevented.
- Backend tests, MySQL migrations, frontend production build, and UI end-to-end demo all pass.

## Deliverables and finish criteria

Produce the complete source, migrations, `.env.example`, secure bootstrap setup, README with Windows/IntelliJ instructions, OpenAPI, ER and workflow diagrams, tests, demo script and `PROGRESS.md` with **real command output summaries**. Document any missing credentials or unverified integration honestly. Never claim success without execution.

**Begin now:** inspect workspace and `DCCMP_Project_Plan.md`, inspect both official PDFs, scaffold project, implement Flyway migrations and auth, then continue milestone by milestone to a demonstrable integrated DCCMP.
