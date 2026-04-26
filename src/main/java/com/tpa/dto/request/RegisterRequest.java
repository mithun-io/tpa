package com.tpa.dto.request;

import com.tpa.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {
    private String username;
    private String password;
    private UserRole role;
    
    // Additional fields like email can be added if required by the User entity
}
