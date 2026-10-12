package com.studentmanager.academic;

import com.studentmanager.academic.dto.AssignmentDtos.AssignmentRequest;
import com.studentmanager.academic.dto.AssignmentDtos.AssignmentResponse;
import com.studentmanager.academic.dto.AssignmentDtos.AssignmentUpdateRequest;
import com.studentmanager.common.exception.ApiExceptions.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AssignmentService {

    private final AssignmentRepository repo;
    private final SubjectService subjectService; // for ownership checks on the parent subject

    public AssignmentService(AssignmentRepository repo, SubjectService subjectService) {
        this.repo = repo;
        this.subjectService = subjectService;
    }

    @Transactional
    public AssignmentResponse create(UUID userId, UUID subjectId, AssignmentRequest req) {
        subjectService.getOwned(userId, subjectId); // 404s if the subject isn't theirs
        Assignment a = new Assignment();
        a.setUserId(userId);
        a.setSubjectId(subjectId);
        a.setName(req.name());
        a.setDueDate(req.dueDate());
        return toResponse(repo.save(a));
    }

    @Transactional(readOnly = true)
    public List<AssignmentResponse> listBySubject(UUID userId, UUID subjectId) {
        subjectService.getOwned(userId, subjectId);
        return repo.findBySubjectIdOrderByDueDateAsc(subjectId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public AssignmentResponse update(UUID userId, UUID id, AssignmentUpdateRequest req) {
        Assignment a = repo.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found."));
        if (req.name() != null) a.setName(req.name());
        if (req.dueDate() != null) a.setDueDate(req.dueDate());
        if (req.done() != null) a.setDone(req.done());
        return toResponse(repo.save(a));
    }

    @Transactional
    public void delete(UUID userId, UUID id) {
        Assignment a = repo.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found."));
        repo.delete(a);
    }

    private AssignmentResponse toResponse(Assignment a) {
        return new AssignmentResponse(a.getId(), a.getSubjectId(), a.getName(), a.getDueDate(), a.isDone());
    }
}
