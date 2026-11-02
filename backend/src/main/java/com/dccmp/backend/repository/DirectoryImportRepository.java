package com.dccmp.backend.repository;
import com.dccmp.backend.entity.DirectoryImport;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface DirectoryImportRepository extends JpaRepository<DirectoryImport,Long> { List<DirectoryImport> findTop20ByOrderByCreatedAtDesc(); }
