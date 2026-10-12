package com.studentmanager.academic;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AssignmentRepository extends JpaRepository<Assignment, UUID> {
    List<Assignment> findBySubjectIdOrderByDueDateAsc(UUID subjectId);
    Optional<Assignment> findByIdAndUserId(UUID id, UUID userId);
    long countBySubjectId(UUID subjectId);
    long countBySubjectIdAndDoneTrue(UUID subjectId);
}
