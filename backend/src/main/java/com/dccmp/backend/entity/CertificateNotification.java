package com.dccmp.backend.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "certificate_notifications")
public class CertificateNotification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    public Long requestId;
    public String kind;
    public String recipient;
    public String state;
    public Integer attempts;
    public String lastError;
    public LocalDateTime attemptedAt;
    public LocalDateTime sentAt;
}
