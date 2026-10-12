package com.studentmanager.academic.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public final class AssignmentDtos {
    private AssignmentDtos() {}

    public record AssignmentRequest(
            @NotBlank @Size(max = 60) String name,
            LocalDate dueDate
    ) {}

    public record AssignmentUpdateRequest(
            @Size(max = 60) String name,
            LocalDate dueDate,
            Boolean done
    ) {}

    public record AssignmentResponse(UUID id, UUID subjectId, String name, LocalDate dueDate, boolean done) {}
}
