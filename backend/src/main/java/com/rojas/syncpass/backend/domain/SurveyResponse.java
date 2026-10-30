package com.rojas.syncpass.backend.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "survey_responses", indexes = {
        @Index(name = "idx_survey_event", columnList = "event_id"),
        @Index(name = "idx_survey_user", columnList = "user_id"),
        @Index(name = "idx_survey_client", columnList = "client_id", unique = true),
        @Index(name = "idx_survey_attendance", columnList = "attendance_id", unique = true),
        @Index(name = "idx_survey_collector", columnList = "collected_by")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SurveyResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "attendance_id")
    private Attendance attendance;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "collected_by")
    private User collectedBy;

    @Column(nullable = false)
    private int rating;

    @Column(nullable = false)
    private int organizationRating;

    @Column(nullable = false)
    private int impactRating;

    @Column(length = 1000)
    private String comment;

    @Column(nullable = false)
    private boolean wouldRecommend;

    @Column(name = "client_id", nullable = false, unique = true, length = 80)
    private String clientId;

    @Column(nullable = false)
    private Instant submittedAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}
