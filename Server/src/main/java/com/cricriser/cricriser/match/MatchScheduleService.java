package com.cricriser.cricriser.match;

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
    private  MatchScheduleRepository repo;

    @Autowired
    private LeagueRepository leagueRepository;

    @Autowired 
    private JwtUtil jwtUtil;

    @Autowired
    private JwtBlacklistService blacklistService;

    // ================= MANUAL MATCH CREATION =================
    // Only use this if admin wants to create a match manually after league
    // scheduling
    public MatchSchedule createMatchManually(String token, MatchSchedule match) throws Exception {
        String adminId = validateToken(token);

        // Validate league exists
        League league = leagueRepository.findById(match.getLeagueId())
                .orElseThrow(() -> new Exception("League not found"));

        // Check if logged-in user is the league admin
        if (!league.getAdminId().equalsIgnoreCase(adminId))
            throw new Exception("You are not authorized to schedule matches for this league");

        // Validate that teams exist in the league
        boolean team1Exists = league.getTeams().stream()
                .anyMatch(t -> t.startsWith(match.getTeam1() + ":"));
        boolean team2Exists = league.getTeams().stream()
                .anyMatch(t -> t.startsWith(match.getTeam2() + ":"));

        if (!team1Exists)
            throw new Exception(match.getTeam1() + " is not part of this league!");
        if (!team2Exists)
            throw new Exception(match.getTeam2() + " is not part of this league!");
        if (match.getTeam1().equalsIgnoreCase(match.getTeam2()))
            throw new Exception("Team1 and Team2 cannot be the same!");

        // Default status
        if (match.getStatus() == null || match.getStatus().isEmpty())
            match.setStatus("Scheduled");

        return repo.save(match);
    }

    // ================= GET ALL MATCHES =================
    public List<MatchSchedule> getAllMatches() {
        return repo.findAll();
    }

    // ================= GET MATCH BY ID =================

    public MatchSchedule getMatchById(String id) throws Exception {
        return repo.findById(id)
                .orElseThrow(() -> new Exception("Match not found with id: " + id));
    }

    // ================= UPDATE MATCH =================
    public MatchSchedule updateMatch(String token, String id, MatchSchedule updatedMatch) throws Exception {
        String adminId = validateToken(token);

        // Fetch existing match
        MatchSchedule existing = repo.findById(id)
                .orElseThrow(() -> new Exception("Match not found"));

        // Fetch league
        League league = leagueRepository.findById(existing.getLeagueId())
                .orElseThrow(() -> new Exception("League not found"));

        // Verify league belongs to admin
        if (!league.getAdminId().equalsIgnoreCase(adminId)) {
            throw new Exception("You are not authorized to update matches for this league");
        }

        // ✅ Ensure both teams belong to league
        boolean team1Exists = league.getTeams().stream()
                .anyMatch(t -> t.startsWith(updatedMatch.getTeam1() + ":") || t.equalsIgnoreCase(updatedMatch.getTeam1()));
        boolean team2Exists = league.getTeams().stream()
                .anyMatch(t -> t.startsWith(updatedMatch.getTeam2() + ":") || t.equalsIgnoreCase(updatedMatch.getTeam2()));

         if (!team1Exists) {
            throw new Exception(updatedMatch.getTeam1() + " must belong to the league");
        }
        if (!team2Exists) {
            throw new Exception(updatedMatch.getTeam2() + " must belong to the league");
        }
        if (!team1Exists || !team2Exists) {
            throw new Exception("Both teams must belong to the league");
        }

        // Check that existing match teams are fixed
        if (!existing.getTeam1().equalsIgnoreCase(updatedMatch.getTeam1()) ||
                !existing.getTeam2().equalsIgnoreCase(updatedMatch.getTeam2())) {
            throw new Exception("You cannot change the teams of this match. Teams are fixed for matchId: " + id);
        }        
       
        // Update allowed fields only (no team changes!)
        if (updatedMatch.getScheduledDate() != null) {
            existing.setScheduledDate(updatedMatch.getScheduledDate());
        }
        if (updatedMatch.getVenue() != null) {
            existing.setVenue(updatedMatch.getVenue());
        }
        if (updatedMatch.getStatus() != null) {
            existing.setStatus(updatedMatch.getStatus());
        }
        if (updatedMatch.getMatchOvers() != null) {
            existing.setMatchOvers(updatedMatch.getMatchOvers());
        }

        return repo.save(existing);
    }

    // ================= DELETE MATCH =================
    public void deleteMatch(String token, String id) throws Exception {
        String adminId = validateToken(token);

        MatchSchedule match = repo.findById(id).orElseThrow(() -> new Exception("Match not found"));
        League league = leagueRepository.findById(match.getLeagueId())
                .orElseThrow(() -> new Exception("League not found"));

        if (!league.getAdminId().equalsIgnoreCase(adminId))
            throw new Exception("You are not authorized to delete matches for this league");

        repo.delete(match);
    }

    // ================= DELETE ALL MATCHES BY ADMIN =================
    public void deleteAllMatchesByAdmin(String token) throws Exception {
        String adminId = validateToken(token);

        List<League> adminLeagues = leagueRepository.findByAdminId(adminId);
        for (League league : adminLeagues) {
            List<MatchSchedule> matches = repo.findByLeagueId(league.getId());
            repo.deleteAll(matches);
        }
    }

    // ================= TOKEN VALIDATION =================
    private String validateToken(String token) throws Exception {
        if (token == null || !token.startsWith("Bearer "))
            throw new Exception("Authorization header missing or invalid");

        String jwt = token.substring(7);

        if (blacklistService.isBlacklisted(jwt))
            throw new Exception("Token is invalid or logged out. Please login again");

        return jwtUtil.extractEmail(jwt); // returns adminId/email
    }
}
