package com.rojas.syncpass.backend.repo;

import com.rojas.syncpass.backend.domain.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    List<Attendance> findByEventIdOrderByRegisteredAtDesc(Long eventId);

    List<Attendance> findByUserIdOrderByRegisteredAtDesc(Long userId);

    List<Attendance> findByRegisteredByIdOrderByRegisteredAtDesc(Long registeredById);

    List<Attendance> findByEventIdAndRegisteredByIdOrderByRegisteredAtDesc(Long eventId, Long registeredById);

    Optional<Attendance> findByClientId(String clientId);

    Optional<Attendance> findByEventIdAndUserId(Long eventId, Long userId);

    @Query("""
            select a from Attendance a
            where a.user.id = :userId or a.registeredBy.id = :userId
            order by a.registeredAt desc
            """)
    List<Attendance> findForMonitor(@Param("userId") Long userId);

    long countByEventId(Long eventId);

    long countByUserId(Long userId);
}
