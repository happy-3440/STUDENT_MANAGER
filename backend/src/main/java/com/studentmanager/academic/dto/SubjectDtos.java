package com.studentmanager.academic.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.UUID;

public final class SubjectDtos {
    private SubjectDtos() {}

    public record SubjectRequest(
            @NotBlank @Size(max = 100) String name,
            @NotBlank @Size(max = 100) String teacher,
            @Min(50) @Max(100) Short requiredAttendancePct
    ) {}

    public record MarksRequest(
            @DecimalMin("0.5") @DecimalMax("10") BigDecimal credits,
            Boolean quizOn,
            @DecimalMin("0") @DecimalMax("10") BigDecimal quizMarks,
            Boolean tutorialOn,
            @DecimalMin("0") @DecimalMax("10") BigDecimal tutorialMarks,
            @DecimalMin("0") @DecimalMax("15") BigDecimal midMarks,
            @DecimalMin("0") @DecimalMax("60") BigDecimal endMarks
    ) {}

    public record SubjectResponse(
            UUID id, UUID semesterId, String name, String teacher, short requiredAttendancePct,
            BigDecimal credits,
            boolean quizOn, BigDecimal quizMarks,
            boolean tutorialOn, BigDecimal tutorialMarks,
            BigDecimal midMarks, BigDecimal endMarks,
            BigDecimal assignmentMarks, BigDecimal attendanceMarks,
            BigDecimal totalMarks, String grade, Integer gradePoints,
            boolean countsTowardSgpa,
            Double attendancePct, long present, long absent, long cancelled
    ) {}
}
