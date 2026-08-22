package com.safepe.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "split_participants")
public class SplitParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "split_bill_id")
    @ToString.Exclude
    private SplitBill splitBill;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "share_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal shareAmount;

    @Column
    @Builder.Default
    private Boolean paid = false;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;
}
