package com.cricriser.cricriser.match.matchscheduling;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.cricriser.cricriser.league.League;
import com.cricriser.cricriser.league.LeagueRepository;
import com.cricriser.cricriser.security.JwtBlacklistService;
import com.cricriser.cricriser.security.JwtUtil;

@Service
public class MatchScheduleService {

    @Autowired
    private MatchScheduleRepository repo;

    @Autowired
    private LeagueRepository leagueRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private JwtBlacklistService blacklistService;

    // ================= MANUAL MATCH CREATION =================
    public MatchSchedule createMatchManually(String token, MatchSchedule match) throws Exception {

        String adminId = validateToken(token);

        // ========== LEAGUE VALIDATION ==========
        League league = leagueRepository.findById(match.getLeagueId())
                .orElseThrow(() -> new Exception("League not found"));

        if (!league.getAdminId().equalsIgnoreCase(adminId)) {
            throw new Exception("You are not authorized for this league");
        }

        // ========== TEAM ID VALIDATION ==========
        // league.teams entries are stored like "India:TEAM_ID"
        List<String> teamIdsInLeague = league.getTeams().stream()
                .map(t -> t.contains(":") ? t.split(":")[1] : t)
                .toList();

        if (!teamIdsInLeague.contains(match.getTeam1Id())) {
            throw new Exception("Team1 does not belong to this league");
        }

        if (!teamIdsInLeague.contains(match.getTeam2Id())) {
            throw new Exception("Team2 does not belong to this league");
        }

        if (match.getTeam1Id().equals(match.getTeam2Id())) {
            throw new Exception("Team1 and Team2 cannot be same");
        }

        // ========== AUTO MATCH NUMBER ==========
        long currentCount = repo.countByLeagueId(match.getLeagueId());
        int nextMatchNo = (int) currentCount + 1;
        match.setMatchNo(nextMatchNo);

        // ========== AUTO MATCH TYPE ==========
        if (match.getMatchType() == null || match.getMatchType().isEmpty()) {
            match.setMatchType("LEAGUE");
        }

        // ========== STATUS ==========
        if (match.getStatus() == null || match.getStatus().isEmpty()) {
            match.setStatus("Scheduled");
        }

        // ========== MATCH OVERS (DEFAULT FROM LEAGUE) ==========
        if (match.getMatchOvers() == null) {
            match.setMatchOvers(league.getOversPerInnings());
        }

        // ========== SAVE ==========
        MatchSchedule saved = repo.save(match);

        // ========== UPDATE LEAGUE NO OF MATCHES ==========
        league.setNoOfMatches(nextMatchNo);
        leagueRepository.save(league);

        return saved;
    }

    // ================= GET ALL =================
    public List<MatchSchedule> getAllMatches() {
        return repo.findAll();
    }

    // ================= GET BY ID =================
    public MatchSchedule getMatchById(String id) throws Exception {
        return repo.findById(id)
                .orElseThrow(() -> new Exception("Match not found with id: " + id));
    }

    // ================= UPDATE MATCH =================
    public MatchSchedule updateMatch(String token, String id, MatchSchedule updated) throws Exception {

        String adminId = validateToken(token);

        MatchSchedule existing = repo.findById(id)
                .orElseThrow(() -> new Exception("Match not found"));

        League league = leagueRepository.findById(existing.getLeagueId())
                .orElseThrow(() -> new Exception("League not found"));

        if (!league.getAdminId().equalsIgnoreCase(adminId)) {
            throw new Exception("You are not authorized to update this match");
        }

        // ❌ DO NOT ALLOW TEAM CHANGE
        if (updated.getTeam1Id() != null || updated.getTeam2Id() != null) {
            throw new Exception("Team change is NOT allowed for this match");
        }

        // ✅ ALLOWED FIELDS
        if (updated.getScheduledDate() != null) {
            existing.setScheduledDate(updated.getScheduledDate());
        }

        if (updated.getVenue() != null) {
            existing.setVenue(updated.getVenue());
        }

        if (updated.getStatus() != null) {
            existing.setStatus(updated.getStatus());
        }

        if (updated.getMatchOvers() != null) {
            existing.setMatchOvers(updated.getMatchOvers());
        }

        return repo.save(existing);
    }

    // ================= DELETE MATCH =================
    public void deleteMatch(String token, String id) throws Exception {

        String adminId = validateToken(token);

        MatchSchedule match = repo.findById(id)
                .orElseThrow(() -> new Exception("Match not found"));

        League league = leagueRepository.findById(match.getLeagueId())
                .orElseThrow(() -> new Exception("League not found"));

        if (!league.getAdminId().equalsIgnoreCase(adminId)) {
            throw new Exception("You are not authorized to delete this match");
        }

        repo.delete(match);
    }

    // ================= DELETE ALL MATCHES BY ADMIN =================
    public void deleteAllMatchesByAdmin(String token) throws Exception {

        String adminId = validateToken(token);

        List<League> leagues = leagueRepository.findByAdminId(adminId);

        for (League league : leagues) {
            List<MatchSchedule> matches = repo.findByLeagueId(league.getId());
            repo.deleteAll(matches);
        }
    }

    // ================= TOKEN VALIDATION =================
    private String validateToken(String token) throws Exception {

        if (token == null || !token.startsWith("Bearer ")) {
            throw new Exception("Authorization header missing or invalid");
        }

        String jwt = token.substring(7);

        if (blacklistService.isBlacklisted(jwt)) {
            throw new Exception("Token is invalid or logged out. Please login again");
        }

        return jwtUtil.extractEmail(jwt); // adminId / email
    }
}
