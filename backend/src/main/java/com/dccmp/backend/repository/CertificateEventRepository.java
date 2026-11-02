package com.dccmp.backend.repository;

import com.dccmp.backend.entity.CertificateEvent;

import java.util.*;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface CertificateEventRepository extends JpaRepository<CertificateEvent, Long> {
    List<CertificateEvent> findByRequestIdOrderByOccurredAtAsc(Long requestId);
}
