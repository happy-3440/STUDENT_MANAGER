package com.studentmanager.academic;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * A subject's 100 marks: quiz/tutorial (10, shared if both are on) +
 * assignments (10, split evenly across however many are added) +
 * attendance (5, awarded once the subject's own required percentage is
 * reached) + mid semester (15) + end semester (60).
 *
 * This used to be duplicated in the prototype's JavaScript. It now lives
 * here ONCE, so the backend's number is always the real number, and a
 * client (web, Flutter, anything) never has to re-derive it.
 */
public final class GradeCalculator {
    private GradeCalculator() {}

    public record Grade(String letter, int points) {}

    public static Grade gradeOf(BigDecimal totalOutOf100) {
        double m = totalOutOf100.doubleValue();
        if (m >= 90) return new Grade("O", 10);
        if (m >= 80) return new Grade("A+", 9);
        if (m >= 70) return new Grade("A", 8);
        if (m >= 60) return new Grade("B+", 7);
        if (m >= 55) return new Grade("B", 6);
        if (m >= 50) return new Grade("C", 5);
        if (m >= 40) return new Grade("P", 4);
        return new Grade("F", 0);
    }

    public static BigDecimal quizTutorialMarks(Subject s) {
        BigDecimal q = (s.isQuizOn() && s.getQuizMarks() != null) ? s.getQuizMarks() : BigDecimal.ZERO;
        BigDecimal t = (s.isTutorialOn() && s.getTutorialMarks() != null) ? s.getTutorialMarks() : BigDecimal.ZERO;
        return q.add(t);
    }

    public static BigDecimal assignmentMarks(long totalAssignments, long doneAssignments) {
        if (totalAssignments == 0) return BigDecimal.ZERO;
        return BigDecimal.TEN
                .multiply(BigDecimal.valueOf(doneAssignments))
                .divide(BigDecimal.valueOf(totalAssignments), 2, RoundingMode.HALF_UP);
    }

    public static BigDecimal attendanceMarks(Double attendancePct, short requiredPct) {
        if (attendancePct == null || attendancePct < requiredPct) return BigDecimal.ZERO;
        return BigDecimal.valueOf(5);
    }

    public static BigDecimal totalMarks(Subject s, BigDecimal assignmentMarks, BigDecimal attendanceMarks) {
        BigDecimal mid = s.getMidMarks() != null ? s.getMidMarks() : BigDecimal.ZERO;
        BigDecimal end = s.getEndMarks() != null ? s.getEndMarks() : BigDecimal.ZERO;
        return quizTutorialMarks(s).add(assignmentMarks).add(attendanceMarks).add(mid).add(end);
    }
}
