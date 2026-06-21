package com.safepe.layer3.repository;

import com.safepe.layer3.entity.EncryptedAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountVaultRepository extends JpaRepository<EncryptedAccount, UUID> {
    Optional<EncryptedAccount> findByAccountHash(String accountHash);
    List<EncryptedAccount> findByUserId(String userId);
}
