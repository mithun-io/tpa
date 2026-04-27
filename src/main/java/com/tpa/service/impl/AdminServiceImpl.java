package com.tpa.service.impl;

import com.tpa.dto.response.CustomerResponse;
import com.tpa.dto.response.UserResponse;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import com.tpa.entity.Customer;
import com.tpa.entity.User;
import com.tpa.enums.UserRole;
import com.tpa.enums.UserStatus;
import com.tpa.exception.ConflictException;
import com.tpa.exception.NoResourceFoundException;
import com.tpa.kafka.producer.ProducerService;
import com.tpa.mapper.CustomerMapper;
import com.tpa.mapper.UserMapper;
import com.tpa.repository.CustomerRepository;
import com.tpa.repository.UserRepository;
import com.tpa.service.AdminService;
import com.tpa.dto.request.ClaimReviewRequest;
import com.tpa.dto.response.ClaimResponse;
import com.tpa.entity.Claim;
import com.tpa.enums.ClaimStatus;
import com.tpa.kafka.event.ClaimNotificationEvent;
import com.tpa.dto.response.AiAnalysisResponse;
import com.tpa.dto.response.CarrierResponse;
import com.tpa.mapper.ClaimMapper;
import com.tpa.repository.CarrierRepository;
import com.tpa.repository.ClaimRepository;
import com.tpa.kafka.ClaimEventProducer;
import com.tpa.service.AiClaimAssistantService;
import com.tpa.service.AuditLogService;
import com.tpa.service.ClaimStateMachine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;

    private final UserMapper userMapper;
    private final CustomerMapper customerMapper;
    private final ClaimMapper claimMapper;

    private final ProducerService producerService;
    private final ClaimRepository claimRepository;
    private final AiClaimAssistantService aiClaimAssistantService;
    private final AuditLogService auditLogService;
    private final ClaimStateMachine claimStateMachine;
    private final com.tpa.service.NotificationService notificationService;
    private final CarrierRepository carrierRepository;
    private final ClaimEventProducer claimEventProducer;
    private final com.tpa.helper.EmailService emailService;

    private User getUser(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new NoResourceFoundException("user not found"));
    }

    private void validateUserTransition(UserStatus currentStatus, UserStatus target) {
        if (currentStatus == target) {
            throw new ConflictException("user already in " + currentStatus + " state");
        }

        boolean isValid = switch (currentStatus) {
            case ACTIVE -> target == UserStatus.INACTIVE || target == UserStatus.BLOCKED;
            case INACTIVE, BLOCKED -> target == UserStatus.ACTIVE;
            default -> throw new IllegalStateException("unexpected status: " + currentStatus);
        };

        if (!isValid) {
            throw new IllegalArgumentException("invalid status transition");
        }
    }

    @Transactional
    @Override
    public UserResponse blockUser(Long id) {
        User user = getUser(id);
        if (user.getUserRole() == UserRole.FMG_ADMIN) {
            throw new IllegalArgumentException("admin cannot be blocked!");
        }
        validateUserTransition(user.getUserStatus(), UserStatus.BLOCKED);
        user.setUserStatus(UserStatus.BLOCKED);
        return userMapper.toUserResponse(user);
    }

    @Transactional
    @Override
    public UserResponse unblockUser(Long id) {
        User user = getUser(id);
        validateUserTransition(user.getUserStatus(), UserStatus.ACTIVE);
        user.setUserStatus(UserStatus.ACTIVE);
        return userMapper.toUserResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<UserResponse> getAllUsers(int page, int size, String search) {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size, org.springframework.data.domain.Sort.by("createdAt").descending());
        org.springframework.data.domain.Page<User> users;
        if (search != null && !search.trim().isEmpty()) {
            users = userRepository.findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(search.trim(), search.trim(), pageable);
        } else {
            users = userRepository.findAll(pageable);
        }
        return users.map(userMapper::toUserResponse);
    }

    @Transactional
    @Override
    public List<CustomerResponse> getAllCustomers() {
        List<Customer> customers = customerRepository.findAll();
        if (customers.isEmpty()) {
            throw new NoResourceFoundException("no customers found");
        }
        return customerMapper.toCustomerResponses(customers);
    }

    @Transactional
    @Override
    @Caching(evict = {
        @CacheEvict(value = "claims", key = "#request.claimId"),
        @CacheEvict(value = "aiSummaries", key = "#request.claimId")
    })
    public ClaimResponse reviewClaim(ClaimReviewRequest request, Principal principal) {
        Claim claim = claimRepository.findById(request.getClaimId())
                .orElseThrow(() -> new NoResourceFoundException("claim not found"));

        claimStateMachine.validateTransition(claim.getStatus(), request.getStatus());

        ClaimStatus previousStatus = claim.getStatus();
        claim.setStatus(request.getStatus());
        claim.setRejectionReason(request.getReviewNotes());
        claim.setProcessedDate(LocalDateTime.now());
        claim.setReviewedBy(principal.getName());
        claim.setReviewedAt(LocalDateTime.now());
        claim.setReviewNotes(request.getReviewNotes());
        
        claimRepository.save(claim);
        log.info("Admin {} reviewed claim {} with status {}", principal.getName(), claim.getId(), claim.getStatus());

        auditLogService.logAction(claim.getId(), "ADMIN_REVIEW", previousStatus, claim.getStatus());

        // Send Kafka notification
        ClaimNotificationEvent notificationEvent = ClaimNotificationEvent.builder()
                .claimId(claim.getId())
                .policyNumber(claim.getPolicyNumber())
                .customerEmail(claim.getUser().getEmail())
                .status(claim.getStatus())
                .message("Your claim has been " + claim.getStatus() + ". Notes: " + request.getReviewNotes())
                .build();
                
        producerService.sendClaimNotificationEvent(notificationEvent);
        notificationService.createNotification(claim.getUser(), "Claim " + claim.getStatus(), notificationEvent.getMessage(), "/claims/" + claim.getId());

        return claimMapper.toDto(claim);
    }

    @Transactional
    @Override
    @Caching(evict = {
        @CacheEvict(value = "claims", key = "#claimId"),
        @CacheEvict(value = "aiSummaries", key = "#claimId")
    })
    public ClaimResponse approveClaim(Long claimId, String reason, Principal principal) {
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new NoResourceFoundException("claim not found"));

        claimStateMachine.validateTransition(claim.getStatus(), ClaimStatus.APPROVED);

        ClaimStatus previousStatus = claim.getStatus();
        claim.setStatus(ClaimStatus.APPROVED);
        claim.setProcessedDate(LocalDateTime.now());
        claim.setReviewedBy(principal.getName());
        claim.setReviewedAt(LocalDateTime.now());
        claim.setReviewNotes(reason);
        
        claimRepository.save(claim);
        log.info("Admin {} APPROVED claim {}", principal.getName(), claim.getId());

        auditLogService.logAction(claim.getId(), "ADMIN_APPROVED", previousStatus, ClaimStatus.APPROVED);

        ClaimNotificationEvent notificationEvent = ClaimNotificationEvent.builder()
                .claimId(claim.getId())
                .policyNumber(claim.getPolicyNumber())
                .customerEmail(claim.getUser().getEmail())
                .status(ClaimStatus.APPROVED)
                .message("Your claim has been APPROVED. Notes: " + reason)
                .build();
        producerService.sendClaimNotificationEvent(notificationEvent);
        notificationService.createNotification(claim.getUser(), "Claim Approved", notificationEvent.getMessage(), "/claims/" + claim.getId());

        return claimMapper.toDto(claim);
    }

    @Transactional
    @Override
    @Caching(evict = {
        @CacheEvict(value = "claims", key = "#claimId"),
        @CacheEvict(value = "aiSummaries", key = "#claimId")
    })
    public ClaimResponse rejectClaim(Long claimId, String reason, Principal principal) {
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new NoResourceFoundException("claim not found"));

        claimStateMachine.validateTransition(claim.getStatus(), ClaimStatus.REJECTED);

        ClaimStatus previousStatus = claim.getStatus();
        claim.setStatus(ClaimStatus.REJECTED);
        claim.setRejectionReason(reason);
        claim.setProcessedDate(LocalDateTime.now());
        claim.setReviewedBy(principal.getName());
        claim.setReviewedAt(LocalDateTime.now());
        claim.setReviewNotes(reason);
        
        claimRepository.save(claim);
        log.info("Admin {} REJECTED claim {}", principal.getName(), claim.getId());

        auditLogService.logAction(claim.getId(), "ADMIN_REJECTED", previousStatus, ClaimStatus.REJECTED);

        ClaimNotificationEvent notificationEvent = ClaimNotificationEvent.builder()
                .claimId(claim.getId())
                .policyNumber(claim.getPolicyNumber())
                .customerEmail(claim.getUser().getEmail())
                .status(ClaimStatus.REJECTED)
                .message("Your claim has been REJECTED. Reason: " + reason)
                .build();
        producerService.sendClaimNotificationEvent(notificationEvent);
        notificationService.createNotification(claim.getUser(), "Claim Rejected", notificationEvent.getMessage(), "/claims/" + claim.getId());

        return claimMapper.toDto(claim);
    }

    @Override
    @Transactional(readOnly = true)
    public AiAnalysisResponse getClaimAiSummary(Long claimId) {
        if (!claimRepository.existsById(claimId)) {
            throw new NoResourceFoundException("claim not found");
        }
        log.info("Requesting AI summary for claim {}", claimId);
        return aiClaimAssistantService.analyzeClaim(claimId, "Please summarize this claim for an admin reviewer. Highlight any discrepancies or high-risk factors.");
    }

    @Override
    @Transactional(readOnly = true)
    public AiAnalysisResponse askAiAboutClaim(Long claimId, String prompt) {
        if (!claimRepository.existsById(claimId)) {
            throw new NoResourceFoundException("claim not found");
        }
        log.info("Requesting custom AI analysis for claim {} with prompt: {}", claimId, prompt);
        return aiClaimAssistantService.analyzeClaim(claimId, prompt);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CarrierResponse> getAllCarriers() {
        return carrierRepository.findAll().stream().map(c -> CarrierResponse.builder()
            .id(c.getId())
            .companyName(c.getCompanyName())
            .email(c.getUser().getEmail())
            .mobile(c.getUser().getMobile())
            .companyType(c.getCompanyType())
            .licenseNumber(c.getLicenseNumber())
            .registrationNumber(c.getRegistrationNumber())
            .taxId(c.getTaxId())
            .contactPersonName(c.getContactPersonName())
            .contactPersonPhone(c.getContactPersonPhone())
            .website(c.getWebsite())
            .userStatus(c.getUser().getUserStatus().name())
            .aiRiskScore(c.getAiRiskScore())
            .aiRiskStatus(c.getAiRiskStatus())
            .aiRecommendation(c.getAiRecommendation())
            .build()
        ).toList();
    }

    @Override
    @Transactional
    public CarrierResponse approveCarrier(Long carrierId) {
        com.tpa.entity.Carrier carrier = carrierRepository.findById(carrierId)
            .orElseThrow(() -> new NoResourceFoundException("carrier not found"));

        User carrierUser = carrier.getUser();
        carrierUser.setUserStatus(UserStatus.ACTIVE);
        userRepository.saveAndFlush(carrierUser);
        log.info("Carrier {} APPROVED by admin", carrier.getCompanyName());

        // Fire email + Kafka AFTER commit — never blocks the HTTP response, never rolls back DB
        final Long cId = carrier.getId();
        final String cName = carrier.getCompanyName();
        final String cEmail = carrierUser.getEmail();
        TransactionSynchronizationManager.registerSynchronization(new org.springframework.transaction.support.TransactionSynchronization() {
            @Override
            public void afterCommit() {
                // Send approval email
                try {
                    emailService.sendCarrierApprovalEmail(cEmail, cName);
                } catch (Exception e) {
                    log.warn("Failed to send carrier approval email to {}: {}", cEmail, e.getMessage());
                }
                // Publish Kafka event
                try {
                    claimEventProducer.publishCarrierApprovedEvent(cId, cName, cEmail);
                } catch (Exception e) {
                    log.warn("Failed to publish carrier-approved Kafka event for {}: {}", cId, e.getMessage());
                }
            }
        });

        return CarrierResponse.builder()
            .id(carrier.getId())
            .companyName(carrier.getCompanyName())
            .email(carrierUser.getEmail())
            .mobile(carrierUser.getMobile())
            .companyType(carrier.getCompanyType())
            .licenseNumber(carrier.getLicenseNumber())
            .registrationNumber(carrier.getRegistrationNumber())
            .taxId(carrier.getTaxId())
            .contactPersonName(carrier.getContactPersonName())
            .contactPersonPhone(carrier.getContactPersonPhone())
            .website(carrier.getWebsite())
            .userStatus(UserStatus.ACTIVE.name())
            .aiRiskScore(carrier.getAiRiskScore())
            .aiRiskStatus(carrier.getAiRiskStatus())
            .aiRecommendation(carrier.getAiRecommendation())
            .build();
    }

    @Override
    @Transactional
    public CarrierResponse rejectCarrier(Long carrierId) {
        com.tpa.entity.Carrier carrier = carrierRepository.findById(carrierId)
            .orElseThrow(() -> new NoResourceFoundException("carrier not found"));

        User carrierUser = carrier.getUser();
        carrierUser.setUserStatus(UserStatus.BLOCKED);
        userRepository.saveAndFlush(carrierUser);
        log.info("Carrier {} REJECTED by admin", carrier.getCompanyName());

        // Send rejection email AFTER commit
        final String cEmail = carrierUser.getEmail();
        final String cName = carrier.getCompanyName();
        TransactionSynchronizationManager.registerSynchronization(new org.springframework.transaction.support.TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    emailService.sendCarrierRejectionEmail(cEmail, cName);
                } catch (Exception e) {
                    log.warn("Failed to send carrier rejection email to {}: {}", cEmail, e.getMessage());
                }
            }
        });

        return CarrierResponse.builder()
            .id(carrier.getId())
            .companyName(carrier.getCompanyName())
            .email(carrierUser.getEmail())
            .mobile(carrierUser.getMobile())
            .companyType(carrier.getCompanyType())
            .licenseNumber(carrier.getLicenseNumber())
            .registrationNumber(carrier.getRegistrationNumber())
            .taxId(carrier.getTaxId())
            .contactPersonName(carrier.getContactPersonName())
            .contactPersonPhone(carrier.getContactPersonPhone())
            .website(carrier.getWebsite())
            .userStatus(UserStatus.BLOCKED.name())
            .aiRiskScore(carrier.getAiRiskScore())
            .aiRiskStatus(carrier.getAiRiskStatus())
            .aiRecommendation(carrier.getAiRecommendation())
            .build();
    }

    @Override
    @Transactional
    public ClaimResponse assignCarrierToClaim(Long claimId, Long carrierId) {
        Claim claim = claimRepository.findById(claimId)
            .orElseThrow(() -> new NoResourceFoundException("claim not found"));
        com.tpa.entity.Carrier carrier = carrierRepository.findById(carrierId)
            .orElseThrow(() -> new NoResourceFoundException("carrier not found"));
        if (carrier.getUser().getUserStatus() != UserStatus.ACTIVE) {
            throw new IllegalArgumentException("Carrier is not active and cannot be assigned to claims");
        }
        claim.setCarrier(carrier);
        claimRepository.save(claim);
        log.info("Claim {} assigned to carrier {} by admin", claimId, carrier.getCompanyName());
        return claimMapper.toDto(claim);
    }
}