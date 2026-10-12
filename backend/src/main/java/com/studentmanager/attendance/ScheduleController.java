package com.studentmanager.attendance;

import com.studentmanager.attendance.dto.ScheduleDtos.ScheduleRequest;
import com.studentmanager.attendance.dto.ScheduleDtos.ScheduleResponse;
import com.studentmanager.common.ApiResponse;
import com.studentmanager.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class ScheduleController {

    private final ScheduleService service;

    public ScheduleController(ScheduleService service) { this.service = service; }

    @PostMapping("/subjects/{subjectId}/schedule")
    public ResponseEntity<ApiResponse<ScheduleResponse>> create(
            @AuthenticationPrincipal AuthenticatedUser me, @PathVariable UUID subjectId,
            @Valid @RequestBody ScheduleRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(service.create(me.userId(), subjectId, req)));
    }

    @GetMapping("/subjects/{subjectId}/schedule")
    public ApiResponse<List<ScheduleResponse>> listForSubject(
            @AuthenticationPrincipal AuthenticatedUser me, @PathVariable UUID subjectId) {
        return ApiResponse.of(service.listForSubject(me.userId(), subjectId));
    }

    /** dayOfWeek: 0 = Monday ... 6 = Sunday - the full timetable for one day, across all subjects. */
    @GetMapping("/schedule/day/{dayOfWeek}")
    public ApiResponse<List<ScheduleResponse>> listForDay(
            @AuthenticationPrincipal AuthenticatedUser me, @PathVariable short dayOfWeek) {
        return ApiResponse.of(service.listForDay(me.userId(), dayOfWeek));
    }

    @DeleteMapping("/schedule/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal AuthenticatedUser me, @PathVariable UUID id) {
        service.delete(me.userId(), id);
        return ResponseEntity.noContent().build();
    }
}
