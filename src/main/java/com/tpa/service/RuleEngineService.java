package com.tpa.service;

import com.tpa.dto.request.ClaimDataRequest;
import com.tpa.dto.response.ClaimDecisionResponse;

public interface RuleEngineService {
    ClaimDecisionResponse evaluateClaim(ClaimDataRequest claimData);
}
