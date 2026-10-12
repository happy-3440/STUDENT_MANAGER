package com.studentmanager.attendance.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public final class ScheduleDtos {
    private ScheduleDtos() {}

    public record ScheduleRequest(
            @NotNull @Min(0) @Max(6) Short dayOfWeek,
            @NotNull @Min(0) @Max(1439) Short startMinutes,
            @NotNull @Min(1) @Max(1440) Short endMinutes
    ) {}

    public record ScheduleResponse(UUID id, UUID subjectId, short dayOfWeek, short startMinutes, short endMinutes) {}
}
