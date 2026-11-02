package com.dccmp.backend.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "original_handover_visits")
public class OriginalVisit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    public Long applicationId;
    public Long adminId;
    public Boolean identityVerified;
    @Column(columnDefinition = "TEXT")
    public String remarks;
    public LocalDateTime handedOverAt;
}
