package com.rojas.syncpass.backend.repo;

import com.rojas.syncpass.backend.domain.Event;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {
    List<Event> findAllByOrderByStartsAtAsc();

    List<Event> findByProjectIdOrderByStartsAtAsc(Long projectId);

    Optional<Event> findByQrToken(String qrToken);

    boolean existsByQrToken(String qrToken);

    long countByProjectId(Long projectId);
}
