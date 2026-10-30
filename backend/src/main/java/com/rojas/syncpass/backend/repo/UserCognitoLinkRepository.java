package com.rojas.syncpass.backend.repo;

import com.rojas.syncpass.backend.domain.UserCognitoLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserCognitoLinkRepository extends JpaRepository<UserCognitoLink, Long> {
    Optional<UserCognitoLink> findByCognitoSub(String cognitoSub);

    Optional<UserCognitoLink> findByUser_Id(Long userId);

    boolean existsByCognitoSub(String cognitoSub);

    boolean existsByUser_Id(Long userId);
}
