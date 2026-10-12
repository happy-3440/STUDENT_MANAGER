package com.studentmanager.academic;

import com.studentmanager.academic.dto.AcademicDashboardResponse;
import com.studentmanager.academic.dto.SubjectDtos.SubjectResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Service
public class AcademicDashboardService {

    private final SubjectRepository subjectRepo;
    private final SubjectService subjectService;

    public AcademicDashboardService(SubjectRepository subjectRepo, SubjectService subjectService) {
        this.subjectRepo = subjectRepo;
        this.subjectService = subjectService;
    }

    @Transactional(readOnly = true)
    public AcademicDashboardResponse forSemester(UUID userId, UUID semesterId) {
        var subjects = subjectRepo.findBySemesterIdAndUserId(semesterId, userId).stream()
                .map(subjectService::toResponse).toList();

        BigDecimal creditsCounted = BigDecimal.ZERO;
        BigDecimal creditsTotal = BigDecimal.ZERO;
        BigDecimal weightedPoints = BigDecimal.ZERO;

        for (SubjectResponse s : subjects) {
            if (s.credits() != null) creditsTotal = creditsTotal.add(s.credits());
            if (s.countsTowardSgpa()) {
                creditsCounted = creditsCounted.add(s.credits());
                weightedPoints = weightedPoints.add(s.credits().multiply(BigDecimal.valueOf(s.gradePoints())));
            }
        }

        BigDecimal sgpa = creditsCounted.signum() > 0
                ? weightedPoints.divide(creditsCounted, 2, RoundingMode.HALF_UP)
                : null;

        return new AcademicDashboardResponse(sgpa, creditsCounted, creditsTotal, subjects);
    }
}
