package com.safepe.repository;

import com.safepe.model.Merchant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MerchantRepository extends JpaRepository<Merchant, UUID> {
    Optional<Merchant> findByNameIgnoreCase(String name);
    Optional<Merchant> findByUpiIdMasked(String upiIdMasked);
    List<Merchant> findByIsFlaggedTrue();
    List<Merchant> findByTrustScoreLessThan(Double score);
    List<Merchant> findByCategory(String category);

    @Query("SELECT m FROM Merchant m WHERE LOWER(m.name) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Merchant> searchByName(@Param("query") String query);
}
