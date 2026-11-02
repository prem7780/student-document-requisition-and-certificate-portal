package com.dccmp.backend.repository;

import com.dccmp.backend.entity.StudentDirectory;

import java.util.*;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface StudentDirectoryRepository extends JpaRepository<StudentDirectory, Long> {
    Optional<StudentDirectory> findByRollNumber(String rollNumber);

    Optional<StudentDirectory> findByEmail(String email);

    List<StudentDirectory> findTop30ByRollNumberContainingIgnoreCaseOrStudentNameContainingIgnoreCaseOrderByRollNumber(String roll, String name);
}
