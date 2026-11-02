package com.dccmp.backend.service;

import com.dccmp.backend.entity.*;
import com.dccmp.backend.repository.*;
import org.apache.commons.csv.*;
import org.apache.poi.ss.usermodel.*;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import java.util.regex.Pattern;

@Service
public class StudentDirectoryService {
    private final StudentDirectoryRepository directory;
    private final DirectoryImportRepository imports;
    private final DirectoryChangeRepository changes;
    private final StudentRepository students;
    private final TransactionTemplate transactions;
    private static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}$");
    private static final java.time.ZoneId ZONE = java.time.ZoneId.of("Asia/Kolkata");

    public StudentDirectoryService(StudentDirectoryRepository d, DirectoryImportRepository i, DirectoryChangeRepository c, StudentRepository s, PlatformTransactionManager manager) {
        directory = d;
        imports = i;
        changes = c;
        students = s;
        transactions = new TransactionTemplate(manager);
    }

    public Optional<StudentDirectory> find(String roll) {
        return directory.findByRollNumber(IdentityService.normalizedRoll(roll));
    }

    public String emailFor(String roll) {
        return find(roll).map(s -> s.email).orElse(IdentityService.normalizedRoll(roll).toLowerCase(Locale.ROOT) + "@usharama.in");
    }

    public List<StudentDirectory> search(String query) {
        String q = query == null ? "" : query.trim();
        if (q.isEmpty()) return directory.findAll().stream().limit(30).toList();
        return directory.findTop30ByRollNumberContainingIgnoreCaseOrStudentNameContainingIgnoreCaseOrderByRollNumber(q, q);
    }

    public List<DirectoryImport> recentImports() {
        return imports.findTop20ByOrderByCreatedAtDesc();
    }

    public List<DirectoryChange> recentChanges() {
        return changes.findTop100ByOrderByChangedAtDesc();
    }

    public DirectoryImport getImport(long id) {
        return imports.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Import not found"));
    }

    private String clean(String s) {
        return s == null ? "" : s.trim();
    }

    private record Row(int line, String roll, String name, String branch, String email, String batch) {
    }

    public Map<String, Object> upload(MultipartFile file, long adminId, String rollRegex) {
        if (file == null || file.isEmpty()) throw WorkflowService.bad("Choose a non-empty CSV or XLSX file");
        if (file.getSize() > 5 * 1024 * 1024)
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Maximum file size is 5 MB");
        String filename = Objects.toString(file.getOriginalFilename(), "import");
        String ext = filename.toLowerCase(Locale.ROOT);
        if (!ext.endsWith(".csv") && !ext.endsWith(".xlsx"))
            throw WorkflowService.bad("Only .csv and .xlsx files are supported");
        List<Row> rows;
        try {
            rows = ext.endsWith(".csv") ? readCsv(file) : readExcel(file);
        } catch (Exception ex) {
            throw WorkflowService.bad("Cannot read the uploaded file: " + ex.getMessage());
        }
        if (rows.size() > 10000) throw WorkflowService.bad("Upload at most 10,000 records per file");
        DirectoryImport batch = new DirectoryImport();
        batch.adminId = adminId;
        batch.fileName = filename;
        batch.createdAt = LocalDateTime.now(ZONE);
        batch.totalRows = rows.size();
        batch.createdRows = 0;
        batch.updatedRows = 0;
        batch.unchangedRows = 0;
        batch.invalidRows = 0;
        batch = imports.save(batch);
        StringBuilder err = new StringBuilder("row,roll_number,error\n");
        Set<String> seen = new HashSet<>();
        for (Row r : rows) {
            String roll = IdentityService.normalizedRoll(r.roll), name = clean(r.name), branch = clean(r.branch), email = clean(r.email).toLowerCase(Locale.ROOT), batchYear = clean(r.batch);
            String failure = null;
            if (!roll.matches(rollRegex)) failure = "Invalid or missing roll number";
            else if (!seen.add(roll)) failure = "Duplicate roll number in file";
            else if (name.isEmpty() || name.length() > 150) failure = "Student name is required (max 150 characters)";
            else if (branch.isEmpty() || branch.length() > 80) failure = "Branch is required (max 80 characters)";
            else if (!EMAIL.matcher(email).matches() || email.length() > 200) failure = "Invalid or missing email";
                // Admin-imported mappings may contain name-based and external addresses (for example Gmail).
                // The OTP workflow still requires an exact match with the email mapped to this roll number.
            else if (batchYear.length() > 30) failure = "Batch is too long";
            else if (directory.findByEmail(email).filter(d -> !roll.equals(d.rollNumber)).isPresent())
                failure = "Email belongs to another roll number";
            if (failure == null) {
                try {
                    final long batchId = batch.id;
                    String operation = transactions.execute(tx -> {
                        StudentDirectory d = directory.findByRollNumber(roll).orElse(null);
                        if (d == null) {
                            d = new StudentDirectory();
                            d.rollNumber = roll;
                            d.studentName = name;
                            d.branch = branch;
                            d.email = email;
                            d.batchYear = batchYear;
                            d.updatedAt = LocalDateTime.now(ZONE);
                            d.updatedBy = adminId;
                            directory.saveAndFlush(d);
                            return "CREATED";
                        }
                        boolean changed = false;
                        changed |= change(d, "studentName", d.studentName, name, adminId, batchId);
                        changed |= change(d, "branch", d.branch, branch, adminId, batchId);
                        changed |= change(d, "email", d.email, email, adminId, batchId);
                        changed |= change(d, "batchYear", d.batchYear, batchYear, adminId, batchId);
                        if (changed) {
                            d.studentName = name;
                            d.branch = branch;
                            d.email = email;
                            d.batchYear = batchYear;
                            d.updatedAt = LocalDateTime.now(ZONE);
                            d.updatedBy = adminId;
                            directory.saveAndFlush(d);
                            return "UPDATED";
                        }
                        return "UNCHANGED";
                    });
                    if ("CREATED".equals(operation)) batch.createdRows++;
                    else if ("UPDATED".equals(operation)) batch.updatedRows++;
                    else batch.unchangedRows++;
                } catch (org.springframework.dao.DataIntegrityViolationException e) {
                    failure = "Duplicate email or database uniqueness conflict";
                } catch (Exception ex) {
                    failure = "Database error (check server log)";
                }
            }
            if (failure != null) {
                batch.invalidRows++;
                err.append(r.line).append(',').append(csvEscape(roll)).append(',').append(csvEscape(failure)).append('\n');
            }
        }
        batch.errorCsv = err.toString();
        imports.save(batch);
        return Map.of("importId", batch.id, "totalRows", batch.totalRows, "created", batch.createdRows, "updated", batch.updatedRows, "unchanged", batch.unchangedRows, "invalid", batch.invalidRows);
    }

    private boolean change(StudentDirectory d, String field, String old, String updated, long admin, long batch) {
        if (Objects.equals(clean(old), clean(updated))) return false;
        DirectoryChange c = new DirectoryChange();
        c.directoryId = d.id;
        c.rollNumber = d.rollNumber;
        c.fieldName = field;
        c.oldValue = old;
        c.newValue = updated;
        c.adminId = admin;
        c.importId = batch;
        c.changedAt = LocalDateTime.now(ZONE);
        changes.save(c);
        return true;
    }

    private String csvEscape(String s) {
        return "\"" + Objects.toString(s, "").replace("\"", "\"\"") + "\"";
    }

    private List<Row> readCsv(MultipartFile file) throws Exception {
        try (Reader input = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).setIgnoreSurroundingSpaces(true).get().parse(input)) {
            List<Row> out = new ArrayList<>();
            Map<String, Integer> cols = columns(parser.getHeaderNames());
            for (CSVRecord r : parser) {
                out.add(new Row((int) r.getRecordNumber() + 1, get(r, cols, "roll"), get(r, cols, "name"), get(r, cols, "branch"), get(r, cols, "email"), get(r, cols, "batch")));
            }
            return out;
        }
    }

    private List<Row> readExcel(MultipartFile file) throws Exception {
        try (InputStream in = file.getInputStream(); Workbook workbook = WorkbookFactory.create(in)) {
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<org.apache.poi.ss.usermodel.Row> it = sheet.iterator();
            if (!it.hasNext()) throw new IllegalArgumentException("Missing header row");
            DataFormatter formatter = new DataFormatter(Locale.ENGLISH);
            org.apache.poi.ss.usermodel.Row header = it.next();
            List<String> headers = new ArrayList<>();
            for (int i = 0; i < header.getLastCellNum(); i++) headers.add(formatter.formatCellValue(header.getCell(i)));
            Map<String, Integer> cols = columns(headers);
            List<Row> result = new ArrayList<>();
            while (it.hasNext()) {
                org.apache.poi.ss.usermodel.Row line = it.next();
                if (result.size() > 10000) break;
                String roll = get(line, cols, "roll", formatter), name = get(line, cols, "name", formatter), branch = get(line, cols, "branch", formatter), email = get(line, cols, "email", formatter), batch = get(line, cols, "batch", formatter);
                if (roll.isBlank() && name.isBlank() && branch.isBlank() && email.isBlank()) continue;
                result.add(new Row(line.getRowNum() + 1, roll, name, branch, email, batch));
            }
            return result;
        }
    }

    private Map<String, Integer> columns(List<String> heads) {
        Map<String, Integer> found = new HashMap<>();
        for (int i = 0; i < heads.size(); i++) {
            String h = heads.get(i).replace("\uFEFF", "").replaceAll("[^A-Za-z0-9]", "").toLowerCase(Locale.ROOT);
            if (Set.of("rollnumber", "rollno", "roll", "registrationnumber").contains(h)) found.put("roll", i);
            if (Set.of("studentname", "name").contains(h)) found.put("name", i);
            if (Set.of("branch", "department").contains(h)) found.put("branch", i);
            if (Set.of("email", "emailaddress", "collegeemail").contains(h)) found.put("email", i);
            if (Set.of("batch", "batchyear", "year").contains(h)) found.put("batch", i);
        }
        for (String k : List.of("roll", "name", "branch", "email"))
            if (!found.containsKey(k)) throw new IllegalArgumentException("Missing required column: " + k);
        return found;
    }

    private String get(CSVRecord r, Map<String, Integer> c, String key) {
        Integer n = c.get(key);
        return n == null || n >= r.size() ? "" : r.get(n);
    }

    private String get(org.apache.poi.ss.usermodel.Row r, Map<String, Integer> c, String key, DataFormatter f) {
        Integer n = c.get(key);
        return n == null ? "" : f.formatCellValue(r.getCell(n));
    }
}
