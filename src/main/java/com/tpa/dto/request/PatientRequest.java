
package com.tpa.dto.request;

import com.tpa.enums.Gender;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;

@Data
public class PatientRequest {

    @NotBlank(message = "patient name is required")
    @Size(min = 2, max = 100, message = "patient name must be between 2 and 100 characters")
    private String patientName;

    @NotBlank(message = "email is required")
    @Email(message = "invalid email format")
    private String email;

    @NotBlank(message = "mobile number is required")
    @Pattern(regexp = "^[6-9]\\d{9}$", message = "invalid mobile number")
    private String phoneNumber;

    @NotBlank(message = "password is required")
    @Size(min = 8, max = 20, message = "password must be between 8 and 20 characters")
    @Pattern(regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*[!@#$%&?:*/])(?=.*\\d)[A-Za-z!@#$%&?:*/\\d]{8,}$", message = "password must contain 8 characters with uppercase, lowercase, number and special character")
    private String password;

    @NotNull(message = "date of birth is required")
    @Past(message = "date of birth must be in the past")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @JsonSerialize(using = LocalDateSerializer.class)
    @JsonDeserialize(using = LocalDateDeserializer.class)
    private LocalDate dateOfBirth;

    @NotNull(message = "gender is required")
    private Gender gender;

    @NotBlank(message = "address is required")
    @Size(max = 300, message = "address must not exceed 300 characters")
    private String address;
}