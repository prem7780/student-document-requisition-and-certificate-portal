package com.dccmp.backend.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "certificate_requests")
public class CertificateRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @Column(nullable = false)
    public Long studentId;
    @Column(nullable = false)
    public String rollNumber;
    @Column(nullable = false)
    public String studentName;
    @Column(nullable = false)
    public String certificateType;
    @Column(nullable = false)
    public String branch;
    @Column(nullable = false)
    public Long hodDepartmentId;
    @Column(columnDefinition = "TEXT")
    public String purpose;
    @Column(columnDefinition = "TEXT")
    public String remarks;
    @Column(nullable = false)
    public String status;
    public Long hodStaffId;
    public String hodRemarks;
    public LocalDateTime hodDecidedAt;
    public Long principalStaffId;
    public String principalRemarks;
    public LocalDateTime principalDecidedAt;
    public LocalDateTime expiresAt;
    public Long cancelledBy;
    public String cancellationReason;
    public LocalDateTime cancelledAt;
    public Long collectedBy;
    public LocalDateTime collectedAt;
    public LocalDateTime submittedAt;
    @Version
    public Long version;
}
