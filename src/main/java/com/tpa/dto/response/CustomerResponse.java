package com.tpa.dto.response;

import com.tpa.enums.Gender;
import com.tpa.enums.UserRole;
import com.tpa.enums.UserStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class CustomerResponse {

    private Long id;

    private String name;

    private String email;

    private String mobile;

    private Gender gender;

    private LocalDate dateOfBirth;

    private String address;

    private UserRole customerRole;

    private UserStatus customerStatus;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm a")
    private LocalDateTime createdAt;
}