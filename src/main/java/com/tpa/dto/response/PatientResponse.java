
package com.tpa.dto.response;

import com.tpa.enums.Gender;
import com.tpa.enums.UserStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;
@Data
public class PatientResponse {

    private Long id;

    private String patientName;

    private String email;

    private String phoneNumber;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateOfBirth;

    private Gender gender;

    private String address;

    private UserStatus status;
}