package com.studentmanager.academic;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubjectRepository extends JpaRepository<Subject, UUID> {
    List<Subject> findBySemesterIdAndUserId(UUID semesterId, UUID userId);
    Optional<Subject> findByIdAndUserId(UUID id, UUID userId);
}
