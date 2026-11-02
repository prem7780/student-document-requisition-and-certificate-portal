package com.dccmp.backend.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "original_documents")
public class OriginalDocument {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    public Long applicationId;
    public String documentName;
    public Integer expectedQuantity;
    public Integer collectedQuantity;
    public Boolean active;
    public LocalDateTime createdAt;
}
