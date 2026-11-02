package com.dccmp.backend.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "directory_imports")
public class DirectoryImport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    public Long adminId;
    public String fileName;
    public Integer totalRows;
    public Integer createdRows;
    public Integer updatedRows;
    public Integer unchangedRows;
    public Integer invalidRows;
    public LocalDateTime createdAt;
    @Column(columnDefinition = "LONGTEXT")
    public String errorCsv;
}
