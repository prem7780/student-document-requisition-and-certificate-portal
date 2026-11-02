package com.dccmp.backend.repository;

import com.dccmp.backend.entity.CertificateType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.*;

public interface CertificateTypeRepository extends JpaRepository<CertificateType, Long> {
    Optional<CertificateType> findByCode(String code);

    List<CertificateType> findByActiveTrueOrderById();
}
