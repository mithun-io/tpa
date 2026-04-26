package com.tpa.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Builder
@Table(name = "carriers")
@AllArgsConstructor
@NoArgsConstructor
public class Carrier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false)
    private String companyName;

    @Column(nullable = false, unique = true)
    private String registrationNumber;

    @Column(nullable = false)
    private String companyType;

    @Column(nullable = false)
    private String licenseNumber;

    @Column(nullable = false)
    private String taxId;

    @Column(nullable = false)
    private String contactPersonName;

    @Column(nullable = false)
    private String contactPersonPhone;

    private String website;

    // AI pre-validation result (populated after OTP verification)
    private Double aiRiskScore;
    private String aiRiskStatus;      // LOW_RISK, MEDIUM_RISK, HIGH_RISK
    private String aiRecommendation;  // SAFE_TO_APPROVE, MANUAL_REVIEW, REJECT
}
