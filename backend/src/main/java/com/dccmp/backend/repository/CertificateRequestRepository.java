package com.dccmp.backend.repository;

import com.dccmp.backend.entity.CertificateRequest;

import java.util.*;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface CertificateRequestRepository extends JpaRepository<CertificateRequest, Long> {
    List<CertificateRequest> findByStudentIdOrderBySubmittedAtDesc(Long studentId);

    List<CertificateRequest> findByHodDepartmentIdOrderBySubmittedAtDesc(Long departmentId);

    List<CertificateRequest> findAllByOrderBySubmittedAtDesc();

    List<CertificateRequest> findByStatusOrderBySubmittedAtDesc(String status);

    List<CertificateRequest> findByStatusAndExpiresAtBefore(String status, java.time.LocalDateTime expiry);

    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from CertificateRequest r where r.id = :id")
    Optional<CertificateRequest> lockById(@Param("id") Long id);
}
