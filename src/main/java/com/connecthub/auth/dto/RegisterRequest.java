package com.connecthub.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50)
    public String username;

    @Email
    @NotBlank
    public String email;

    @NotBlank
    @Size(min = 6)
    public String password;

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100)
    public String fullName;
}
