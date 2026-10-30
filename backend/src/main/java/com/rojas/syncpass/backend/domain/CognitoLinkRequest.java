package com.rojas.syncpass.backend.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/**
 * Pending access request from an authenticated Cognito identity that is not
 * yet linked to a SyncPass user. Approval is explicit and coordinator-driven.
 */
@Entity
@Table(name = "cognito_link_requests", indexes = {
        @Index(name = "idx_clr_sub", columnList = "cognito_sub", unique = true),
        @Index(name = "idx_clr_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CognitoLinkRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cognito_sub", nullable = false, unique = true, length = 128)
    private String cognitoSub;

    @Column(nullable = false, length = 160)
    private String email;

    @Column(name = "full_name", nullable = false, length = 160)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private RequestStatus status = RequestStatus.PENDING;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Column(name = "decided_by", length = 80)
    private String decidedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "linked_user_id")
    private User linkedUser;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    public enum RequestStatus {
        PENDING,
        APPROVED,
        REJECTED
    }
}
