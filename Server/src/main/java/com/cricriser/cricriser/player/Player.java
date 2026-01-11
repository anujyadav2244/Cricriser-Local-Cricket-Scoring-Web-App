package com.cricriser.cricriser.player;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "player")
public class Player {

    @Id
    private String id;

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;
    @NotBlank(message = "oldPassword is required")
    @Size(min = 6, message = "oldPassword must be at least 6 characters")
    private String oldPassword;
    @NotBlank(message = "newPassword is required")
    @Size(min = 6, message = "newPassword must be at least 6 characters")
    private String newPassword;

    private String otp;
    private LocalDateTime otpGeneratedAt;
    private LocalDateTime createdAt = LocalDateTime.now();
    private Boolean isVerified = false;

    // Profile
    private String role;
    private String battingStyle;
    private String bowlingType;
    private String bowlingStyle;
    private String photoUrl;

    // Only 1 team at a time
    private String currentTeamId;

    // Active league details
    private String activeLeagueId;
    private LocalDateTime leagueStartDate;
    private LocalDateTime leagueEndDate;
}
