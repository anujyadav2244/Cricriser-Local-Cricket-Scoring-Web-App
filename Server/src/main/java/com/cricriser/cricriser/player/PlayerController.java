package com.cricriser.cricriser.player;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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

import com.cricriser.cricriser.player.playerstats.PlayerStats;

@RestController
@RequestMapping("/api/player")
@CrossOrigin(origins = "${app.allowed.origins:http://localhost:5173}", allowCredentials = "true")
public class PlayerController {

    @Autowired
    private PlayerService playerService;

    // ================= REGISTER =================
    @PostMapping("/signup")
    public ResponseEntity<?> register(@RequestBody RegisterRequest req) {
        try {
            return ResponseEntity.ok(playerService.signup(req));
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", ex.getMessage()));
        }
    }

    // ================= VERIFY OTP =================
    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody Map<String, String> req) {
        try {
            return ResponseEntity.ok(playerService.verifyOtp(
                    req.get("email"),
                    req.get("otp")
            ));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", ex.getMessage()));
        }
    }

    // ================= LOGIN =================
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> req) {
        try {
            return ResponseEntity.ok(
                    playerService.login(
                            req.get("email"),
                            req.get("password")
                    )
            );
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", ex.getMessage()));
        }
    }

    // ================= FORGOT PASSWORD =================
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> req) {
        try {
            return ResponseEntity.ok(playerService.forgotPassword(req.get("email")));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", ex.getMessage()));
        }
    }

    // ================= VERIFY FORGOT OTP =================
    @PostMapping("/verify-forgot-otp")
    public ResponseEntity<?> verifyForgotOtp(@RequestBody Map<String, String> req) {
        try {
            return ResponseEntity.ok(
                    playerService.verifyForgotOtp(
                            req.get("email"),
                            req.get("otp"),
                            req.get("newPassword")
                    )
            );
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", ex.getMessage()));
        }
    }

    // ================= RESET PASSWORD =================
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(
            @RequestHeader("Authorization") String token,
            @RequestBody Map<String, String> req) {
        try {
            return ResponseEntity.ok(
                    playerService.resetPassword(
                            req.get("oldPassword"),
                            req.get("newPassword")
                    )
            );
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", ex.getMessage()));
        }
    }

    // ================= UPDATE PROFILE =================
    @PutMapping("/update-profile")
    public ResponseEntity<?> updateProfile(
            @RequestHeader("Authorization") String token,
            @RequestBody Player req) {
        try {
            return ResponseEntity.ok(playerService.updateProfile(req));
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", ex.getMessage()));
        }
    }

    // ================= UPDATE STATS =================
    @PutMapping("/update-stats")
    public ResponseEntity<?> updateStats(
            @RequestHeader("Authorization") String token,
            @RequestBody PlayerStats stats) {
        try {
            return ResponseEntity.ok(playerService.updatePlyerStats(stats));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", ex.getMessage()));
        }
    }

    // ================= GET CURRENT PLAYER =================
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentPlayer(
            @RequestHeader("Authorization") String token) {
        try {
            String email = playerService.extractEmail(token.substring(7));
            return ResponseEntity.ok(playerService.getCurrentPlayerProfile(email));
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", ex.getMessage()));
        }
    }

    // ================= LOGOUT =================
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader("Authorization") String token) {
        try {
            playerService.logout(token.substring(7));
            return ResponseEntity.ok("Logged out successfully");
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", ex.getMessage()));
        }
    }

    // ================= DELETE ACCOUNT =================
    @DeleteMapping("/delete-account")
    public ResponseEntity<?> deleteAccount(
            @RequestHeader("Authorization") String token) {
        try {
            playerService.deleteAccount();
            return ResponseEntity.ok("Account deleted successfully");
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", ex.getMessage()));
        }
    }
}
