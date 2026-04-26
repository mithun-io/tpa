package com.tpa.helper;

import com.tpa.dto.request.CarrierRegistrationRequest;
import com.tpa.dto.request.CustomerRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tpa.dto.request.PatientRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@Component
@RequiredArgsConstructor
public class RedisService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String OTP_PREFIX = "OTP:";
    private static final String PENDING_CUSTOMER_PREFIX = "PENDING:CUSTOMER:";
    private static final String PENDING_PATIENT_PREFIX = "PENDING:PATIENT:";
    private static final String PENDING_CARRIER_PREFIX = "PENDING:CARRIER:";

    public Integer getOtp(String email) {
        return (Integer) redisTemplate.opsForValue().get(OTP_PREFIX + email);
    }

    public CustomerRequest getPendingCustomer(String email) {
        try {
            String json = (String) redisTemplate.opsForValue().get(PENDING_CUSTOMER_PREFIX + email);
            if (json == null) return null;
            return objectMapper.readValue(json, CustomerRequest.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("error while parsing customer json from redis", e);
        }
    }

    public PatientRequest getPendingPatient(String email) {
        try {
            String json = (String) redisTemplate.opsForValue().get(PENDING_PATIENT_PREFIX + email);
            if (json == null) return null;
            return objectMapper.readValue(json, PatientRequest.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("error while parsing customer json from redis", e);
        }
    }

    public void storeOtp(String email, Integer otp) {
        redisTemplate.opsForValue().set(OTP_PREFIX + email, otp, Duration.ofMinutes(5));
    }

    public void storePendingCustomer(String email, CustomerRequest customerRequest) {
        try {
            String json = objectMapper.writeValueAsString(customerRequest);
            redisTemplate.opsForValue().set(PENDING_CUSTOMER_PREFIX + email, json, Duration.ofMinutes(5));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("error while parsing customer json from redis", e);
        }
    }

    public void storePendingPatient(String email, PatientRequest patientRequest) {
        try {
            String json = objectMapper.writeValueAsString(patientRequest);
            redisTemplate.opsForValue().set(PENDING_PATIENT_PREFIX + email, json, Duration.ofMinutes(5));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("error while parsing customer json from redis", e);
        }
    }

    public void deleteOtp(String email) {
        redisTemplate.delete(OTP_PREFIX + email);
    }

    public void deletePendingCustomer(String email) {
        redisTemplate.delete(PENDING_CUSTOMER_PREFIX + email);
    }

    public void deletePendingPatient(String email) {
        redisTemplate.delete(PENDING_PATIENT_PREFIX + email);
    }

    public boolean isPendingCustomerExists(String email) {
        return getPendingCustomer(email) != null;
    }

    public boolean isPendingPatientExists(String email) {
        return getPendingPatient(email) != null;
    }

    public CarrierRegistrationRequest getPendingCarrier(String email) {
        try {
            String json = (String) redisTemplate.opsForValue().get(PENDING_CARRIER_PREFIX + email);
            if (json == null) return null;
            return objectMapper.readValue(json, CarrierRegistrationRequest.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("error while parsing carrier json from redis", e);
        }
    }

    public void storePendingCarrier(String email, CarrierRegistrationRequest request) {
        try {
            String json = objectMapper.writeValueAsString(request);
            redisTemplate.opsForValue().set(PENDING_CARRIER_PREFIX + email, json, Duration.ofMinutes(5));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("error while serializing carrier json to redis", e);
        }
    }

    public void deletePendingCarrier(String email) {
        redisTemplate.delete(PENDING_CARRIER_PREFIX + email);
    }

    public boolean isPendingCarrierExists(String email) {
        return getPendingCarrier(email) != null;
    }
}