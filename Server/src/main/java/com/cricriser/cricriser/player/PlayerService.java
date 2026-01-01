package com.cricriser.cricriser.player;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.cricriser.cricriser.player.playerstats.PlayerStats;
import com.cricriser.cricriser.player.playerstats.PlayerStatsRepository;
import com.cricriser.cricriser.security.JwtBlacklistService;
import com.cricriser.cricriser.security.JwtUtil;
import com.cricriser.cricriser.service.EmailService;
import com.cricriser.cricriser.team.Team;
import com.cricriser.cricriser.team.TeamRepository;

@Service
public class PlayerService {

    @Autowired
    private PlayerRepository playerRepository;
    @Autowired
    private PlayerStatsRepository statsRepository;
    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private EmailService emailService;
    @Autowired
    private JwtBlacklistService jwtBlacklistService;

    // Temporary stores for OTP verification during registration
    private final Map<String, RegisterRequest> tempRegisterData = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<String, String> tempOtpStore = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> otpTimeStore = new java.util.concurrent.ConcurrentHashMap<>();

    // ================= REGISTER =====================
    public String signup(RegisterRequest req) {

        if (playerRepository.findByEmail(req.getEmail()).isPresent()) {
            throw new RuntimeException("Email already registered");
        }

        String otp = String.format("%06d", new Random().nextInt(1_000_000));

        // Save OTP in temp store
        tempRegisterData.put(req.getEmail(), req);
        tempOtpStore.put(req.getEmail(), otp);
        otpTimeStore.put(req.getEmail(), LocalDateTime.now());

        // Send OTP email
        emailService.sendOtpEmail(req.getEmail(), otp);

        return "OTP sent to the email: " + req.getEmail();
    }

    // ================= VERIFY OTP =====================
    public String verifyOtp(String email, String otp) {

        if (!tempOtpStore.containsKey(email)) {
            throw new RuntimeException("No OTP found for this email. Please signup again.");
        }

        if (!otp.equals(tempOtpStore.get(email))) {
            throw new RuntimeException("Invalid OTP");
        }

        if (otpTimeStore.get(email).plusMinutes(10).isBefore(LocalDateTime.now())) {
            throw new RuntimeException("OTP expired");
        }

        // Retrieve signup data
        RegisterRequest req = tempRegisterData.get(email);

        Player player = new Player();
        player.setName(req.getName());
        player.setEmail(req.getEmail());
        player.setPassword(passwordEncoder.encode(req.getPassword()));
        player.setIsVerified(true);
        player.setCurrentTeamId(null);

        playerRepository.save(player);

        // Create blank stats
        PlayerStats stats = new PlayerStats();
        stats.setPlayerId(player.getId());
        statsRepository.save(stats);

        // CLEAR TEMP DATA
        tempOtpStore.remove(email);
        tempRegisterData.remove(email);
        otpTimeStore.remove(email);

        return "Email verified successfully and account created!";
    }

    // ================= LOGIN =====================
    public Map<String, String> login(String email, String password) {

        Player player = playerRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Player not found"));

        if (!player.getIsVerified()) {
            throw new RuntimeException("Email not verified");
        }

        if (!passwordEncoder.matches(password, player.getPassword())) {
            throw new RuntimeException("Invalid password");
        }

        String token = jwtUtil.generateToken(email);

        return Map.of(
                "token", token,
                "playerId", player.getId()
        );
    }

    // ================= FORGOT PASSWORD =====================
    public String forgotPassword(String email) {

        Player player = playerRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Player not found"));

        String otp = String.format("%06d", new Random().nextInt(1_000_000));
        player.setOtp(otp);
        player.setOtpGeneratedAt(LocalDateTime.now());

        playerRepository.save(player);
        emailService.sendOtpEmail(email, otp);

        return "OTP sent to email : " + email;
    }

    // ================= VERIFY FORGOT =====================
    public String verifyForgotOtp(String email, String otp, String newPassword) {

        Player player = playerRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Player not found"));

        if (!otp.equals(player.getOtp())) {
            throw new RuntimeException("Invalid OTP");
        }

        if (player.getOtpGeneratedAt().plusMinutes(10).isBefore(LocalDateTime.now())) {
            throw new RuntimeException("OTP expired");
        }

        player.setPassword(passwordEncoder.encode(newPassword));
        player.setOtp(null);
        player.setOtpGeneratedAt(null);
        playerRepository.save(player);

        return "Password updated successfully";
    }

