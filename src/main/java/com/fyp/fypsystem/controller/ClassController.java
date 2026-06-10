package com.fyp.fypsystem.controller;

import com.fyp.fypsystem.model.*;
import com.fyp.fypsystem.repository.*;
import com.fyp.fypsystem.security.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/classes")
public class ClassController {

    private final ClassSlotRepository slotRepo;
    private final ClassEnrollmentRepository enrollRepo;
    private final ClassAttendanceRepository attendanceRepo;
    private final UserRepository userRepo;
    private final JwtUtil jwtUtil;

    public ClassController(ClassSlotRepository slotRepo,
                           ClassEnrollmentRepository enrollRepo,
                           ClassAttendanceRepository attendanceRepo,
                           UserRepository userRepo,
                           JwtUtil jwtUtil) {
        this.slotRepo = slotRepo;
        this.enrollRepo = enrollRepo;
        this.attendanceRepo = attendanceRepo;
        this.userRepo = userRepo;
        this.jwtUtil = jwtUtil;
    }

    // ── GET /api/classes/coach ──────────────────────────────────────────
    @GetMapping("/coach")
    public ResponseEntity<?> getCoachClasses(@RequestHeader("Authorization") String auth) {
        User coach = resolve(auth);
        if (coach == null) return ResponseEntity.status(401).body(err("Unauthorized"));

        List<ClassSlot> slots = slotRepo.findByCoachIdOrderByDayOfWeekAscStartTimeAsc(coach.getId());
        List<Map<String, Object>> result = new ArrayList<>();
        for (ClassSlot s : slots) {
            Map<String, Object> m = slotToMap(s);
            List<ClassEnrollment> students = enrollRepo.findByClassSlotId(s.getId());
            m.put("students", students);
            m.put("activeCount", students.stream().filter(e -> "ACTIVE".equals(e.getStatus())).count());
            result.add(m);
        }
        return ResponseEntity.ok(result);
    }

    // ── GET /api/classes/{id} ───────────────────────────────────────────
    @GetMapping("/{id}")
    public ResponseEntity<?> getOne(@PathVariable Long id,
                                    @RequestHeader("Authorization") String auth) {
        User coach = resolve(auth);
        if (coach == null) return ResponseEntity.status(401).body(err("Unauthorized"));
        ClassSlot s = slotRepo.findById(id).orElse(null);
        if (s == null) return ResponseEntity.notFound().build();
        if (!s.getCoachId().equals(coach.getId()) && !"ADMIN".equals(coach.getRole()))
            return ResponseEntity.status(403).body(err("Forbidden"));

        Map<String, Object> m = slotToMap(s);
        m.put("students", enrollRepo.findByClassSlotId(id));
        m.put("attendance", attendanceRepo.findByClassSlotIdOrderBySessionDateDesc(id));
        return ResponseEntity.ok(m);
    }

