package com.studentmanager.academic;

import com.studentmanager.academic.dto.AssignmentDtos.AssignmentRequest;
import com.studentmanager.academic.dto.AssignmentDtos.AssignmentResponse;
import com.studentmanager.academic.dto.AssignmentDtos.AssignmentUpdateRequest;
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
public class AssignmentController {

    private final AssignmentService service;

    public AssignmentController(AssignmentService service) { this.service = service; }

    @PostMapping("/subjects/{subjectId}/assignments")
    public ResponseEntity<ApiResponse<AssignmentResponse>> create(
            @AuthenticationPrincipal AuthenticatedUser me, @PathVariable UUID subjectId,
            @Valid @RequestBody AssignmentRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(service.create(me.userId(), subjectId, req)));
    }

    @GetMapping("/subjects/{subjectId}/assignments")
    public ApiResponse<List<AssignmentResponse>> list(
            @AuthenticationPrincipal AuthenticatedUser me, @PathVariable UUID subjectId) {
        return ApiResponse.of(service.listBySubject(me.userId(), subjectId));
    }

    @PatchMapping("/assignments/{id}")
    public ApiResponse<AssignmentResponse> update(
            @AuthenticationPrincipal AuthenticatedUser me, @PathVariable UUID id,
            @Valid @RequestBody AssignmentUpdateRequest req) {
        return ApiResponse.of(service.update(me.userId(), id, req));
    }

    @DeleteMapping("/assignments/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal AuthenticatedUser me, @PathVariable UUID id) {
        service.delete(me.userId(), id);
        return ResponseEntity.noContent().build();
    }
}
