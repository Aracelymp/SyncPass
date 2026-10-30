package com.rojas.syncpass.backend.repo;

import com.rojas.syncpass.backend.domain.SurveyResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SurveyRepository extends JpaRepository<SurveyResponse, Long> {
    List<SurveyResponse> findByEventIdOrderBySubmittedAtDesc(Long eventId);

    long countByEventId(Long eventId);

    List<SurveyResponse> findByUserIdOrderBySubmittedAtDesc(Long userId);

    Optional<SurveyResponse> findByClientId(String clientId);

    Optional<SurveyResponse> findByEventIdAndUserId(Long eventId, Long userId);

    Optional<SurveyResponse> findByAttendanceId(Long attendanceId);

    @Query("""
            select s from SurveyResponse s
            where s.user.id = :userId or s.collectedBy.id = :userId
            order by s.submittedAt desc
            """)
    List<SurveyResponse> findForUserOrCollector(@Param("userId") Long userId);

    @Query("select avg(s.rating) from SurveyResponse s where s.event.id = :eventId")
    Double averageRatingByEvent(@Param("eventId") Long eventId);

    @Query("select avg(s.organizationRating) from SurveyResponse s where s.event.id = :eventId")
    Double averageOrganizationByEvent(@Param("eventId") Long eventId);

    @Query("select avg(s.impactRating) from SurveyResponse s where s.event.id = :eventId")
    Double averageImpactByEvent(@Param("eventId") Long eventId);

    @Query("select avg(s.rating) from SurveyResponse s")
    Double averageRating();
}
