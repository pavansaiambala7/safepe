package com.safepe.layer3.repository;

import com.safepe.layer3.entity.EncryptedUPI;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UPIVaultRepository extends JpaRepository<EncryptedUPI, UUID> {
    Optional<EncryptedUPI> findByUpiHash(String upiHash);
    List<EncryptedUPI> findByUserId(String userId);
}
