package com.rojas.syncpass.backend.repo;

import com.rojas.syncpass.backend.domain.CognitoLinkRequest;
import com.rojas.syncpass.backend.domain.CognitoLinkRequest.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CognitoLinkRequestRepository extends JpaRepository<CognitoLinkRequest, Long> {

    Optional<CognitoLinkRequest> findByCognitoSub(String cognitoSub);

    List<CognitoLinkRequest> findByStatusOrderByCreatedAtDesc(RequestStatus status);

    boolean existsByCognitoSub(String cognitoSub);
}
