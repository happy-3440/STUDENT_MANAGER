package com.studentmanager.attendance;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "weekly_schedule")
public class WeeklySchedule {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "subject_id", nullable = false)
    private UUID subjectId;

    /** 0 = Monday ... 6 = Sunday. */
    @Column(name = "day_of_week", nullable = false)
    private short dayOfWeek;

    @Column(name = "start_minutes", nullable = false)
    private short startMinutes;

    @Column(name = "end_minutes", nullable = false)
    private short endMinutes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() { createdAt = Instant.now(); }

    // --- getters / setters ---

    public UUID getId() { return id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public UUID getSubjectId() { return subjectId; }
    public void setSubjectId(UUID subjectId) { this.subjectId = subjectId; }

    public short getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(short dayOfWeek) { this.dayOfWeek = dayOfWeek; }

    public short getStartMinutes() { return startMinutes; }
    public void setStartMinutes(short startMinutes) { this.startMinutes = startMinutes; }

    public short getEndMinutes() { return endMinutes; }
    public void setEndMinutes(short endMinutes) { this.endMinutes = endMinutes; }
}
