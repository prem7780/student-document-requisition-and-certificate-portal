package com.dccmp.backend.service;
import com.dccmp.backend.entity.*;
import com.dccmp.backend.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class CertificateRulesTest {
 CertificateRequestRepository requests=mock(CertificateRequestRepository.class);
 CertificateEventRepository events=mock(CertificateEventRepository.class);
 CertificateNotificationRepository notifications=mock(CertificateNotificationRepository.class);
 DepartmentRepository departments=mock(DepartmentRepository.class);
 StudentRepository students=mock(StudentRepository.class);
 StudentDirectoryService directory=mock(StudentDirectoryService.class);
 CertificateTypeRepository types=mock(CertificateTypeRepository.class);
 JavaMailSender mail=mock(JavaMailSender.class);
 CertificateService service=new CertificateService(requests,events,notifications,departments,students,directory,mail,types);
 private Staff hod(){Staff s=new Staff();s.id=1L;s.role="STAFF";s.departmentId=4L;return s;}
 private CertificateRequest request(String status){CertificateRequest c=new CertificateRequest();c.id=80L;c.studentId=11L;c.rollNumber="23NG1A0501";c.hodDepartmentId=4L;c.status=status;c.certificateType="STUDY";return c;}
 @Test void approvedRequestExpiresAfterThirtyDays(){CertificateRequest r=request("APPROVED");r.expiresAt=LocalDateTime.now(ZoneId.of("Asia/Kolkata")).minusSeconds(1);assertEquals("EXPIRED",service.effectiveStatus(r,LocalDateTime.now(ZoneId.of("Asia/Kolkata"))));}
 @Test void cannotCollectUnverifiedStudent(){Staff a=new Staff();a.role="ADMIN";a.id=8L;assertThrows(ResponseStatusException.class,()->service.collect(a,80L,new CertificateService.Handover(false,"")));}
 @Test void hodCannotApproveUnassignedBranch(){Staff s=hod();Department hodDepartment=new Department();hodDepartment.code="hod_cse";when(departments.findById(4L)).thenReturn(Optional.of(hodDepartment));
 CertificateRequest r=request("PENDING_HOD");r.hodDepartmentId=3L;when(requests.lockById(80L)).thenReturn(Optional.of(r));assertThrows(ResponseStatusException.class,()->service.decideHod(s,80L,true,""));}
 @Test void hodMustSupplyRejectionReason(){Staff s=hod();Department hodDepartment=new Department();hodDepartment.code="hod_cse";when(departments.findById(4L)).thenReturn(Optional.of(hodDepartment));
 when(requests.lockById(80L)).thenReturn(Optional.of(request("PENDING_HOD")));assertThrows(ResponseStatusException.class,()->service.decideHod(s,80L,false," "));}
 @Test void duplicatePendingCertificateBlocked(){Student st=new Student();st.id=11L;st.rollNumber="23NG1A0501";when(students.lockById(11L)).thenReturn(Optional.of(st));
 Department d=new Department();d.id=4L;d.code="hod_cse";d.active=true;when(departments.findByCode("hod_cse")).thenReturn(Optional.of(d));
 CertificateType t=new CertificateType();t.code="STUDY";t.active=true;when(types.findByCode("STUDY")).thenReturn(Optional.of(t));
 when(requests.findByStudentIdOrderBySubmittedAtDesc(11L)).thenReturn(List.of(request("PENDING_HOD")));
 assertThrows(ResponseStatusException.class,()->service.submit(11L,new CertificateService.Input("STUDY","CSE","purpose","","Test Student")));
 }
}
