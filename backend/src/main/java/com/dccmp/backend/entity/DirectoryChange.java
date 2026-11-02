package com.dccmp.backend.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "directory_changes")
public class DirectoryChange {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    public Long directoryId;
    public String rollNumber;
    public String fieldName;
    public String oldValue;
    public String newValue;
    public Long adminId;
    public Long importId;
    public LocalDateTime changedAt;
}
