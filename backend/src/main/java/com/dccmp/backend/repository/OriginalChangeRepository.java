package com.dccmp.backend.repository;

import com.dccmp.backend.entity.OriginalChange;

import java.util.*;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface OriginalChangeRepository extends JpaRepository<OriginalChange, Long> {
    List<OriginalChange> findByApplicationIdOrderByOccurredAtAsc(Long applicationId);
}
