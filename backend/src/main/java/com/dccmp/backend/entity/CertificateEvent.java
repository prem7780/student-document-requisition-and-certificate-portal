package com.dccmp.backend.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "certificate_request_events")
public class CertificateEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    public Long requestId;
    public String action;
    public String actorRole;
    public Long actorId;
    @Column(columnDefinition = "TEXT")
    public String remarks;
    public LocalDateTime occurredAt;
}
