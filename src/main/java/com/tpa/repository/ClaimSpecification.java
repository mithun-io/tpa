package com.tpa.repository;

import com.tpa.entity.Claim;
import com.tpa.enums.ClaimStatus;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

public class ClaimSpecification {

    public static Specification<Claim> hasStatus(ClaimStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<Claim> createdBetween(LocalDateTime from, LocalDateTime to) {
        return (root, query, cb) -> {
            if (from != null && to != null) return cb.between(root.get("createdDate"), from, to);
            if (from != null) return cb.greaterThanOrEqualTo(root.get("createdDate"), from);
            if (to != null) return cb.lessThanOrEqualTo(root.get("createdDate"), to);
            return null;
        };
    }

    public static Specification<Claim> amountBetween(Double minAmount, Double maxAmount) {
        return (root, query, cb) -> {
            if (minAmount != null && maxAmount != null) return cb.between(root.get("amount"), minAmount, maxAmount);
            if (minAmount != null) return cb.greaterThanOrEqualTo(root.get("amount"), minAmount);
            if (maxAmount != null) return cb.lessThanOrEqualTo(root.get("amount"), maxAmount);
            return null;
        };
    }

    public static Specification<Claim> hasUser(String username) {
        return (root, query, cb) -> {
            if (username == null || username.isEmpty()) return null;
            return cb.or(
                cb.equal(root.join("user").get("username"), username),
                cb.equal(root.join("user").get("email"), username)
            );
        };
    }
}
