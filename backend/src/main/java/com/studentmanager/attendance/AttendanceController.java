package com.studentmanager.attendance;

import com.studentmanager.attendance.dto.AttendanceDtos.AttendanceResponse;
import com.studentmanager.attendance.dto.AttendanceDtos.MarkRequest;
import com.studentmanager.attendance.dto.AttendanceDtos.SubjectAttendanceSummary;
import com.studentmanager.common.ApiResponse;
import com.studentmanager.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/attendance")
public class AttendanceController {

    private final AttendanceService service;

    public AttendanceController(AttendanceService service) { this.service = service; }

    @PostMapping
    public ApiResponse<AttendanceResponse> mark(
            @AuthenticationPrincipal AuthenticatedUser me, @Valid @RequestBody MarkRequest req) {
        return ApiResponse.of(service.mark(me.userId(), req));
    }

    @DeleteMapping("/schedule/{scheduleId}")
    public ResponseEntity<Void> unmark(
            @AuthenticationPrincipal AuthenticatedUser me, @PathVariable UUID scheduleId,
            @RequestParam("date") LocalDate classDate) {
        service.unmark(me.userId(), scheduleId, classDate);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/subject/{subjectId}")
    public ApiResponse<SubjectAttendanceSummary> subjectSummary(
            @AuthenticationPrincipal AuthenticatedUser me, @PathVariable UUID subjectId) {
        return ApiResponse.of(service.summaryForSubject(me.userId(), subjectId));
    }
}
