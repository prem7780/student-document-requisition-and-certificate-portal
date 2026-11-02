package com.dccmp.backend.repository;

import com.dccmp.backend.entity.DirectoryChange;

import java.util.*;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface DirectoryChangeRepository extends JpaRepository<DirectoryChange, Long> {
    List<DirectoryChange> findTop100ByOrderByChangedAtDesc();
}
