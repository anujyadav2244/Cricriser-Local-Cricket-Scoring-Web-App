package com.cricriser.cricriser.admin;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.cricriser.cricriser.security.JwtBlacklistService;
import com.cricriser.cricriser.security.JwtUtil;
import com.cricriser.cricriser.service.EmailService;

@Service
public class AdminService {

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private EmailService emailService;

    @Autowired
    private JwtBlacklistService jwtBlacklistService;

    // ----------------- SIGNUP -----------------
    public String signup(Admin admin) {
        if (adminRepository.findByEmail(admin.getEmail()).isPresent()) {
            throw new RuntimeException("Email already registered!");
        }

        String otp = String.format("%06d", new Random().nextInt(1_000_000));
        admin.setOtp(otp);
        admin.setOtpGeneratedAt(LocalDateTime.now());
        admin.setVerified(false);
        admin.setPassword(passwordEncoder.encode(admin.getPassword()));

        emailService.sendOtpEmail(admin.getEmail(), otp);
        adminRepository.save(admin);
        return "OTP sent to email!";
    }

    // ----------------- VERIFY SIGNUP OTP -----------------
    public String verifyOtp(String email, String otp) {
        Admin admin = adminRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found!"));

        if (admin.getVerified())
            return "User already verified!";
        if (!otp.equalsIgnoreCase(admin.getOtp()))
            throw new RuntimeException("Invalid OTP!");
        if (admin.getOtpGeneratedAt().plusMinutes(10).isBefore(LocalDateTime.now())) {
            throw new RuntimeException("OTP expired!");
        }

        admin.setVerified(true);
        admin.setOtp(null);
        admin.setOtpGeneratedAt(null);
        adminRepository.save(admin);

        return "Email verified successfully!";
    }

    // ----------------- LOGIN -----------------
    public Map<String, String> login(String email, String password) {
        // Check if admin exists
        Optional<Admin> optionalAdmin = adminRepository.findByEmail(email);
        if (optionalAdmin.isEmpty()) {
            throw new RuntimeException("No account found with this email");
        }

        Admin admin = optionalAdmin.get();

        // Check if email is verified
        if (!admin.getVerified()) {
            throw new RuntimeException("Email not verified. Please verify your email first.");
        }

        // Check if password matches
        if (!passwordEncoder.matches(password, admin.getPassword())) {
            throw new RuntimeException("Incorrect password");
        }

        // Generate JWT token
        String token = jwtUtil.generateToken(email);

        return Map.of(
                "token", token,
                "userId", String.valueOf(admin.getId()));
    }

    // ----------------- FORGOT PASSWORD -----------------
    public String forgotPassword(String email) {
        Admin admin = adminRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found!"));

        String otp = String.format("%06d", new Random().nextInt(1_000_000));
        admin.setOtp(otp);
        admin.setOtpGeneratedAt(LocalDateTime.now());
        adminRepository.save(admin);

        emailService.sendOtpEmail(email, otp);
        return "OTP sent to email!";
    }

    // ----------------- VERIFY FORGOT OTP -----------------
    public String verifyForgotOtp(String email, String otp, String newPassword) {
        Admin admin = adminRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found!"));

        if (!otp.equalsIgnoreCase(admin.getOtp()))
            throw new RuntimeException("Invalid OTP!");
        if (admin.getOtpGeneratedAt().plusMinutes(10).isBefore(LocalDateTime.now())) {
            throw new RuntimeException("OTP expired!");
        }

        admin.setPassword(passwordEncoder.encode(newPassword));
        admin.setOtp(null);
        admin.setOtpGeneratedAt(null);
        adminRepository.save(admin);

        return "Password updated successfully!";
    }

    // ----------------- RESET PASSWORD (AUTH REQUIRED) -----------------
    public String resetPassword(String email, String oldPassword, String newPassword) {
        Admin admin = adminRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found!"));

        if (!passwordEncoder.matches(oldPassword, admin.getPassword()))
            throw new RuntimeException("Current password is incorrect!");

        admin.setPassword(passwordEncoder.encode(newPassword));
        adminRepository.save(admin);

        return "Password updated successfully!";
    }

    // ----------------- HELPER: GET EMAIL FROM JWT -----------------
    public String getEmailFromToken(String token) {
        if (jwtBlacklistService.isBlacklisted(token)) {
            throw new RuntimeException("Token is blacklisted");
        }
        return jwtUtil.extractEmail(token);
    }

    // ----------------- GET CURRENT USER -----------------
    public Admin getCurrentUser(String email) {
        return adminRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    // ----------------- UPDATE PROFILE -----------------
    public String updateProfile(String email, Map<String, String> updates) {
        Admin admin = getCurrentUser(email);
        if (updates.containsKey("name"))
            admin.setName(updates.get("name"));
        if (updates.containsKey("password"))
            admin.setPassword(passwordEncoder.encode(updates.get("password")));
        adminRepository.save(admin);
        return "Profile updated successfully";
    }

    // ----------------- LOGOUT -----------------
    public void logout(String token) {
        if (token != null && !jwtBlacklistService.isBlacklisted(token)) {
            jwtBlacklistService.blacklistToken(token);
        }
    }

    // ----------------- DELETE ACCOUNT -----------------
    public String deleteCurrentUser(String email) {
        Admin admin = getCurrentUser(email);
        adminRepository.delete(admin);
        return "User account deleted successfully";
    }
}
