package com.dccmp.backend.controller;

import com.dccmp.backend.security.Access;
import com.dccmp.backend.repository.StaffRepository;
import com.dccmp.backend.entity.*;
import com.dccmp.backend.service.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.*;

@RestController
@RequestMapping("/api/admin/directory")
public class StudentDirectoryController {
    private final Access access;
    private final StaffRepository staff;
    private final StudentDirectoryService directory;
    @Value("${app.student-roll-regex}")
    private String rollRegex;

    public StudentDirectoryController(Access a, StaffRepository s, StudentDirectoryService d) {
        access = a;
        staff = s;
        directory = d;
    }

    @GetMapping
    public List<StudentDirectory> list(HttpServletRequest r, @RequestParam(defaultValue = "") String q) {
        access.currentStaff(r, staff, "ADMIN");
        return directory.search(q);
    }

    @GetMapping("/imports")
    public List<DirectoryImport> imports(HttpServletRequest r) {
        access.currentStaff(r, staff, "ADMIN");
        return directory.recentImports();
    }

    @GetMapping("/changes")
    public List<DirectoryChange> changes(HttpServletRequest r) {
        access.currentStaff(r, staff, "ADMIN");
        return directory.recentChanges();
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> upload(HttpServletRequest r, @RequestPart("file") MultipartFile file) {
        Staff admin = access.currentStaff(r, staff, "ADMIN");
        return directory.upload(file, admin.id, rollRegex);
    }

    @GetMapping("/imports/{id}/errors")
    public ResponseEntity<byte[]> errors(HttpServletRequest r, @PathVariable long id) {
        access.currentStaff(r, staff, "ADMIN");
        return ResponseEntity.ok().contentType(MediaType.parseMediaType("text/csv; charset=UTF-8")).header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=directory-errors-" + id + ".csv").body(directory.getImport(id).errorCsv.getBytes(StandardCharsets.UTF_8));
    }

    @GetMapping("/template")
    public ResponseEntity<byte[]> template(HttpServletRequest r) {
        access.currentStaff(r, staff, "ADMIN");
        return ResponseEntity.ok().contentType(MediaType.parseMediaType("text/csv; charset=UTF-8")).header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=student-directory-template.csv").body("Roll Number,Student Name,Branch,Email,Batch\n".getBytes(StandardCharsets.UTF_8));
    }
}
