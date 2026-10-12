package com.studentmanager.academic;

import com.studentmanager.academic.dto.SubjectDtos.MarksRequest;
import com.studentmanager.academic.dto.SubjectDtos.SubjectRequest;
import com.studentmanager.academic.dto.SubjectDtos.SubjectResponse;
import com.studentmanager.attendance.AttendanceRecord;
import com.studentmanager.attendance.AttendanceRecordRepository;
import com.studentmanager.common.exception.ApiExceptions.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class SubjectService {

    private final SubjectRepository subjects;
    private final AssignmentRepository assignments;
    private final AttendanceRecordRepository attendance;

    public SubjectService(SubjectRepository subjects, AssignmentRepository assignments, AttendanceRecordRepository attendance) {
        this.subjects = subjects;
        this.assignments = assignments;
        this.attendance = attendance;
    }

    @Transactional
    public SubjectResponse create(UUID userId, UUID semesterId, SubjectRequest req) {
        Subject s = new Subject();
        s.setUserId(userId);
        s.setSemesterId(semesterId);
        s.setName(req.name());
        s.setTeacher(req.teacher());
        if (req.requiredAttendancePct() != null) s.setRequiredAttendancePct(req.requiredAttendancePct());
        return toResponse(subjects.save(s));
    }

    @Transactional(readOnly = true)
    public List<SubjectResponse> listBySemester(UUID userId, UUID semesterId) {
        return subjects.findBySemesterIdAndUserId(semesterId, userId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public SubjectResponse updateBasics(UUID userId, UUID subjectId, SubjectRequest req) {
        Subject s = getOwned(userId, subjectId);
        s.setName(req.name());
        s.setTeacher(req.teacher());
        if (req.requiredAttendancePct() != null) s.setRequiredAttendancePct(req.requiredAttendancePct());
        return toResponse(subjects.save(s));
    }

    @Transactional
    public SubjectResponse updateMarks(UUID userId, UUID subjectId, MarksRequest req) {
        Subject s = getOwned(userId, subjectId);
        s.setCredits(req.credits());
        if (req.quizOn() != null) s.setQuizOn(req.quizOn());
        s.setQuizMarks(req.quizMarks());
        if (req.tutorialOn() != null) s.setTutorialOn(req.tutorialOn());
        s.setTutorialMarks(req.tutorialMarks());
        s.setMidMarks(req.midMarks());
        s.setEndMarks(req.endMarks());
        return toResponse(subjects.save(s));
    }

    @Transactional
    public void delete(UUID userId, UUID subjectId) {
        Subject s = getOwned(userId, subjectId);
        subjects.delete(s); // cascades to schedule, attendance_records, assignments
    }

    @Transactional(readOnly = true)
    public Subject getOwned(UUID userId, UUID id) {
        return subjects.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found."));
    }

    public SubjectResponse toResponse(Subject s) {
        long present = attendance.countBySubjectIdAndStatus(s.getId(), AttendanceRecord.Status.PRESENT);
        long absent = attendance.countBySubjectIdAndStatus(s.getId(), AttendanceRecord.Status.ABSENT);
        long cancelled = attendance.countBySubjectIdAndStatus(s.getId(), AttendanceRecord.Status.CANCELLED);
        long marked = present + absent;
        Double pct = marked == 0 ? null : (present * 100.0 / marked);

        long totalAssignments = assignments.countBySubjectId(s.getId());
        long doneAssignments = assignments.countBySubjectIdAndDoneTrue(s.getId());
        BigDecimal assignmentMarks = GradeCalculator.assignmentMarks(totalAssignments, doneAssignments);
        BigDecimal attendanceMarks = GradeCalculator.attendanceMarks(pct, s.getRequiredAttendancePct());

        boolean counts = s.hasCompleteMarks() && s.getCredits() != null;
        BigDecimal total = null;
        String grade = null;
        Integer points = null;
        if (s.hasCompleteMarks()) {
            total = GradeCalculator.totalMarks(s, assignmentMarks, attendanceMarks);
            var g = GradeCalculator.gradeOf(total);
            grade = g.letter();
            points = g.points();
        }

        return new SubjectResponse(
                s.getId(), s.getSemesterId(), s.getName(), s.getTeacher(), s.getRequiredAttendancePct(),
                s.getCredits(),
                s.isQuizOn(), s.getQuizMarks(),
                s.isTutorialOn(), s.getTutorialMarks(),
                s.getMidMarks(), s.getEndMarks(),
                assignmentMarks, attendanceMarks,
                total, grade, points, counts,
                pct, present, absent, cancelled
        );
    }
}
