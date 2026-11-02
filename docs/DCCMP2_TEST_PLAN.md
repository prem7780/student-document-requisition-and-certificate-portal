# DCCMP 2.0 — Test Plan and Error Evidence

**Status:** Cases defined; execute in a properly configured local/test deployment. This file is not a claim that the tests passed.

For each case capture: Case ID, preconditions, steps, input, expected HTTP/status, actual result, pass/fail, console/server log reference and screenshot(s).

| ID | Scenario | Expected behavior | Evidence |
|---|---|---|---|
| TC-EMAIL-001 | Roll number + mismatched email | HTTP 400; user sees mismatch; no OTP sent | Student login error screenshot |
| TC-EMAIL-002 | Imported name-based email | OTP sent only to mapped official email | Masked mailbox screenshot and API response |
| TC-EMAIL-003 | No directory mapping | Roll-based college email accepted | OTP request/response and test mailbox |
| TC-EMAIL-004 | Expired / incorrect OTP | Reject, enforce maximum attempts | Browser error, sanitized log |
| TC-IMPORT-001 | Valid CSV upload | New directory records saved | Admin import summary |
| TC-IMPORT-002 | Valid XLSX upload | Excel parsed and saved | Admin import summary |
| TC-IMPORT-003 | Invalid / duplicate rows | Valid rows persist; invalid skipped with error CSV | Summary + downloaded error CSV |
| TC-IMPORT-004 | Existing roll / modified email | Existing mapping replaced; old/new audit record retained | Directory and audit screenshot |
| TC-IMPORT-005 | Non-admin attempts upload | HTTP 403 | API error screenshot |
| TC-NODUES-001 | Submit without Alumni Registration | Rejected | Student portal error |
| TC-NODUES-002 | Submit second No Dues after approval | HTTP 409; only first request retained | Response and database query |
| TC-CERT-001 | Study certificate form | Routes to selected HOD | Student form + HOD queue |
| TC-CERT-002 | Non-HOD department tries decision | HTTP 403 | API response |
| TC-CERT-003 | Wrong-branch HOD tries decision | HTTP 403 | API response |
| TC-CERT-004 | Principal approves before HOD | HTTP 409 | Principal error |
| TC-CERT-005 | HOD rejection without remarks | HTTP 400 | HOD error |
| TC-CERT-006 | Principal approval triggers email | Request remains approved; notify logged | Approved status + email log |
| TC-CERT-007 | SMTP unavailable on approval | Approval remains approved; notification FAILED | Server log + Admin failed notifications |
| TC-CERT-008 | Admin retries failed notification | New attempt recorded, decision unchanged | Admin retry result |
| TC-CERT-009 | Duplicate active same type | HTTP 409 | Student error |
| TC-CERT-010 | Parallel requests of different types | Both accepted | Student request history |
| TC-CERT-011 | Rejected request → fresh application | New Request ID, old history preserved | History |
| TC-CERT-012 | Admin cancels uncollected request | Required reason; status CANCELLED | Admin cancellation + audit |
| TC-CERT-013 | Student tries to cancel | No student cancellation endpoint; denied | API response |
| TC-CERT-014 | Admin collects without identity check | HTTP 400 | Admin UI validation |
| TC-CERT-015 | Admin collects approved certificate | Exactly one physical certificate; COLLECTED | Admin history |
| TC-CERT-016 | Attempt second collection | HTTP 409 | API response |
| TC-CERT-017 | After 25 days (controlled test clock) | One reminder logged | Notification log |
| TC-CERT-018 | After 30 days (controlled test clock) | Status EXPIRED, handover rejected | Admin portal & test logs |
| TC-CERT-019 | Request after collection / expiry | New request accepted | Student history |
| TC-ORIGINAL-001 | Initialize expected original checklist | One student-specific checklist | Admin screen |
| TC-ORIGINAL-002 | First visit partial handover | Exact items, quantities, visit recorded | Visit history |
| TC-ORIGINAL-003 | Second visit | Remaining documents updated | Visit history |
| TC-ORIGINAL-004 | Duplicate handover of collected item | HTTP 409 | API response |
| TC-ORIGINAL-005 | Remove already collected document | HTTP 409; audit retained | Admin error |
| TC-ORIGINAL-006 | Complete all original handovers | No Dues collection status HANDED_OVER | Student status |

## Selenium screenshot test

From `testing/selenium`, with Chrome, Java 21, Maven, MySQL, backend and frontend running:

```bash
mvn test -Dtest=StudentEmailValidationTest -Ddccmp.url=http://localhost:5173
```

If the deployed authentication response differs from the expected text, update the assertion to match the implemented user-facing error before using the screenshot as evidence.

## Backend unit tests

From `backend` run:

```bash
mvn test
```

Review `WorkflowServiceTest`, `WorkflowRulesTest`, and `CertificateRulesTest` before recording passed results. For 30-day expiration, use an isolated test database and controlled clock or seeded timestamps; never manipulate production records just to produce screenshots.

## Error handling format for project report

Record **Input → Validation → Error response → User feedback → Corrected input → Retest outcome**. For backend failures, include a sanitized log excerpt and relevant fix or root cause. Do not paste raw SMTP credentials, one-time codes or session tokens.
