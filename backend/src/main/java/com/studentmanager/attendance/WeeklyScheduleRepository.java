package com.studentmanager.attendance;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WeeklyScheduleRepository extends JpaRepository<WeeklySchedule, UUID> {
    List<WeeklySchedule> findBySubjectId(UUID subjectId);
    List<WeeklySchedule> findByUserIdAndDayOfWeek(UUID userId, short dayOfWeek);
    Optional<WeeklySchedule> findByIdAndUserId(UUID id, UUID userId);
}
