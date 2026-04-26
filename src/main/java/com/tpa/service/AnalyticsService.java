package com.tpa.service;

import com.tpa.enums.ClaimStatus;
import com.tpa.repository.ClaimRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final ClaimRepository claimRepository;

    public Map<String, Object> getDashboardAnalytics() {
        Map<String, Object> response = new HashMap<>();

        // Status counts
        List<Object[]> statusCountsObj = claimRepository.countClaimsByStatus();
        Map<String, Long> statusCounts = new HashMap<>();
        for (Object[] obj : statusCountsObj) {
            statusCounts.put(((ClaimStatus) obj[0]).name(), (Long) obj[1]);
        }
        response.put("statusDistribution", statusCounts);

        // Claims per day (last 30 days)
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        List<Object[]> dailyCountsObj = claimRepository.countClaimsPerDay(thirtyDaysAgo);
        List<Map<String, Object>> dailyCounts = dailyCountsObj.stream().map(obj -> {
            Map<String, Object> map = new HashMap<>();
            map.put("date", obj[0].toString());
            map.put("count", obj[1]);
            return map;
        }).collect(Collectors.toList());
        response.put("claimsPerDay", dailyCounts);

        // Total Approved Payout
        Double totalPayout = claimRepository.sumApprovedClaimAmount();
        response.put("totalApprovedPayout", totalPayout != null ? totalPayout : 0.0);

        return response;
    }
}
