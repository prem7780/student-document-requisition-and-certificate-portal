package com.dccmp.backend.repository;

import com.dccmp.backend.entity.CertificateNotification;

import java.util.*;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface CertificateNotificationRepository extends JpaRepository<CertificateNotification, Long> {
    Optional<CertificateNotification> findByRequestIdAndKind(Long id, String kind);

    List<CertificateNotification> findTop100ByStateOrderByAttemptedAtDesc(String state);
}
