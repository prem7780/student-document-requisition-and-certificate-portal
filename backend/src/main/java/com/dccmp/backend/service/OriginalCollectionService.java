package com.dccmp.backend.service;

import com.dccmp.backend.entity.*;
import com.dccmp.backend.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.*;
import java.util.*;

@Service
public class OriginalCollectionService {
    public static final List<String> STANDARD = List.of("SSC Original", "Intermediate Original", "Transfer Certificate", "Original Marks Memo", "Other Original Document");
    private final OriginalDocumentRepository docs;
    private final OriginalVisitRepository visits;
    private final OriginalVisitItemRepository items;
    private final OriginalChangeRepository changes;
    private final ApplicationRepository applications;
    private final GeneratedFormRepository forms;
    private final StudentRepository students;
    private static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");

    public OriginalCollectionService(OriginalDocumentRepository d, OriginalVisitRepository v, OriginalVisitItemRepository i, OriginalChangeRepository c, ApplicationRepository a, GeneratedFormRepository f, StudentRepository s) {
        docs = d;
        visits = v;
        items = i;
        changes = c;
        applications = a;
        forms = f;
        students = s;
    }

    public record Expected(String name, int quantity) {
    }

    public record Given(Long documentId, int quantity) {
    }

    public record Collection(boolean identityVerified, String remarks, List<Given> items) {
    }

    private LocalDateTime now() {
        return LocalDateTime.now(ZONE);
    }

    private Application approved(long id) {
        Application a = applications.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No Dues application not found"));
        if (!"APPROVED".equals(a.status) || forms.findByApplicationId(id).isEmpty())
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Approved No Dues with issued PDF is required");
        return a;
    }

    private void change(Long app, Long document, Long admin, String action, String description) {
        OriginalChange c = new OriginalChange();
        c.applicationId = app;
        c.documentId = document;
        c.adminId = admin;
        c.action = action;
        c.details = description;
        c.occurredAt = now();
        changes.save(c);
    }

    public Map<String, Object> summary(long id) {
        approved(id);
        List<OriginalDocument> d = docs.findByApplicationIdOrderById(id);
        return Map.of("initialized", !d.isEmpty(), "documents", d, "visits", visits.findByApplicationIdOrderByHandedOverAtAsc(id).stream().map(v -> Map.of("visit", v, "items", items.findByVisitId(v.id))).toList(), "history", changes.findByApplicationIdOrderByOccurredAtAsc(id));
    }

    @Transactional
    public Map<String, Object> initialize(Staff admin, long id, List<Expected> requested) {
        approved(id);
        if (!docs.findByApplicationIdOrderById(id).isEmpty())
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Document checklist already initialized");
        if (requested == null || requested.isEmpty() || requested.size() > 30)
            throw WorkflowService.bad("Provide 1 to 30 expected original documents");
        Set<String> names = new HashSet<>();
        for (Expected e : requested) {
            String name = WorkflowService.mandatory(e.name(), "Document name");
            if (name.length() > 120 || !names.add(name.toLowerCase(Locale.ROOT)) || e.quantity() < 1 || e.quantity() > 20)
                throw WorkflowService.bad("Invalid or duplicate document entry");
            OriginalDocument d = new OriginalDocument();
            d.applicationId = id;
            d.documentName = name;
            d.expectedQuantity = e.quantity();
            d.collectedQuantity = 0;
            d.active = true;
            d.createdAt = now();
            d = docs.save(d);
            change(id, d.id, admin.id, "ADD_EXPECTED", name + " (" + e.quantity() + ")");
        }
        return summary(id);
    }

    @Transactional
    public Map<String, Object> add(Staff admin, long id, Expected e) {
        approved(id);
        if (docs.findByApplicationIdOrderById(id).isEmpty())
            throw WorkflowService.bad("Initialize the document checklist first");
        String name = WorkflowService.mandatory(e.name(), "Document name");
        if (name.length() > 120 || e.quantity() < 1 || e.quantity() > 20)
            throw WorkflowService.bad("Invalid expected document quantity");
        if (docs.findByApplicationIdOrderById(id).stream().anyMatch(x -> Boolean.TRUE.equals(x.active) && x.documentName.equalsIgnoreCase(name)))
            throw WorkflowService.conflict("This document already exists in the checklist");
        OriginalDocument d = new OriginalDocument();
        d.applicationId = id;
        d.documentName = name;
        d.expectedQuantity = e.quantity();
        d.collectedQuantity = 0;
        d.active = true;
        d.createdAt = now();
        d = docs.save(d);
        change(id, d.id, admin.id, "ADD_EXPECTED", name + " (" + e.quantity() + ")");
        return summary(id);
    }

    @Transactional
    public Map<String, Object> remove(Staff admin, long id, long documentId, String reason) {
        approved(id);
        OriginalDocument d = docs.findById(documentId).orElseThrow(() -> WorkflowService.bad("Document not found"));
        if (!Objects.equals(d.applicationId, id)) throw WorkflowService.bad("Document does not belong to application");
        if (d.collectedQuantity > 0) throw WorkflowService.conflict("A collected document cannot be removed");
        if (!Boolean.TRUE.equals(d.active)) throw WorkflowService.conflict("Document already removed");
        d.active = false;
        docs.save(d);
        change(id, d.id, admin.id, "REMOVE_EXPECTED", d.documentName + ": " + WorkflowService.mandatory(reason, "Reason"));
        return summary(id);
    }

    @Transactional
    public Map<String, Object> collect(Staff admin, long id, Collection body) {
        approved(id);
        if (body == null || !body.identityVerified())
            throw WorkflowService.bad("Confirm student identity before handover");
        List<OriginalDocument> available = docs.findByApplicationIdOrderById(id);
        if (available.isEmpty()) throw WorkflowService.bad("Initialize expected documents at first collection visit");
        if (body.items() == null || body.items().isEmpty())
            throw WorkflowService.bad("Select at least one document handed over");
        Map<Long, OriginalDocument> index = new HashMap<>();
        for (OriginalDocument d : available) index.put(d.id, d);
        Set<Long> seen = new HashSet<>();
        for (Given g : body.items()) {
            if (g.documentId() == null || !seen.add(g.documentId()))
                throw WorkflowService.bad("Duplicate or invalid handover document");
            OriginalDocument d = index.get(g.documentId());
            if (d == null || !Boolean.TRUE.equals(d.active) || g.quantity() < 1 || g.quantity() > d.expectedQuantity - d.collectedQuantity)
                throw WorkflowService.conflict("Invalid quantity or already collected document");
        }
        OriginalVisit v = new OriginalVisit();
        v.applicationId = id;
        v.adminId = admin.id;
        v.identityVerified = true;
        v.remarks = WorkflowService.optional(body.remarks());
        v.handedOverAt = now();
        v = visits.save(v);
        for (Given g : body.items()) {
            OriginalDocument d = index.get(g.documentId());
            d.collectedQuantity += g.quantity();
            docs.save(d);
            OriginalVisitItem item = new OriginalVisitItem();
            item.visitId = v.id;
            item.documentId = d.id;
            item.quantity = g.quantity();
            items.save(item);
            change(id, d.id, admin.id, "HANDOVER", d.documentName + " quantity " + g.quantity() + " visit #" + v.id);
        }
        return summary(id);
    }
}
