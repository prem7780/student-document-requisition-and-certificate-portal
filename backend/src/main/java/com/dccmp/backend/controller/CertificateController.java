package com.dccmp.backend.controller;

import com.dccmp.backend.service.*;
import com.dccmp.backend.security.Access;
import com.dccmp.backend.repository.StaffRepository;
import com.dccmp.backend.entity.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api")
public class CertificateController {
    private final Access access;
    private final StaffRepository staff;
    private final CertificateService cert;

    public CertificateController(Access a, StaffRepository s, CertificateService c) {
        access = a;
        staff = s;
        cert = c;
    }

    @GetMapping("/certificates/types")
    public List<String> types(HttpServletRequest r) {
        access.student(r);
        return cert.availableTypes();
    }

    @GetMapping("/certificates/branches")
    public List<Map<String, Object>> branches(HttpServletRequest r) {
        access.student(r);
        return cert.branches();
    }

    @PostMapping("/student/certificates")
    public Map<String, Object> submit(HttpServletRequest r, @RequestBody CertificateService.Input input) {
        return cert.view(cert.submit(access.student(r), input));
    }

    @GetMapping("/student/certificates")
    public List<Map<String, Object>> own(HttpServletRequest r) {
        return cert.own(access.student(r));
    }

    @GetMapping("/student/certificates/{id}")
    public Map<String, Object> ownDetails(HttpServletRequest r, @PathVariable long id) {
        return cert.ownDetail(access.student(r), id);
    }

    @GetMapping("/staff/certificates")
    public List<Map<String, Object>> hod(HttpServletRequest r) {
        return cert.hodRequests(access.currentStaff(r, staff, "STAFF"));
    }

    @GetMapping("/staff/certificates/{id}")
    public Map<String, Object> hodDetail(HttpServletRequest r, @PathVariable long id) {
        return cert.hodDetail(access.currentStaff(r, staff, "STAFF"), id);
    }

    @GetMapping("/staff/certificates/analytics")
    public Map<String, Object> hodAnalytics(HttpServletRequest r) {
        return cert.analytics("HOD", access.currentStaff(r, staff, "STAFF").departmentId);
    }

    @PatchMapping("/staff/certificates/{id}/approve")
    public Map<String, Object> hodApprove(HttpServletRequest r, @PathVariable long id, @RequestBody(required = false) CertificateService.Decision d) {
        return cert.view(cert.decideHod(access.currentStaff(r, staff, "STAFF"), id, true, d == null ? "" : d.remarks()));
    }

    @PatchMapping("/staff/certificates/{id}/reject")
    public Map<String, Object> hodReject(HttpServletRequest r, @PathVariable long id, @RequestBody CertificateService.Decision d) {
        CertificateRequest c = cert.decideHod(access.currentStaff(r, staff, "STAFF"), id, false, d.remarks());
        cert.notifyEvent(c.id, "REJECTED");
        return cert.view(c);
    }

    @GetMapping("/principal/certificates")
    public List<Map<String, Object>> principal(HttpServletRequest r) {
        access.currentStaff(r, staff, "PRINCIPAL");
        return cert.principalRequests();
    }

    @GetMapping("/principal/certificates/{id}")
    public Map<String, Object> principalDetail(HttpServletRequest r, @PathVariable long id) {
        access.currentStaff(r, staff, "PRINCIPAL");
        return cert.anyDetail(id);
    }

    @GetMapping("/principal/certificates/analytics")
    public Map<String, Object> principalAnalytics(HttpServletRequest r) {
        access.currentStaff(r, staff, "PRINCIPAL");
        return cert.analytics("ALL", null);
    }

    @PatchMapping("/principal/certificates/{id}/approve")
    public Map<String, Object> principalApprove(HttpServletRequest r, @PathVariable long id, @RequestBody(required = false) CertificateService.Decision d) {
        CertificateRequest c = cert.decidePrincipal(access.currentStaff(r, staff, "PRINCIPAL"), id, true, d == null ? "" : d.remarks());
        cert.notifyEvent(c.id, "APPROVED");
        return cert.view(c);
    }

    @PatchMapping("/principal/certificates/{id}/reject")
    public Map<String, Object> principalReject(HttpServletRequest r, @PathVariable long id, @RequestBody CertificateService.Decision d) {
        CertificateRequest c = cert.decidePrincipal(access.currentStaff(r, staff, "PRINCIPAL"), id, false, d.remarks());
        cert.notifyEvent(c.id, "REJECTED");
        return cert.view(c);
    }

    @GetMapping("/admin/certificates")
    public List<Map<String, Object>> admin(HttpServletRequest r) {
        access.currentStaff(r, staff, "ADMIN");
        return cert.adminRequests();
    }

    @GetMapping("/admin/certificates/{id}")
    public Map<String, Object> adminDetail(HttpServletRequest r, @PathVariable long id) {
        access.currentStaff(r, staff, "ADMIN");
        return cert.anyDetail(id);
    }

    @GetMapping("/admin/certificates/analytics")
    public Map<String, Object> adminAnalytics(HttpServletRequest r) {
        access.currentStaff(r, staff, "ADMIN");
        return cert.analytics("ADMIN", null);
    }

    @GetMapping("/admin/certificates/failed-notifications")
    public List<CertificateNotification> failed(HttpServletRequest r) {
        access.currentStaff(r, staff, "ADMIN");
        return cert.failures();
    }

    @PostMapping("/admin/certificates/{id}/resend")
    public Map<String, Object> resend(HttpServletRequest r, @PathVariable long id, @RequestParam String kind) {
        access.currentStaff(r, staff, "ADMIN");
        cert.notifyEvent(id, kind);
        return Map.of("message", "Notification attempt recorded");
    }

    @PatchMapping("/admin/certificates/{id}/cancel")
    public Map<String, Object> cancel(HttpServletRequest r, @PathVariable long id, @RequestBody CertificateService.Decision d) {
        return cert.view(cert.cancel(access.currentStaff(r, staff, "ADMIN"), id, d.remarks()));
    }

    @PostMapping("/admin/certificates/{id}/collect")
    public Map<String, Object> collect(HttpServletRequest r, @PathVariable long id, @RequestBody CertificateService.Handover body) {
        return cert.view(cert.collect(access.currentStaff(r, staff, "ADMIN"), id, body));
    }
}
