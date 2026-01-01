package com.cricriser.cricriser.team;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.cricriser.cricriser.cloudinary.CloudinaryService;
import com.cricriser.cricriser.league.League;
import com.cricriser.cricriser.league.LeagueRepository;
import com.cricriser.cricriser.player.Player;
import com.cricriser.cricriser.player.PlayerRepository;
import com.cricriser.cricriser.security.JwtBlacklistService;
import com.cricriser.cricriser.security.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class TeamService {

    @Autowired
    private TeamRepository teamRepository;
    @Autowired
    private LeagueRepository leagueRepository;
    @Autowired
    private PlayerRepository playerRepository;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private JwtBlacklistService blacklistService;
    @Autowired
    private CloudinaryService cloudinaryService;
    @Autowired
    private ObjectMapper objectMapper;

    // ================= CREATE TEAM =================
    public Team createTeam(String token, String teamJson, MultipartFile logoFile) throws Exception {

        String adminId = validateToken(token);
        Team team = objectMapper.readValue(teamJson, Team.class);

        League league = leagueRepository.findById(team.getLeagueId())
                .orElseThrow(() -> new Exception("League not found"));

        if (!league.getAdminId().equalsIgnoreCase(adminId)) {
            throw new Exception("Unauthorized");
        }

        if (teamRepository.existsByLeagueIdAndNameIgnoreCase(
                team.getLeagueId(), team.getName())) {
            throw new Exception("Team name already exists");
        }

        // 🔹 CHANGED
        validateTeam(team, league);

        if (logoFile != null && !logoFile.isEmpty()) {
            team.setLogoUrl(cloudinaryService.uploadFile(logoFile, "team_logos"));
        }

        Team savedTeam = teamRepository.save(team);

        league.getTeams().add(savedTeam.getId());
        leagueRepository.save(league);

        // 🔹 CHANGED
        assignTeamToPlayers(savedTeam, league);

        return savedTeam;
    }

    // ================= UPDATE =================
    public Team updateTeam(String token, String id, String teamJson, MultipartFile logoFile) throws Exception {

        String adminId = validateToken(token);

        Team existing = teamRepository.findById(id)
                .orElseThrow(() -> new Exception("Team not found"));

        League league = leagueRepository.findById(existing.getLeagueId())
                .orElseThrow(() -> new Exception("League not found"));

        if (!league.getAdminId().equalsIgnoreCase(adminId)) {
            throw new Exception("Unauthorized");
        }

        Team updated = objectMapper.readValue(teamJson, Team.class);

        // 🔹 use same league
        validateTeam(updated, league);

        existing.setName(updated.getName());
        existing.setCoach(updated.getCoach());
        existing.setCaptain(updated.getCaptain());
        existing.setViceCaptain(updated.getViceCaptain());
        existing.setSquadPlayerIds(updated.getSquadPlayerIds());

        if (logoFile != null && !logoFile.isEmpty()) {
            if (existing.getLogoUrl() != null) {
                cloudinaryService.deleteFile(existing.getLogoUrl());
            }
            existing.setLogoUrl(cloudinaryService.uploadFile(logoFile, "team_logos"));
        }

        Team saved = teamRepository.save(existing);

        // 🔹 correct call
        assignTeamToPlayers(saved, league);

        return saved;

    }

    // ================= DELETE =================
    public void deleteTeamById(String token, String id) throws Exception {

        String adminId = validateToken(token);

        Team team = teamRepository.findById(id)
                .orElseThrow(() -> new Exception("Team not found"));

        League league = leagueRepository.findById(team.getLeagueId())
                .orElseThrow(() -> new Exception("League not found"));

        if (!league.getAdminId().equalsIgnoreCase(adminId)) {
            throw new Exception("Unauthorized");
        }

        //  RELEASE ALL PLAYERS
        removeTeamFromPlayers(team);

        //  DELETE LOGO IF PRESENT
        if (team.getLogoUrl() != null && !team.getLogoUrl().isBlank()) {
            cloudinaryService.deleteFile(team.getLogoUrl());
        }

        //  REMOVE TEAM FROM LEAGUE
        league.getTeams().remove(team.getId());
        leagueRepository.save(league);

        //  DELETE TEAM DOCUMENT
        teamRepository.delete(team);
    }

    // ================= VALIDATION =================
    private void validateTeam(Team team, League league) throws Exception {

        if (team.getLeagueId() == null) {
            throw new Exception("League ID missing");
        }

        if (team.getSquadPlayerIds() == null || team.getSquadPlayerIds().size() < 11) {
            throw new Exception("Minimum 11 players required");
        }

        if (team.getCoach() == null || team.getCoach().isBlank()) {
            throw new Exception("Coach required");
        }

        if (team.getCaptain() == null || team.getViceCaptain() == null) {
            throw new Exception("Captain & Vice Captain required");
        }

        if (team.getCaptain().equals(team.getViceCaptain())) {
            throw new Exception("Captain and Vice Captain cannot be same");
        }

        long distinct = team.getSquadPlayerIds().stream().distinct().count();
        if (distinct != team.getSquadPlayerIds().size()) {
            throw new Exception("Duplicate players in squad");
        }

        // 🔹 CHANGED
        validatePlayers(team, league);
    }

    // ================= PLAYER VALIDATION =================
    private void validatePlayers(Team team, League league) {

        // convert league dates once
        LocalDateTime leagueStart
                = league.getStartDate().toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDateTime();

        LocalDateTime leagueEnd
                = league.getEndDate().toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDateTime();

        for (String playerId : team.getSquadPlayerIds()) {

            Player player = playerRepository.findById(playerId)
                    .orElseThrow(() -> new RuntimeException("Invalid player ID: " + playerId));

            if (player.getActiveLeagueId() != null) {

                boolean overlap
                        = !(leagueEnd.isBefore(player.getLeagueStartDate())
                        || leagueStart.isAfter(player.getLeagueEndDate()));

                if (overlap) {
                    throw new RuntimeException(
                            player.getName() + " is already playing in another league during this period");
                }
            }
        }

        if (!team.getSquadPlayerIds().contains(team.getCaptain())
                || !team.getSquadPlayerIds().contains(team.getViceCaptain())) {
            throw new RuntimeException("Captain & Vice must be in squad");
        }
    }

    // ================= PLAYER LINK =================
    private void assignTeamToPlayers(Team team, League league) {

        // 🔹 Convert Date → LocalDateTime ONCE
        LocalDateTime leagueStart
                = league.getStartDate().toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDateTime();

        LocalDateTime leagueEnd
                = league.getEndDate().toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDateTime();

        for (String pid : team.getSquadPlayerIds()) {

            Player p = playerRepository.findById(pid)
                    .orElseThrow(() -> new RuntimeException("Invalid Player ID: " + pid));

            p.setCurrentTeamId(team.getId());
            p.setActiveLeagueId(league.getId());
            p.setLeagueStartDate(leagueStart); // ✅ LocalDateTime
            p.setLeagueEndDate(leagueEnd);     // ✅ LocalDateTime

            playerRepository.save(p);
        }
    }

    private void removeTeamFromPlayers(Team team) {

        for (String pid : team.getSquadPlayerIds()) {
            Player p = playerRepository.findById(pid).orElse(null);
            if (p != null) {
                p.setCurrentTeamId(null);
                playerRepository.save(p);
            }
        }
    }

    public void deleteAllTeamsByAdmin(String token) throws Exception {

        String adminId = validateToken(token);

        List<League> leagues = leagueRepository.findByAdminId(adminId);

        for (League league : leagues) {

            List<Team> teams = teamRepository.findByLeagueId(league.getId());

            for (Team team : teams) {

                //  RELEASE PLAYERS
                removeTeamFromPlayers(team);

                //  DELETE LOGO
                if (team.getLogoUrl() != null && !team.getLogoUrl().isBlank()) {
                    cloudinaryService.deleteFile(team.getLogoUrl());
                }

                //  DELETE TEAM
                teamRepository.delete(team);
            }

            //  CLEAR TEAM REFERENCES FROM LEAGUE
            league.getTeams().clear();
            leagueRepository.save(league);
        }
    }

    // ================= GET =================
    public Team getTeamById(String id) {
        return teamRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Team not found"));
    }

    public List<Team> getAllTeams() {
        return teamRepository.findAll();
    }

    // ================= TOKEN =================
    private String validateToken(String token) throws Exception {

        if (token == null || !token.startsWith("Bearer ")) {
            throw new Exception("Missing token");
        }

        String jwt = token.substring(7);

        if (blacklistService.isBlacklisted(jwt)) {
            throw new Exception("Session expired");
        }

        return jwtUtil.extractEmail(jwt);
    }
}
