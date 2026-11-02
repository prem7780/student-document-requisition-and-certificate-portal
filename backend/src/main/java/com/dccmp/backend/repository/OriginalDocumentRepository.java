package com.dccmp.backend.repository;

import com.dccmp.backend.entity.OriginalDocument;

import java.util.*;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface OriginalDocumentRepository extends JpaRepository<OriginalDocument, Long> {
    List<OriginalDocument> findByApplicationIdOrderById(Long applicationId);
}
