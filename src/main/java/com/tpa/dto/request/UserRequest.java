package com.tpa.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tpa.enums.Gender;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UserRequest {

    @NotBlank(message = "username is required")
    private String username;

    @Email(message = "invalid email format")
    @NotBlank(message = "email is required")
    private String email;

    @NotBlank(message = "mobile is required")
    @Pattern(regexp = "^[0-9]{10}$", message = "mobile must be 10 digits")
    private String mobile;

    @NotNull(message = "date of birth is required")
    @Past(message = "date of birth should be past")
    private LocalDate dateOfBirth;

    @NotBlank(message = "address is required")
    private String address;

    @NotBlank(message = "password is required")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$", message = "password must be at least 8 characters and include uppercase, lowercase, number and special character")
    private String password;

    @NotNull(message = "gender is required")
    private Gender gender;
}