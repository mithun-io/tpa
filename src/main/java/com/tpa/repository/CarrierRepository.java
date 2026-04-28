package com.tpa.repository;

import com.tpa.entity.Carrier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CarrierRepository extends JpaRepository<Carrier, Long> {
    Optional<Carrier> findByCompanyNameIgnoreCase(String companyName);
    Optional<Carrier> findByUser_Email(String email);
    Optional<Carrier> findByUser_Username(String username);
    Optional<Carrier> findByUser_Id(Long userId);
    boolean existsByRegistrationNumber(String registrationNumber);
}
