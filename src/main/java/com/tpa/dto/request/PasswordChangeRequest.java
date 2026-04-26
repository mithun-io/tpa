package com.tpa.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PasswordChangeRequest {

    @NotBlank(message = "previous password is required")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String previousPassword;

    @NotBlank(message = "new password is required")
    @Size(min = 8, max = 20, message = "new password must be between 8 and 20 characters")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z\\d]).{8,}$", message = "new password must contain uppercase, lowercase, number and special character")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String newPassword;
}