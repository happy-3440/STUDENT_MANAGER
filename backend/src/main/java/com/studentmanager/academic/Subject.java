package com.studentmanager.academic;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "subjects")
public class Subject {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "semester_id", nullable = false)
    private UUID semesterId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 100)
    private String teacher;

    @Column(name = "required_attendance_pct", nullable = false)
    private short requiredAttendancePct = 75;

    private BigDecimal credits;

    @Column(name = "quiz_on", nullable = false)
    private boolean quizOn;

    @Column(name = "quiz_marks")
    private BigDecimal quizMarks;

    @Column(name = "tutorial_on", nullable = false)
    private boolean tutorialOn;

    @Column(name = "tutorial_marks")
    private BigDecimal tutorialMarks;

    @Column(name = "mid_marks")
    private BigDecimal midMarks;

    @Column(name = "end_marks")
    private BigDecimal endMarks;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() { Instant now = Instant.now(); createdAt = now; updatedAt = now; }

    @PreUpdate
    void onUpdate() { updatedAt = Instant.now(); }

    /** True once every mark this subject currently requires has been entered. */
    public boolean hasCompleteMarks() {
        boolean quizOk = !quizOn || quizMarks != null;
        boolean tutorialOk = !tutorialOn || tutorialMarks != null;
        return quizOk && tutorialOk && midMarks != null && endMarks != null;
    }

    // --- getters / setters ---

    public UUID getId() { return id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public UUID getSemesterId() { return semesterId; }
    public void setSemesterId(UUID semesterId) { this.semesterId = semesterId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getTeacher() { return teacher; }
    public void setTeacher(String teacher) { this.teacher = teacher; }

    public short getRequiredAttendancePct() { return requiredAttendancePct; }
    public void setRequiredAttendancePct(short v) { this.requiredAttendancePct = v; }

    public BigDecimal getCredits() { return credits; }
    public void setCredits(BigDecimal credits) { this.credits = credits; }

    public boolean isQuizOn() { return quizOn; }
    public void setQuizOn(boolean quizOn) { this.quizOn = quizOn; }

    public BigDecimal getQuizMarks() { return quizMarks; }
    public void setQuizMarks(BigDecimal quizMarks) { this.quizMarks = quizMarks; }

    public boolean isTutorialOn() { return tutorialOn; }
    public void setTutorialOn(boolean tutorialOn) { this.tutorialOn = tutorialOn; }

    public BigDecimal getTutorialMarks() { return tutorialMarks; }
    public void setTutorialMarks(BigDecimal tutorialMarks) { this.tutorialMarks = tutorialMarks; }

    public BigDecimal getMidMarks() { return midMarks; }
    public void setMidMarks(BigDecimal midMarks) { this.midMarks = midMarks; }

    public BigDecimal getEndMarks() { return endMarks; }
    public void setEndMarks(BigDecimal endMarks) { this.endMarks = endMarks; }
}
