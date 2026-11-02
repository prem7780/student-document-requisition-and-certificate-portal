package com.dccmp.backend.service;

import com.dccmp.backend.entity.*;
import com.dccmp.backend.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.server.ResponseStatusException;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CertificateService {
    public static final List<String> TYPES = List.of("STUDY", "BONAFIDE", "LOR", "DUPLICATE_TC", "CONDUCT");
    public static final Set<String> TERMINAL = Set.of("COLLECTED", "REJECTED", "CANCELLED", "EXPIRED");
    private static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");
    private final CertificateTypeRepository types;
    private final CertificateRequestRepository requests;
    private final CertificateEventRepository events;
    private final CertificateNotificationRepository notifications;
    private final DepartmentRepository departments;
    private final StudentRepository students;
    private final StudentDirectoryService directory;
    private final JavaMailSender mail;
    @Value("${app.mail-from}")
    private String mailFrom;

    public CertificateService(CertificateRequestRepository r, CertificateEventRepository e, CertificateNotificationRepository n, DepartmentRepository d, StudentRepository s, StudentDirectoryService sd, JavaMailSender m, CertificateTypeRepository types) {
        this.types = types;
        requests = r;
        events = e;
        notifications = n;
        departments = d;
        students = s;
        directory = sd;
        mail = m;
    }

    private LocalDateTime now() {
        return LocalDateTime.now(ZONE);
    }

    private ResponseStatusException bad(String s) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, s);
    }

    private ResponseStatusException conflict(String s) {
        return new ResponseStatusException(HttpStatus.CONFLICT, s);
    }

    private void event(CertificateRequest r, String action, String role, Long actor, String remarks) {
        CertificateEvent e = new CertificateEvent();
        e.requestId = r.id;
        e.action = action;
        e.actorRole = role;
        e.actorId = actor;
        e.remarks = remarks;
        e.occurredAt = now();
        events.save(e);
    }

    private CertificateRequest lock(long id) {
        return requests.lockById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Certificate request not found"));
    }

    private CertificateRequest lookup(long id) {
        return requests.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Certificate request not found"));
    }

    private String departmentCode(String branch) {
        String x = Objects.toString(branch, "").trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
        return switch (x) {
            case "CSE" -> "hod_cse";
            case "ECE" -> "hod_ece";
            case "EEE" -> "hod_eee";
            case "CIVIL" -> "hod_civil";
            case "MECH", "MECHANICAL" -> "hod_mech";
            case "AI" -> "hod_ai";
            case "IT" -> "hod_it";
            default -> throw bad("Select a valid branch with an assigned HOD");
        };
    }

    public List<Map<String, Object>> branches() {
        return departments.findAll().stream().filter(d -> Boolean.TRUE.equals(d.active) && d.code != null && d.code.toLowerCase(Locale.ROOT).startsWith("hod_")).map(d -> Map.<String, Object>of("code", d.code.substring(4).toUpperCase(Locale.ROOT), "name", d.name, "departmentId", d.id)).toList();
    }

    public List<String> availableTypes() {
        return types.findByActiveTrueOrderById().stream().map(x -> x.code).toList();
    }

    public record Input(String certificateType, String branch, String purpose, String remarks, String studentName) {
    }

    public record Decision(String remarks) {
    }

    public record Handover(boolean identityVerified, String remarks) {
    }

    @Transactional
    public CertificateRequest submit(long studentId, Input input) {
        if (input == null) throw bad("Application form is required");
        String type = Objects.toString(input.certificateType(), "").trim().toUpperCase(Locale.ROOT);
        if (types.findByCode(type).filter(t -> Boolean.TRUE.equals(t.active)).isEmpty())
            throw bad("Unsupported certificate type");
        String code = departmentCode(input.branch());
        Department hod = departments.findByCode(code).filter(d -> Boolean.TRUE.equals(d.active)).orElseThrow(() -> bad("Branch HOD is unavailable"));
        String purpose = WorkflowService.mandatory(input.purpose(), "Purpose");
        if (purpose.length() > 2000) throw bad("Purpose is too long");
        // Lock the student row so two concurrent submissions for the same type cannot both succeed.
        Student student = students.lockById(studentId).orElseThrow(() -> bad("Verified student not found"));
        for (CertificateRequest prior : requests.findByStudentIdOrderBySubmittedAtDesc(studentId)) {
            if (prior.certificateType.equals(type) && !TERMINAL.contains(effectiveStatus(prior, now())))
                throw conflict("An active request for this certificate type already exists");
        }
        String name = directory.find(student.rollNumber).map(d -> d.studentName).orElseGet(() -> WorkflowService.mandatory(input.studentName(), "Student name"));
        CertificateRequest r = new CertificateRequest();
        r.studentId = studentId;
        r.rollNumber = student.rollNumber;
        r.studentName = name;
        r.certificateType = type;
        r.branch = code.substring(4).toUpperCase(Locale.ROOT);
        r.hodDepartmentId = hod.id;
        r.purpose = purpose;
        r.remarks = WorkflowService.optional(input.remarks());
        r.status = "PENDING_HOD";
        r.submittedAt = now();
        r = requests.save(r);
        event(r, "SUBMITTED", "STUDENT", studentId, "Certificate request submitted");
        return r;
    }

    public String effectiveStatus(CertificateRequest r, LocalDateTime at) {
        return "APPROVED".equals(r.status) && r.expiresAt != null && !r.expiresAt.isAfter(at) ? "EXPIRED" : r.status;
    }

    public Map<String, Object> view(CertificateRequest r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", r.id);
        m.put("rollNumber", r.rollNumber);
        m.put("studentName", r.studentName);
        m.put("certificateType", r.certificateType);
        m.put("branch", r.branch);
        m.put("purpose", r.purpose);
        m.put("remarks", Objects.toString(r.remarks, ""));
        m.put("status", effectiveStatus(r, now()));
        m.put("hodRemarks", Objects.toString(r.hodRemarks, ""));
        m.put("principalRemarks", Objects.toString(r.principalRemarks, ""));
        m.put("cancellationReason", Objects.toString(r.cancellationReason, ""));
        m.put("submittedAt", r.submittedAt);
        m.put("hodDecidedAt", r.hodDecidedAt);
        m.put("principalDecidedAt", r.principalDecidedAt);
        m.put("expiresAt", r.expiresAt);
        m.put("collectedAt", r.collectedAt);
        return m;
    }

    public List<Map<String, Object>> own(long studentId) {
        return requests.findByStudentIdOrderBySubmittedAtDesc(studentId).stream().map(this::view).toList();
    }

    public Map<String, Object> ownDetail(long studentId, long id) {
        CertificateRequest r = lookup(id);
        if (!r.studentId.equals(studentId)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your request");
        return detail(r);
    }

    public Map<String, Object> detail(CertificateRequest r) {
        Map<String, Object> v = new LinkedHashMap<>(view(r));
        v.put("history", events.findByRequestIdOrderByOccurredAtAsc(r.id));
        return v;
    }

    public Map<String, Object> hodDetail(Staff hod, long id) {
        requireHod(hod);
        CertificateRequest r = lookup(id);
        if (!hod.departmentId.equals(r.hodDepartmentId))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This request belongs to another HOD");
        return detail(r);
    }

    public Map<String, Object> anyDetail(long id) {
        return detail(lookup(id));
    }

    public List<Map<String, Object>> hodRequests(Staff hod) {
        requireHod(hod);
        return requests.findByHodDepartmentIdOrderBySubmittedAtDesc(hod.departmentId).stream().map(this::view).toList();
    }

    private void requireHod(Staff s) {
        if (!"STAFF".equals(s.role) || s.departmentId == null || departments.findById(s.departmentId).map(d -> d.code != null && d.code.startsWith("hod_")).orElse(false) == false)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the assigned branch HOD can decide certificates");
    }

    public List<Map<String, Object>> principalRequests() {
        return requests.findAllByOrderBySubmittedAtDesc().stream().map(this::view).toList();
    }

    public List<Map<String, Object>> adminRequests() {
        return principalRequests();
    }

    @Transactional
    public CertificateRequest decideHod(Staff hod, long id, boolean approved, String remarks) {
        requireHod(hod);
        CertificateRequest r = lock(id);
        if (!hod.departmentId.equals(r.hodDepartmentId))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This request belongs to another HOD");
        if (!"PENDING_HOD".equals(r.status)) throw conflict("Request is no longer waiting for HOD approval");
        if (!approved) remarks = WorkflowService.mandatory(remarks, "Rejection remarks");
        r.status = approved ? "PENDING_PRINCIPAL" : "REJECTED";
        r.hodStaffId = hod.id;
        r.hodDecidedAt = now();
        r.hodRemarks = WorkflowService.optional(remarks);
        requests.save(r);
        event(r, approved ? "HOD_APPROVED" : "HOD_REJECTED", "STAFF", hod.id, r.hodRemarks);
        return r;
    }

    @Transactional
    public CertificateRequest decidePrincipal(Staff principal, long id, boolean approved, String remarks) {
        if (!"PRINCIPAL".equals(principal.role)) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        CertificateRequest r = lock(id);
        if (!"PENDING_PRINCIPAL".equals(r.status)) throw conflict("HOD approval is required before Principal decision");
        if (!approved) remarks = WorkflowService.mandatory(remarks, "Rejection remarks");
        r.status = approved ? "APPROVED" : "REJECTED";
        r.principalStaffId = principal.id;
        r.principalDecidedAt = now();
        r.principalRemarks = WorkflowService.optional(remarks);
        if (approved) r.expiresAt = r.principalDecidedAt.plusDays(30);
        requests.save(r);
        event(r, approved ? "PRINCIPAL_APPROVED" : "PRINCIPAL_REJECTED", "PRINCIPAL", principal.id, r.principalRemarks);
        return r;
    }

    @Transactional
    public CertificateRequest cancel(Staff admin, long id, String reason) {
        if (!"ADMIN".equals(admin.role)) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        CertificateRequest r = lock(id);
        if (TERMINAL.contains(effectiveStatus(r, now())))
            throw conflict("Completed or closed requests cannot be cancelled");
        r.status = "CANCELLED";
        r.cancelledBy = admin.id;
        r.cancellationReason = WorkflowService.mandatory(reason, "Cancellation reason");
        r.cancelledAt = now();
        requests.save(r);
        event(r, "CANCELLED", "ADMIN", admin.id, r.cancellationReason);
        return r;
    }

    @Transactional
    public CertificateRequest collect(Staff admin, long id, Handover details) {
        if (!"ADMIN".equals(admin.role)) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        if (details == null || !details.identityVerified())
            throw bad("Admin must confirm student identity before handover");
        CertificateRequest r = lock(id);
        if (!"APPROVED".equals(effectiveStatus(r, now())))
            throw conflict("Only approved, unexpired and uncollected requests can be handed over");
        r.status = "COLLECTED";
        r.collectedBy = admin.id;
        r.collectedAt = now();
        requests.save(r);
        event(r, "COLLECTED", "ADMIN", admin.id, "One physical " + r.certificateType + " certificate handed to student; " + WorkflowService.optional(details.remarks()));
        return r;
    }

    @Transactional
    @Scheduled(fixedDelay = 300000, initialDelay = 15000)
    public void expireDue() {
        for (CertificateRequest r : requests.findByStatusAndExpiresAtBefore("APPROVED", now())) {
            // Use row lock and recheck. Expiry must not overwrite a concurrent collection or cancellation.
            CertificateRequest current = lock(r.id);
            if (!"APPROVED".equals(current.status) || current.expiresAt == null || current.expiresAt.isAfter(now()))
                continue;
            current.status = "EXPIRED";
            requests.save(current);
            event(current, "EXPIRED", "SYSTEM", null, "30-day collection window ended");
        }
    }

    @Scheduled(fixedDelay = 300000, initialDelay = 30000)
    public void maintenance() {
        for (CertificateRequest r : requests.findAll()) {
            if ("APPROVED".equals(r.status) && r.expiresAt != null && !r.expiresAt.isAfter(now().plusDays(5)) && r.expiresAt.isAfter(now()) && notifications.findByRequestIdAndKind(r.id, "REMINDER").isEmpty())
                try {
                    notifyEvent(r.id, "REMINDER");
                } catch (Exception ex) {
                    System.err.println("Certificate reminder failed for #" + r.id + ": " + ex.getMessage());
                }
            if ("EXPIRED".equals(r.status) && notifications.findByRequestIdAndKind(r.id, "EXPIRED").isEmpty()) try {
                notifyEvent(r.id, "EXPIRED");
            } catch (Exception ex) {
                System.err.println("Expiration notification failed for #" + r.id + ": " + ex.getMessage());
            }
        }
    }

    public void notifyEvent(long id, String kind) {
        CertificateRequest r = lookup(id);
        if (!Set.of("APPROVED", "REJECTED", "REMINDER", "EXPIRED").contains(kind))
            throw bad("Unknown notification kind");
        boolean valid = switch (kind) {
            case "APPROVED" -> "APPROVED".equals(effectiveStatus(r, now()));
            case "REJECTED" -> "REJECTED".equals(r.status);
            case "REMINDER" ->
                    "APPROVED".equals(effectiveStatus(r, now())) && r.expiresAt != null && !r.expiresAt.isAfter(now().plusDays(5));
            default -> "EXPIRED".equals(effectiveStatus(r, now()));
        };
        if (!valid) throw conflict("Notification not valid for current request status");
        CertificateNotification n = notifications.findByRequestIdAndKind(id, kind).orElseGet(() -> {
            CertificateNotification fresh = new CertificateNotification();
            fresh.requestId = id;
            fresh.kind = kind;
            fresh.attempts = 0;
            fresh.state = "PENDING";
            return notifications.save(fresh);
        });
        // Scheduled maintenance must not resend successfully submitted notifications.
        if ("SENT".equals(n.state)) return;
        Student s = students.findById(r.studentId).orElseThrow();
        String recipient = directory.emailFor(s.rollNumber);
        n.recipient = recipient;
        n.attempts++;
        n.attemptedAt = now();
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailFrom);
        message.setTo(recipient);
        message.setSubject("DCCMP " + r.certificateType.replace('_', ' ') + " request #" + r.id + " — " + kind);
        String text = switch (kind) {
            case "APPROVED" ->
                    "Your certificate request is approved. Collect one physical certificate from the college Admin office by " + r.expiresAt + " (IST).";
            case "REJECTED" ->
                    "Your certificate request was rejected. HOD remarks: " + Objects.toString(r.hodRemarks, "") + " Principal remarks: " + Objects.toString(r.principalRemarks, "") + ". Submit a new request if needed.";
            case "REMINDER" ->
                    "Reminder: five days or fewer remain to collect your approved certificate. Deadline: " + r.expiresAt + " (IST).";
            default -> "Your certificate request expired without collection. You may submit a new application.";
        };
        message.setText("Request #" + r.id + "\n" + text + "\nCheck the Student Portal for current status.");
        try {
            mail.send(message);
            n.state = "SENT";
            n.sentAt = now();
            n.lastError = null;
        } catch (Exception ex) {
            n.state = "FAILED";
            n.lastError = Objects.toString(ex.getMessage(), "SMTP delivery failed").substring(0, Math.min(250, Objects.toString(ex.getMessage(), "SMTP delivery failed").length()));
        }
        notifications.save(n);
    }

    public List<CertificateNotification> failures() {
        return notifications.findTop100ByStateOrderByAttemptedAtDesc("FAILED");
    }

    public Map<String, Object> analytics(String scope, Long department) {
        List<CertificateRequest> all = "HOD".equals(scope) ? requests.findByHodDepartmentIdOrderBySubmittedAtDesc(department) : requests.findAll();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("total", all.size());
        for (String status : List.of("PENDING_HOD", "PENDING_PRINCIPAL", "APPROVED", "REJECTED", "CANCELLED", "COLLECTED", "EXPIRED"))
            m.put(status, all.stream().filter(r -> status.equals(effectiveStatus(r, now()))).count());
        Map<String, Long> byType = all.stream().collect(Collectors.groupingBy(r -> r.certificateType, TreeMap::new, Collectors.counting()));
        Map<String, Long> byBranch = all.stream().collect(Collectors.groupingBy(r -> r.branch, TreeMap::new, Collectors.counting()));
        m.put("byType", byType);
        m.put("byBranch", byBranch);
        var decided = all.stream().filter(r -> r.hodDecidedAt != null && r.submittedAt != null).toList();
        m.put("avgHodMinutes", decided.isEmpty() ? 0 : decided.stream().mapToLong(r -> Math.max(0, Duration.between(r.submittedAt, r.hodDecidedAt).toMinutes())).average().orElse(0));
        var principalDecided = all.stream().filter(r -> r.principalDecidedAt != null && r.hodDecidedAt != null).toList();
        m.put("avgPrincipalMinutes", principalDecided.isEmpty() ? 0 : principalDecided.stream().mapToLong(r -> Math.max(0, Duration.between(r.hodDecidedAt, r.principalDecidedAt).toMinutes())).average().orElse(0));
        if ("ADMIN".equals(scope))
            m.put("failedNotifications", notifications.findTop100ByStateOrderByAttemptedAtDesc("FAILED").size());
        return m;
    }
}
