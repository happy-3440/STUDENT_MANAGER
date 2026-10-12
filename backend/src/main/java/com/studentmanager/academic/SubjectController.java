package com.studentmanager.academic;

import com.studentmanager.academic.dto.SubjectDtos.MarksRequest;
import com.studentmanager.academic.dto.SubjectDtos.SubjectRequest;
import com.studentmanager.academic.dto.SubjectDtos.SubjectResponse;
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
public class SubjectController {

    private final SubjectService subjectService;

    public SubjectController(SubjectService subjectService) {
        this.subjectService = subjectService;
    }

    @PostMapping("/semesters/{semesterId}/subjects")
    public ResponseEntity<ApiResponse<SubjectResponse>> create(
            @AuthenticationPrincipal AuthenticatedUser me, @PathVariable UUID semesterId,
            @Valid @RequestBody SubjectRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(subjectService.create(me.userId(), semesterId, req)));
    }

    @GetMapping("/semesters/{semesterId}/subjects")
    public ApiResponse<List<SubjectResponse>> listForSemester(
            @AuthenticationPrincipal AuthenticatedUser me, @PathVariable UUID semesterId) {
        return ApiResponse.of(subjectService.listBySemester(me.userId(), semesterId));
    }

    @PutMapping("/subjects/{id}")
    public ApiResponse<SubjectResponse> updateBasics(
            @AuthenticationPrincipal AuthenticatedUser me, @PathVariable UUID id, @Valid @RequestBody SubjectRequest req) {
        return ApiResponse.of(subjectService.updateBasics(me.userId(), id, req));
    }

    @PutMapping("/subjects/{id}/marks")
    public ApiResponse<SubjectResponse> updateMarks(
            @AuthenticationPrincipal AuthenticatedUser me, @PathVariable UUID id, @Valid @RequestBody MarksRequest req) {
        return ApiResponse.of(subjectService.updateMarks(me.userId(), id, req));
    }

    @DeleteMapping("/subjects/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal AuthenticatedUser me, @PathVariable UUID id) {
        subjectService.delete(me.userId(), id);
        return ResponseEntity.noContent().build();
    }
}
