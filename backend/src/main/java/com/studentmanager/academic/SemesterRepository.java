package com.studentmanager.academic;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SemesterRepository extends JpaRepository<Semester, UUID> {
    List<Semester> findByUserIdOrderByNumberAsc(UUID userId);
    Optional<Semester> findByIdAndUserId(UUID id, UUID userId);
    Optional<Semester> findByUserIdAndNumber(UUID userId, short number);
}
