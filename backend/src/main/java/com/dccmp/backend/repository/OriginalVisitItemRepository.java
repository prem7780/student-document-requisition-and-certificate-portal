package com.dccmp.backend.repository;

import com.dccmp.backend.entity.OriginalVisitItem;

import java.util.*;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface OriginalVisitItemRepository extends JpaRepository<OriginalVisitItem, Long> {
    List<OriginalVisitItem> findByVisitId(Long visitId);
}
