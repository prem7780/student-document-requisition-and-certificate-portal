package com.dccmp.backend.repository;

import com.dccmp.backend.entity.OriginalVisit;

import java.util.*;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface OriginalVisitRepository extends JpaRepository<OriginalVisit, Long> {
    List<OriginalVisit> findByApplicationIdOrderByHandedOverAtAsc(Long applicationId);
}
