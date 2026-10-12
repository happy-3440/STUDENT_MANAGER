package com.studentmanager.academic;

import com.studentmanager.academic.dto.AcademicDashboardResponse;
import com.studentmanager.academic.dto.SemesterDtos.SemesterRequest;
import com.studentmanager.academic.dto.SemesterDtos.SemesterResponse;
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
@RequestMapping("/api/v1/semesters")
public class SemesterController {

    private final SemesterService semesterService;
    private final AcademicDashboardService dashboardService;

    public SemesterController(SemesterService semesterService, AcademicDashboardService dashboardService) {
        this.semesterService = semesterService;
        this.dashboardService = dashboardService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SemesterResponse>> create(
            @AuthenticationPrincipal AuthenticatedUser me, @Valid @RequestBody SemesterRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(semesterService.create(me.userId(), req)));
    }

    @GetMapping
    public ApiResponse<List<SemesterResponse>> list(@AuthenticationPrincipal AuthenticatedUser me) {
        return ApiResponse.of(semesterService.list(me.userId()));
    }

    @GetMapping("/{id}/dashboard")
    public ApiResponse<AcademicDashboardResponse> dashboard(
            @AuthenticationPrincipal AuthenticatedUser me, @PathVariable UUID id) {
        semesterService.getOwned(me.userId(), id); // 404s if not theirs
        return ApiResponse.of(dashboardService.forSemester(me.userId(), id));
    }
}
