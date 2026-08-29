package com.safepe.payment.repository;

import com.safepe.payment.model.ScheduledBill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ScheduledBillRepository extends JpaRepository<ScheduledBill, UUID> {
    List<ScheduledBill> findByUserId(String userId);
    List<ScheduledBill> findByStatus(String status);
}
