package com.safepe.layer3.repository;

import com.safepe.layer3.entity.EncryptedCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CardVaultRepository extends JpaRepository<EncryptedCard, UUID> {
    Optional<EncryptedCard> findByCardHash(String cardHash);
    List<EncryptedCard> findByUserId(String userId);
}
