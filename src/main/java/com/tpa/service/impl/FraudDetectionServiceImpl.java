package com.tpa.service.impl;

import com.tpa.dto.response.FraudClaimDto;
import com.tpa.dto.response.FraudDashboardResponse;
import com.tpa.entity.Carrier;
import com.tpa.entity.Claim;
import com.tpa.entity.User;
import com.tpa.repository.CarrierRepository;
import com.tpa.repository.ClaimRepository;
import com.tpa.repository.UserRepository;
import com.tpa.service.FraudDetectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FraudDetectionServiceImpl implements FraudDetectionService {

    private final ClaimRepository claimRepository;
    private final CarrierRepository carrierRepository;
    private final UserRepository userRepository;

    @Override
    public void calculateAndSaveHealthAndRisk(Claim claim) {
        if (claim == null) return;
        
        // If already marked as safe manually, don't override unless requested
        if ("LOW".equals(claim.getRiskLevel()) && claim.getHealthScore() != null && claim.getHealthScore() == 100) {
            return;
        }

        List<String> reasons = new ArrayList<>();
        double riskScore = 0.0;

        // 1. Amount anomaly
        if (claim.getAmount() != null) {
            if (claim.getAmount() > 50000) {
                riskScore += 30;
                reasons.add("High claim amount (> $50,000)");
            } else if (claim.getAmount() > 10000) {
                riskScore += 10;
                reasons.add("Elevated claim amount (> $10,000)");
            }
            if (claim.getTotalBillAmount() != null && !claim.getAmount().equals(claim.getTotalBillAmount())) {
                riskScore += 20;
                reasons.add("Claimed amount does not match total bill amount");
            }
        }

        // 2. Missing data
        if (claim.getPatientName() == null || claim.getPolicyNumber() == null) {
            riskScore += 15;
            reasons.add("Critical data missing from documents");
        }

        // 3. Dates anomaly
        if (claim.getAdmissionDate() != null && claim.getDischargeDate() != null) {
            if (claim.getDischargeDate().isBefore(claim.getAdmissionDate())) {
                riskScore += 40;
                reasons.add("Discharge date is before admission date");
            } else if (claim.getAdmissionDate().equals(claim.getDischargeDate())) {
                riskScore += 15;
                reasons.add("Zero-day hospital stay pattern detected");
            }
        }

        // 4. Inherit any existing risk from AI documents
        if (claim.getRiskScore() != null && claim.getRiskScore() > 0) {
            // Assume previous riskScore was mapped from AI validation score directly
            riskScore += claim.getRiskScore();
            if (claim.getRiskFlags() != null && !claim.getRiskFlags().isBlank()) {
                reasons.add("AI Document flag: " + claim.getRiskFlags());
            }
        }

        // Cap riskScore
        riskScore = Math.min(riskScore, 100.0);
        
        String riskLevel = "LOW";
        if (riskScore >= 70) {
            riskLevel = "HIGH";
        } else if (riskScore >= 30) {
            riskLevel = "MEDIUM";
        }
        
        int healthScore = 100 - (int) riskScore;

        claim.setRiskScore(riskScore);
        claim.setRiskLevel(riskLevel);
        claim.setRiskFlags(String.join(", ", reasons));
        claim.setHealthScore(healthScore);
        
        claimRepository.save(claim);
        log.info("Calculated HealthScore: {} for Claim ID: {}", healthScore, claim.getId());
    }

    @Override
    public FraudDashboardResponse getAdminFraudDashboard() {
        List<Claim> allClaims = claimRepository.findAll();
        if (allClaims == null) {
            allClaims = new ArrayList<>();
        }
        return generateDashboardResponse(allClaims);
    }

    @Override
    public FraudDashboardResponse getCarrierFraudDashboard(String carrierEmail) {
        User user = userRepository.findByEmail(carrierEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));
                
        Carrier carrier = carrierRepository.findByUser_Id(user.getId())
                .orElseThrow(() -> new RuntimeException("Carrier not found for user ID: " + user.getId()));
        
        List<Claim> carrierClaims = claimRepository.findByCarrier_Id(carrier.getId());
        if (carrierClaims == null) {
            carrierClaims = new ArrayList<>();
        }
        return generateDashboardResponse(carrierClaims);
    }

    @Override
    public void markClaimAsSafe(Long claimId) {
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new RuntimeException("Claim not found"));
        claim.setRiskScore(0.0);
        claim.setRiskLevel("LOW");
        claim.setHealthScore(100);
        claim.setRiskFlags("Marked safe by Admin");
        claimRepository.save(claim);
        log.info("Claim {} marked as safe", claimId);
    }

    private FraudDashboardResponse generateDashboardResponse(List<Claim> claims) {
        int total = claims.size();
        int highRisk = 0;
        int mediumRisk = 0;
        int lowRisk = 0;
        
        List<FraudClaimDto> fraudClaims = new ArrayList<>();

        for (Claim c : claims) {
            if (c.getHealthScore() == null) {
                calculateAndSaveHealthAndRisk(c);
            }
            
            String level = c.getRiskLevel() != null ? c.getRiskLevel() : "LOW";
            if ("HIGH".equals(level)) highRisk++;
            else if ("MEDIUM".equals(level)) mediumRisk++;
            else lowRisk++;
            
            fraudClaims.add(FraudClaimDto.builder()
                    .claimId(c.getId())
                    .policyNumber(c.getPolicyNumber())
                    .amount(c.getAmount())
                    .riskScore(c.getRiskScore())
                    .riskLevel(level)
                    .reasons(c.getRiskFlags() != null && !c.getRiskFlags().isBlank() ? List.of(c.getRiskFlags().split(", ")) : List.of())
                    .build());
        }

        FraudDashboardResponse.DashboardStats stats = FraudDashboardResponse.DashboardStats.builder()
                .totalClaims(total)
                .highRisk(highRisk)
                .mediumRisk(mediumRisk)
                .lowRisk(lowRisk)
                .flagged(highRisk + mediumRisk)
                .build();

        return FraudDashboardResponse.builder()
                .stats(stats)
                .claims(fraudClaims)
                .build();
    }
}
