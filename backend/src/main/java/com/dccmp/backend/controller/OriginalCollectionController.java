package com.dccmp.backend.controller;
import com.dccmp.backend.service.OriginalCollectionService;
import com.dccmp.backend.repository.StaffRepository;
import com.dccmp.backend.security.Access;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController @RequestMapping("/api/admin/original-collections")
public class OriginalCollectionController {
 private final Access access;private final StaffRepository staff;private final OriginalCollectionService service;
 public OriginalCollectionController(Access a,StaffRepository s,OriginalCollectionService v){access=a;staff=s;service=v;}
 @GetMapping("/standard")public List<String> standard(HttpServletRequest r){access.currentStaff(r,staff,"ADMIN");return OriginalCollectionService.STANDARD;}
 @GetMapping("/{id}")public Map<String,Object> get(HttpServletRequest r,@PathVariable long id){access.currentStaff(r,staff,"ADMIN");return service.summary(id);}
 @PostMapping("/{id}/initialize")public Map<String,Object> initialize(HttpServletRequest r,@PathVariable long id,@RequestBody List<OriginalCollectionService.Expected> entries){return service.initialize(access.currentStaff(r,staff,"ADMIN"),id,entries);}
 @PostMapping("/{id}/documents")public Map<String,Object> add(HttpServletRequest r,@PathVariable long id,@RequestBody OriginalCollectionService.Expected e){return service.add(access.currentStaff(r,staff,"ADMIN"),id,e);}
 public record Remove(String reason){}
 @PostMapping("/{id}/documents/{documentId}/remove")public Map<String,Object> remove(HttpServletRequest r,@PathVariable long id,@PathVariable long documentId,@RequestBody Remove body){return service.remove(access.currentStaff(r,staff,"ADMIN"),id,documentId,body.reason());}
 @PostMapping("/{id}/visits")public Map<String,Object> collect(HttpServletRequest r,@PathVariable long id,@RequestBody OriginalCollectionService.Collection body){return service.collect(access.currentStaff(r,staff,"ADMIN"),id,body);}
}
