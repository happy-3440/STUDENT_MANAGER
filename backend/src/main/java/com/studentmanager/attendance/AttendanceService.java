package com.studentmanager.attendance;

import com.studentmanager.academic.SubjectService;
import com.studentmanager.attendance.dto.AttendanceDtos.AttendanceResponse;
import com.studentmanager.attendance.dto.AttendanceDtos.MarkRequest;
import com.studentmanager.attendance.dto.AttendanceDtos.SubjectAttendanceSummary;
import com.studentmanager.common.exception.ApiExceptions.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AttendanceService {

    private final AttendanceRecordRepository records;
    private final WeeklyScheduleRepository schedules;
    private final SubjectService subjectService;

    public AttendanceService(AttendanceRecordRepository records, WeeklyScheduleRepository schedules, SubjectService subjectService) {
        this.records = records;
        this.schedules = schedules;
        this.subjectService = subjectService;
    }

    @Transactional
    public AttendanceResponse mark(UUID userId, MarkRequest req) {
        WeeklySchedule schedule = schedules.findByIdAndUserId(req.scheduleId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Scheduled class not found."));

        // Upsert: marking the same class/date again changes your mind
        // (Present -> Absent), it never creates a second record - the
        // unique constraint on (schedule_id, class_date) is the backstop.
        AttendanceRecord rec = records.findByScheduleIdAndClassDate(schedule.getId(), req.classDate())
                .orElseGet(AttendanceRecord::new);
        rec.setUserId(userId);
        rec.setSubjectId(schedule.getSubjectId());
        rec.setScheduleId(schedule.getId());
        rec.setClassDate(req.classDate());
        rec.setStatus(req.status());
        rec.setReason(req.status() == AttendanceRecord.Status.CANCELLED ? req.reason() : null);

        rec = records.save(rec);
        return new AttendanceResponse(rec.getId(), rec.getSubjectId(), rec.getScheduleId(), rec.getClassDate(), rec.getStatus(), rec.getReason());
    }

    @Transactional
    public void unmark(UUID userId, UUID scheduleId, java.time.LocalDate classDate) {
        schedules.findByIdAndUserId(scheduleId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Scheduled class not found."));
        records.findByScheduleIdAndClassDate(scheduleId, classDate).ifPresent(records::delete);
    }

    @Transactional(readOnly = true)
    public SubjectAttendanceSummary summaryForSubject(UUID userId, UUID subjectId) {
        var subject = subjectService.getOwned(userId, subjectId);
        long present = records.countBySubjectIdAndStatus(subjectId, AttendanceRecord.Status.PRESENT);
        long absent = records.countBySubjectIdAndStatus(subjectId, AttendanceRecord.Status.ABSENT);
        long cancelled = records.countBySubjectIdAndStatus(subjectId, AttendanceRecord.Status.CANCELLED);
        long total = present + absent;

        Double pct = total == 0 ? null : (present * 100.0 / total);
        short required = subject.getRequiredAttendancePct();
        Integer classesNeeded = null;

        if (pct != null && pct < required && required < 100) {
            // need = ceil((required*total - 100*present) / (100-required))
            double raw = (required * (double) total - 100.0 * present) / (100.0 - required);
            classesNeeded = (int) Math.ceil(raw);
        }

        return new SubjectAttendanceSummary(present, absent, cancelled, pct, required, classesNeeded);
    }
}
