package com.studentmanager.academic;

import com.studentmanager.academic.dto.SemesterDtos.SemesterRequest;
import com.studentmanager.academic.dto.SemesterDtos.SemesterResponse;
import com.studentmanager.common.exception.ApiExceptions.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class SemesterService {

    private final SemesterRepository repo;

    public SemesterService(SemesterRepository repo) { this.repo = repo; }

    @Transactional
    public SemesterResponse create(UUID userId, SemesterRequest req) {
        // Upsert by (userId, number): re-saving the same semester number
        // edits its dates rather than creating a duplicate row.
        Semester s = repo.findByUserIdAndNumber(userId, req.number()).orElseGet(Semester::new);
        s.setUserId(userId);
        s.setNumber(req.number());
        s.setStartDate(req.startDate());
        s.setEndDate(req.endDate());
        return toResponse(repo.save(s));
    }

    @Transactional(readOnly = true)
    public List<SemesterResponse> list(UUID userId) {
        return repo.findByUserIdOrderByNumberAsc(userId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public Semester getOwned(UUID userId, UUID id) {
        return repo.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Semester not found."));
    }

    private SemesterResponse toResponse(Semester s) {
        return new SemesterResponse(s.getId(), s.getNumber(), s.getStartDate(), s.getEndDate());
    }
}
