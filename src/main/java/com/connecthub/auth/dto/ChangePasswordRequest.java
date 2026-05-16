package com.connecthub.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChangePasswordRequest {
    @NotBlank
    public String currentPassword;

    @NotBlank
    @Size(min = 6)
    public String newPassword;
}
