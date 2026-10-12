package com.studentmanager.academic.dto;

import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.util.UUID;

public final class SemesterDtos {
    private SemesterDtos() {}

    public record SemesterRequest(
            @NotNull @Min(1) @Max(12) Short number,
            @NotNull LocalDate startDate,
            @NotNull LocalDate endDate
    ) {}

    public record SemesterResponse(UUID id, short number, LocalDate startDate, LocalDate endDate) {}
}
