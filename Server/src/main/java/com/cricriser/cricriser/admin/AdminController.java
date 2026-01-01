package com.cricriser.cricriser.admin;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "${app.allowed.origins:http://localhost:5173}", allowCredentials = "true")
public class AdminController {

    @Autowired
    private AdminService adminService;

    // SIGNUP
    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody Admin admin) {
        try {
            return ResponseEntity.ok(adminService.signup(admin));
        } catch (RuntimeException e) {
            return ResponseEntity
                    .status(401)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // VERIFY EMAIL OTP (JSON)
    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody Map<String, String> body) {

        String email = body.get("email");
        String otp = body.get("otp");

        return ResponseEntity.ok(
                Map.of("message", adminService.verifyOtp(email, otp))
        );
    }

    // LOGIN
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {

        String email = body.get("email");
        String password = body.get("password");

        try {
            return ResponseEntity.ok(adminService.login(email, password));
        } catch (RuntimeException e) {
            return ResponseEntity
                    .status(401)
                    .body(Map.of("error", e.getMessage()));
        }

    }

    // FORGOT PASSWORD
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgot(@RequestBody Map<String, String> body) {

        return ResponseEntity.ok(Map.of(
                "message", adminService.forgotPassword(body.get("email"))
        ));
    }

    // VERIFY FORGOT OTP
    @PostMapping("/verify-forgot-otp")
    public ResponseEntity<?> verifyForgot(@RequestBody Map<String, String> body) {

        return ResponseEntity.ok(Map.of(
                "message",
                adminService.verifyForgotOtp(
                        body.get("email"),
                        body.get("otp"),
                        body.get("newPassword"))
        ));
    }

    // RESET PASSWORD
    @PutMapping("/reset-password")
    public ResponseEntity<?> resetPassword(
            @RequestHeader("Authorization") String token,
            @RequestBody Map<String, String> body) {

        String jwt = token.startsWith("Bearer ") ? token.substring(7) : token;
        String email = adminService.getEmailFromToken(jwt);

        return ResponseEntity.ok(Map.of(
                "message",
                adminService.resetPassword(
                        email,
                        body.get("oldPassword"),
                        body.get("newPassword"))
        ));
    }

    // CURRENT USER
    @GetMapping("/me")
    public ResponseEntity<?> me(@RequestHeader("Authorization") String token) {

        String jwt = token.startsWith("Bearer ") ? token.substring(7) : token;
        String email = adminService.getEmailFromToken(jwt);

        return ResponseEntity.ok(adminService.getCurrentUser(email));
    }

    // UPDATE PROFILE
    @PutMapping("/update-profile")
    public ResponseEntity<?> updateProfile(
            @RequestHeader("Authorization") String token,
            @RequestBody Map<String, String> updates) {

        String jwt = token.startsWith("Bearer ") ? token.substring(7) : token;
        String email = adminService.getEmailFromToken(jwt);

        return ResponseEntity.ok(Map.of(
                "message", adminService.updateProfile(email, updates)));
    }

    // LOGOUT
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader("Authorization") String token) {

        String jwt = token.startsWith("Bearer ") ? token.substring(7) : token;
        adminService.logout(jwt);

        return ResponseEntity.ok(Map.of("message", "Logout successful"));
    }

    // DELETE ACCOUNT
    @DeleteMapping("/delete")
    public ResponseEntity<?> delete(@RequestHeader("Authorization") String token) {

        String jwt = token.startsWith("Bearer ") ? token.substring(7) : token;
        String email = adminService.getEmailFromToken(jwt);

        return ResponseEntity.ok(
                Map.of("message", adminService.deleteCurrentUser(email))
        );
    }
}