    // ================= RESET PASSWORD =====================
    public String resetPassword(String oldPassword, String newPassword) {

        Player player = getCurrentPlayer();

        if (!passwordEncoder.matches(oldPassword, player.getPassword())) {
            throw new RuntimeException("Incorrect old password");
        }

        player.setPassword(passwordEncoder.encode(newPassword));
        playerRepository.save(player);

        return "Password updated";
    }

    // ================= UPDATE PROFILE =====================
    public Player updateProfile(Player req) {

        Player p = getCurrentPlayer();

        if (req.getName() != null) {
            p.setName(req.getName());
        }
        if (req.getRole() != null) {
            p.setRole(req.getRole());
        }
        if (req.getBattingStyle() != null) {
            p.setBattingStyle(req.getBattingStyle());
        }
        if (req.getBowlingType() != null) {
            p.setBowlingType(req.getBowlingType());
        }
        if (req.getBowlingStyle() != null) {
            p.setBowlingStyle(req.getBowlingStyle());
        }

        return playerRepository.save(p);
    }

    // ================= DELETE PLAYER STATS BY PLAYER ID =====================
    public void deletePlayerStatsByPlayerId(String playerId) {

        PlayerStats stats = statsRepository.findByPlayerId(playerId);

        if (stats == null) {
            throw new RuntimeException("Player stats not found");
        }

        statsRepository.delete(stats);
    }

    // ================= UPDATE STATS =====================
    public PlayerStats updatePlyerStats(PlayerStats req) {

        Player player = getCurrentPlayer();

        PlayerStats stats = statsRepository.findByPlayerId(player.getId());

        if (stats == null) {
            throw new RuntimeException("Stats not found");
        }

        stats.setMatches(req.getMatches());
        stats.setInnings(req.getInnings());
        stats.setRunsScored(req.getRunsScored());
        stats.setBattingAverage(req.getBattingAverage());
        stats.setBattingStrikeRate(req.getBattingStrikeRate());
        stats.setFours(req.getFours());
        stats.setSixes(req.getSixes());
        stats.setHundreds(req.getHundreds());
        stats.setFifties(req.getFifties());
        stats.setHighestScore(req.getHighestScore());

        stats.setWickets(req.getWickets());
        stats.setBowlingAverage(req.getBowlingAverage());
        stats.setRunsConceded(req.getRunsConceded());
        stats.setBowlingStrikeRate(req.getBowlingStrikeRate());
        stats.setEconomy(req.getEconomy());
        stats.setFiveWicketHauls(req.getFiveWicketHauls());
        stats.setBestBowlingFigures(req.getBestBowlingFigures());
        stats.setBestMatchFigures(req.getBestMatchFigures());

        return statsRepository.save(stats);
    }

    // ================= LOGOUT =====================
    public void logout(String token) {

        if (!jwtBlacklistService.isBlacklisted(token)) {
            jwtBlacklistService.blacklistToken(token);
        }
    }

    // ================= DELETE ACCOUNT =====================
    public void deleteAccount() {

        Player player = getCurrentPlayer();

        if (player.getCurrentTeamId() != null) {
            Team team = teamRepository.findById(player.getCurrentTeamId()).orElse(null);
            if (team != null) {
                team.getSquadPlayerIds().remove(player.getId());
                teamRepository.save(team);
            }
        }

        PlayerStats stats = statsRepository.findByPlayerId(player.getId());
        if (stats != null) {
            statsRepository.delete(stats);
        }

        playerRepository.deleteById(player.getId());
    }

    // ================= CURRENT PLAYER =====================
    private Player getCurrentPlayer() {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || auth.getPrincipal() == null
                || auth.getPrincipal().equals("anonymousUser")) {
            throw new RuntimeException("Unauthorized");
        }

        String email = auth.getPrincipal().toString();

        return playerRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Player not found"));
    }

    // ================= GET CURRENT PLAYER PROFILE =====================
    public Player getCurrentPlayerProfile(String email) {

        return playerRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Player not found"));
    }

    // ================= EXTRACT EMAIL =====================
    public String extractEmail(String token) {

        if (jwtBlacklistService.isBlacklisted(token)) {
            throw new RuntimeException("Token expired");
        }

        return jwtUtil.extractEmail(token);
    }

}
