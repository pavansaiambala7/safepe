package com.safepe.payment.repository;

import com.safepe.payment.model.TokenizedUPI;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UPITokenRepository extends JpaRepository<TokenizedUPI, UUID> {
    Optional<TokenizedUPI> findByRazorpayTokenId(String razorpayTokenId);
    List<TokenizedUPI> findByUserId(String userId);
}
