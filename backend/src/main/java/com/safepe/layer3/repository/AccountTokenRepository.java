package com.safepe.layer3.repository;

import com.safepe.layer3.entity.TokenizedAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountTokenRepository extends JpaRepository<TokenizedAccount, UUID> {
    Optional<TokenizedAccount> findByRazorpayTokenId(String razorpayTokenId);
    List<TokenizedAccount> findByUserId(String userId);
}
