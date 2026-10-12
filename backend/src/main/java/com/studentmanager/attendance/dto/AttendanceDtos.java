package com.studentmanager.attendance.dto;

import com.studentmanager.attendance.AttendanceRecord;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public final class AttendanceDtos {
    private AttendanceDtos() {}

    public record MarkRequest(
            @NotNull UUID scheduleId,
            @NotNull LocalDate classDate,
            @NotNull AttendanceRecord.Status status,
            @Size(max = 120) String reason
    ) {}

    public record AttendanceResponse(
            UUID id, UUID subjectId, UUID scheduleId, LocalDate classDate,
            AttendanceRecord.Status status, String reason
    ) {}

    /** How many classes in a row the student needs to attend to reach the
     * subject's required percentage, so the UI can say "attend the next 4
     * classes" without re-deriving the formula on every client. */
    public record SubjectAttendanceSummary(
            long present, long absent, long cancelled,
            Double percentage, short requiredPct,
            Integer classesNeededToReachRequired
    ) {}
}
