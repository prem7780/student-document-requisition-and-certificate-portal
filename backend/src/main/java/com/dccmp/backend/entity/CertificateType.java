package com.dccmp.backend.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "certificate_types")
public class CertificateType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @Column(nullable = false, unique = true)
    public String code;
    @Column(nullable = false)
    public String name;
    @Column(nullable = false)
    public Boolean active;
}
