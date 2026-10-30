package com.rojas.syncpass.backend.repo;

import com.rojas.syncpass.backend.domain.Enrollment;
import com.rojas.syncpass.backend.domain.EnrollmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    List<Enrollment> findByEventIdOrderByCreatedAtAsc(Long eventId);

    List<Enrollment> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<Enrollment> findByEventIdAndUserId(Long eventId, Long userId);

    long countByEventId(Long eventId);

    long countByEventIdAndStatus(Long eventId, EnrollmentStatus status);

    long countByUserIdAndStatus(Long userId, EnrollmentStatus status);
}