    // ── POST /api/classes ───────────────────────────────────────────────
    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, Object> body,
                                    @RequestHeader("Authorization") String auth) {
        User coach = resolve(auth);
        if (coach == null) return ResponseEntity.status(401).body(err("Unauthorized"));

        ClassSlot s = new ClassSlot();
        s.setCoachId(coach.getId());
        s.setCoachName(coach.getName());
        s.setName(str(body, "name"));
        s.setDayOfWeek(str(body, "dayOfWeek"));
        s.setStartTime(str(body, "startTime"));
        s.setEndTime(str(body, "endTime"));
        s.setClassType(str(body, "classType") != null ? str(body, "classType") : "Individual");
        s.setZoomAccount(str(body, "zoomAccount"));
        s.setZoomLink(str(body, "zoomLink"));
        s.setMeetingId(str(body, "meetingId"));
        s.setSessionDates(str(body, "sessionDates"));
        if (body.get("monthlySessionCount") != null)
            s.setMonthlySessionCount(toInt(body.get("monthlySessionCount")));
        s.setStatus("ACTIVE");
        return ResponseEntity.ok(slotRepo.save(s));
    }

    // ── PUT /api/classes/{id} ───────────────────────────────────────────
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id,
                                    @RequestBody Map<String, Object> body,
                                    @RequestHeader("Authorization") String auth) {
        User coach = resolve(auth);
        if (coach == null) return ResponseEntity.status(401).body(err("Unauthorized"));
        ClassSlot s = slotRepo.findById(id).orElse(null);
        if (s == null) return ResponseEntity.notFound().build();
        if (!s.getCoachId().equals(coach.getId())) return ResponseEntity.status(403).body(err("Forbidden"));

        if (body.containsKey("status")) s.setStatus(str(body, "status"));
        if (body.containsKey("sessionDates")) s.setSessionDates(str(body, "sessionDates"));
        if (body.containsKey("zoomLink")) s.setZoomLink(str(body, "zoomLink"));
        if (body.containsKey("zoomAccount")) s.setZoomAccount(str(body, "zoomAccount"));
        if (body.containsKey("meetingId")) s.setMeetingId(str(body, "meetingId"));
        if (body.containsKey("startTime")) s.setStartTime(str(body, "startTime"));
        if (body.containsKey("endTime")) s.setEndTime(str(body, "endTime"));
        return ResponseEntity.ok(slotRepo.save(s));
    }

    // ── DELETE /api/classes/{id} ────────────────────────────────────────
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> delete(@PathVariable Long id,
                                    @RequestHeader("Authorization") String auth) {
        User coach = resolve(auth);
        if (coach == null) return ResponseEntity.status(401).body(err("Unauthorized"));
        ClassSlot s = slotRepo.findById(id).orElse(null);
        if (s == null) return ResponseEntity.notFound().build();
        if (!s.getCoachId().equals(coach.getId()) && !"ADMIN".equals(coach.getRole()))
            return ResponseEntity.status(403).body(err("Forbidden"));
        enrollRepo.deleteByClassSlotId(id);
        attendanceRepo.deleteByClassSlotId(id);
        slotRepo.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Class deleted"));
    }

    // ── POST /api/classes/{id}/enroll ───────────────────────────────────
    @PostMapping("/{id}/enroll")
    public ResponseEntity<?> enroll(@PathVariable Long id,
                                    @RequestBody Map<String, Object> body,
                                    @RequestHeader("Authorization") String auth) {
        User coach = resolve(auth);
        if (coach == null) return ResponseEntity.status(401).body(err("Unauthorized"));
        Long studentId = toLong(body.get("studentId"));
        if (studentId == null) return ResponseEntity.badRequest().body(err("studentId required"));
        if (enrollRepo.findByClassSlotIdAndStudentId(id, studentId).isPresent())
            return ResponseEntity.badRequest().body(err("Student already enrolled"));

        User student = userRepo.findById(studentId).orElse(null);
        ClassEnrollment e = new ClassEnrollment();
        e.setClassSlotId(id);
        e.setStudentId(studentId);
        e.setStudentName(student != null ? student.getName() : "Unknown");
        e.setStudentEmail(student != null ? student.getEmail() : "");
        e.setStatus("PENDING");
        return ResponseEntity.ok(enrollRepo.save(e));
    }

    // ── PUT /api/classes/{id}/students/{studentId}/status ───────────────
    @PutMapping("/{id}/students/{studentId}/status")
    public ResponseEntity<?> setStudentStatus(@PathVariable Long id,
                                               @PathVariable Long studentId,
                                               @RequestBody Map<String, Object> body,
                                               @RequestHeader("Authorization") String auth) {
        User coach = resolve(auth);
        if (coach == null) return ResponseEntity.status(401).body(err("Unauthorized"));
        ClassEnrollment e = enrollRepo.findByClassSlotIdAndStudentId(id, studentId).orElse(null);
        if (e == null) return ResponseEntity.notFound().build();
        e.setStatus(str(body, "status"));
        return ResponseEntity.ok(enrollRepo.save(e));
    }

    // ── DELETE /api/classes/{id}/students/{studentId} ───────────────────
    @DeleteMapping("/{id}/students/{studentId}")
    public ResponseEntity<?> removeStudent(@PathVariable Long id,
                                           @PathVariable Long studentId,
                                           @RequestHeader("Authorization") String auth) {
        User coach = resolve(auth);
        if (coach == null) return ResponseEntity.status(401).body(err("Unauthorized"));
        ClassEnrollment e = enrollRepo.findByClassSlotIdAndStudentId(id, studentId).orElse(null);
        if (e == null) return ResponseEntity.notFound().build();
        enrollRepo.delete(e);
        return ResponseEntity.ok(Map.of("message", "Student removed"));
    }

    // ── POST /api/classes/{id}/attendance ───────────────────────────────
    @PostMapping("/{id}/attendance")
    public ResponseEntity<?> recordAttendance(@PathVariable Long id,
                                               @RequestBody Map<String, Object> body,
                                               @RequestHeader("Authorization") String auth) {
        User coach = resolve(auth);
        if (coach == null) return ResponseEntity.status(401).body(err("Unauthorized"));
        Long studentId = toLong(body.get("studentId"));
        String dateStr  = str(body, "sessionDate");
        if (studentId == null || dateStr == null)
            return ResponseEntity.badRequest().body(err("studentId and sessionDate required"));

        LocalDate date = LocalDate.parse(dateStr);
        ClassAttendance att = attendanceRepo
                .findByClassSlotIdAndStudentIdAndSessionDate(id, studentId, date)
                .orElseGet(ClassAttendance::new);
        att.setClassSlotId(id);
        att.setStudentId(studentId);
        att.setSessionDate(date);
        att.setPresent(body.get("present") == null || Boolean.TRUE.equals(body.get("present")));
        att.setRecordedAt(LocalDateTime.now());
        if (att.getStudentName() == null) {
            User st = userRepo.findById(studentId).orElse(null);
            att.setStudentName(st != null ? st.getName() : "Unknown");
        }
        return ResponseEntity.ok(attendanceRepo.save(att));
    }

    // ── GET /api/classes/{id}/attendance ────────────────────────────────
    @GetMapping("/{id}/attendance")
    public ResponseEntity<?> getAttendance(@PathVariable Long id,
                                            @RequestHeader("Authorization") String auth) {
        User coach = resolve(auth);
        if (coach == null) return ResponseEntity.status(401).body(err("Unauthorized"));
        return ResponseEntity.ok(attendanceRepo.findByClassSlotIdOrderBySessionDateDesc(id));
    }

    // ── helpers ─────────────────────────────────────────────────────────
    private User resolve(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) return null;
        try {
            String email = jwtUtil.extractEmail(authHeader.substring(7));
            return userRepo.findByEmail(email).orElse(null);
        } catch (Exception e) { return null; }
    }

    private Map<String, Object> slotToMap(ClassSlot s) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", s.getId());
        m.put("name", s.getName());
        m.put("dayOfWeek", s.getDayOfWeek());
        m.put("startTime", s.getStartTime());
        m.put("endTime", s.getEndTime());
        m.put("classType", s.getClassType());
        m.put("zoomAccount", s.getZoomAccount());
        m.put("zoomLink", s.getZoomLink());
        m.put("meetingId", s.getMeetingId());
        m.put("status", s.getStatus());
        m.put("monthlySessionCount", s.getMonthlySessionCount());
        m.put("sessionDates", s.getSessionDates());
        return m;
    }

    private String str(Map<String, Object> m, String key) {
        Object v = m.get(key); return v != null ? v.toString().trim() : null;
    }
    private Long toLong(Object v) {
        if (v == null) return null;
        try { return Long.parseLong(v.toString()); } catch (Exception e) { return null; }
    }
    private Integer toInt(Object v) {
        if (v == null) return null;
        try { return Integer.parseInt(v.toString()); } catch (Exception e) { return null; }
    }
    private Map<String, String> err(String msg) { return Map.of("error", msg); }
}
