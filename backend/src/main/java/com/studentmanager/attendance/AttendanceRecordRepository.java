package com.studentmanager.attendance;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, UUID> {
    List<AttendanceRecord> findBySubjectId(UUID subjectId);
    Optional<AttendanceRecord> findByScheduleIdAndClassDate(UUID scheduleId, LocalDate classDate);
    long countBySubjectIdAndStatus(UUID subjectId, AttendanceRecord.Status status);
}
