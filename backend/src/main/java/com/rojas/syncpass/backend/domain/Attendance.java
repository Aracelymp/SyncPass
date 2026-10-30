package com.rojas.syncpass.backend.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "attendances", indexes = {
        @Index(name = "idx_att_event", columnList = "event_id"),
        @Index(name = "idx_att_user", columnList = "user_id"),
        @Index(name = "idx_att_client", columnList = "client_id", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Attendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, length = 180)
    private String participantName;

    @Column(length = 160)
    private String participantEmail;

    @Column(length = 60)
    private String participantDocument;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AttendanceMethod method;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "registered_by")
    private User registeredBy;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "client_id", nullable = false, unique = true, length = 80)
    private String clientId;

    @Column(nullable = false)
    private Instant registeredAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}
