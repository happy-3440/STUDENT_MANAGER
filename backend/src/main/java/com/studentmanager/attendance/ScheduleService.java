package com.studentmanager.attendance;

import com.studentmanager.academic.SubjectService;
import com.studentmanager.attendance.dto.ScheduleDtos.ScheduleRequest;
import com.studentmanager.attendance.dto.ScheduleDtos.ScheduleResponse;
import com.studentmanager.common.exception.ApiExceptions.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ScheduleService {

    private final WeeklyScheduleRepository repo;
    private final SubjectService subjectService;

    public ScheduleService(WeeklyScheduleRepository repo, SubjectService subjectService) {
        this.repo = repo;
        this.subjectService = subjectService;
    }

    @Transactional
    public ScheduleResponse create(UUID userId, UUID subjectId, ScheduleRequest req) {
        subjectService.getOwned(userId, subjectId); // 404s if not theirs
        WeeklySchedule w = new WeeklySchedule();
        w.setUserId(userId);
        w.setSubjectId(subjectId);
        w.setDayOfWeek(req.dayOfWeek());
        w.setStartMinutes(req.startMinutes());
        w.setEndMinutes(req.endMinutes());
        // No manual overlap check here - the database's EXCLUDE constraint
        // (ex_schedule_no_overlap) is the real guarantee, and throws a
        // DataIntegrityViolationException the GlobalExceptionHandler turns
        // into a clean 409.
        return toResponse(repo.save(w));
    }

    @Transactional(readOnly = true)
    public List<ScheduleResponse> listForSubject(UUID userId, UUID subjectId) {
        subjectService.getOwned(userId, subjectId);
        return repo.findBySubjectId(subjectId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ScheduleResponse> listForDay(UUID userId, short dayOfWeek) {
        return repo.findByUserIdAndDayOfWeek(userId, dayOfWeek).stream().map(this::toResponse).toList();
    }

    @Transactional
    public void delete(UUID userId, UUID id) {
        WeeklySchedule w = repo.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Scheduled class not found."));
        repo.delete(w); // cascades to its attendance_records
    }

    private ScheduleResponse toResponse(WeeklySchedule w) {
        return new ScheduleResponse(w.getId(), w.getSubjectId(), w.getDayOfWeek(), w.getStartMinutes(), w.getEndMinutes());
    }
}
