package com.safepe.layer3.repository;

import com.safepe.layer3.entity.TokenizedCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CardTokenRepository extends JpaRepository<TokenizedCard, UUID> {
    Optional<TokenizedCard> findByRazorpayTokenId(String razorpayTokenId);
    List<TokenizedCard> findByUserId(String userId);
}
