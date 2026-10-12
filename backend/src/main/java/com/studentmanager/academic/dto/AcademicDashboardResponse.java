package com.studentmanager.academic.dto;

import java.math.BigDecimal;
import java.util.List;

public record AcademicDashboardResponse(
        BigDecimal sgpa,          // null if no subject has complete marks + credits yet
        BigDecimal creditsCounted,
        BigDecimal creditsTotal,
        List<SubjectDtos.SubjectResponse> subjects
) {}
