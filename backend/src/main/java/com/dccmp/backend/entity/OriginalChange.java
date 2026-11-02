package com.dccmp.backend.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "original_document_changes")
public class OriginalChange {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    public Long applicationId;
    public Long documentId;
    public Long adminId;
    public String action;
    @Column(columnDefinition = "TEXT")
    public String details;
    public LocalDateTime occurredAt;
}
