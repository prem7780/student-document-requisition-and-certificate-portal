package com.dccmp.backend.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "student_directory", uniqueConstraints = @UniqueConstraint(columnNames = "roll_number"))
public class StudentDirectory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @Column(nullable = false, unique = true)
    public String rollNumber;
    @Column(nullable = false)
    public String studentName;
    @Column(nullable = false)
    public String branch;
    @Column(nullable = false, unique = true)
    public String email;
    public String batchYear;
    public LocalDateTime updatedAt;
    public Long updatedBy;
}
