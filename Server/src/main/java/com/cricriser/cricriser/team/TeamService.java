package com.cricriser.cricriser.team;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.cricriser.cricriser.cloudinary.CloudinaryService;
import com.cricriser.cricriser.league.League;
import com.cricriser.cricriser.league.LeagueRepository;
import com.cricriser.cricriser.model.Player;
import com.cricriser.cricriser.security.JwtBlacklistService;
import com.cricriser.cricriser.security.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class TeamService {

    @Autowired
    TeamRepository teamRepository;
    @Autowired
    private LeagueRepository leagueRepository;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private JwtBlacklistService blacklistService;
    @Autowired
    private CloudinaryService cloudinaryService;
    @Autowired
    private ObjectMapper objectMapper;

    // ======= CREATE TEAM =======
    public Team createTeam(String token, String teamJson, MultipartFile logoFile) throws Exception {
        String adminId = validateToken(token);
        Team team = objectMapper.readValue(teamJson, Team.class);

        League league = leagueRepository.findById(team.getLeagueId())
                .orElseThrow(() -> new Exception("League not found!"));

        if (!league.getAdminId().equalsIgnoreCase(adminId))
            throw new Exception("You are not authorized to add team to this league!");

        // Duplicate team name inside league
        List<Team> existingTeams = teamRepository.findByLeagueId(team.getLeagueId());
        for (Team t : existingTeams) {
            if (t.getName().equalsIgnoreCase(team.getName())) {
                throw new Exception("Team name already exists in this league!");
            }
        }

        assignPlayerIds(team);
        validateTeam(team, null);

        // Upload logo
        if (logoFile != null && !logoFile.isEmpty()) {
            String logoUrl = cloudinaryService.uploadFile(logoFile, "team_logos");
            team.setLogoUrl(logoUrl);
        }

        // FIRST save team
        Team savedTeam = teamRepository.save(team);

        // SECOND save team ID to league (IMPORTANT FIX)
        league.getTeams().add(savedTeam.getId());
        leagueRepository.save(league);

        return savedTeam;
    }

    // ======= UPDATE TEAM =======
    public Team updateTeam(String token, String id, String teamJson, MultipartFile logoFile) throws Exception {
        String adminId = validateToken(token);

        Team existingTeam = teamRepository.findById(id)
                .orElseThrow(() -> new Exception("Team not found"));

        Optional<League> leagueOpt = leagueRepository.findById(existingTeam.getLeagueId());
        if (leagueOpt.isEmpty())
            throw new Exception("League not found!");
        if (!leagueOpt.get().getAdminId().equalsIgnoreCase(adminId))
            throw new Exception("You are not authorized to update this team!");

        Team team = objectMapper.readValue(teamJson, Team.class);

        // Duplicate name check (exclude same team)
        List<Team> teamsInLeague = teamRepository.findByLeagueId(existingTeam.getLeagueId());
        for (Team t : teamsInLeague) {
            if (!t.getId().equalsIgnoreCase(existingTeam.getId()) &&
                    t.getName().equalsIgnoreCase(team.getName())) {
                throw new Exception("Another team in this league already has the same name!");
            }
        }

        assignPlayerIds(team);
        validateTeam(team, existingTeam);

        // Update fields
        existingTeam.setName(team.getName());
        existingTeam.setCoach(team.getCoach());
        existingTeam.setSquad(team.getSquad());
        existingTeam.setCaptain(team.getCaptain());
        existingTeam.setViceCaptain(team.getViceCaptain());

        // Update league team list name (if name changed)
        League league = leagueOpt.get();
        league.getTeams().remove(existingTeam.getId());
        league.getTeams().add(existingTeam.getId());

        leagueRepository.save(league);

        // Logo update
        if (logoFile != null && !logoFile.isEmpty()) {
            List<Team> allTeams = teamRepository.findAll();
            for (Team t : allTeams) {
                if (!t.getId().equalsIgnoreCase(existingTeam.getId()) &&
                        t.getLogoUrl() != null &&
                        t.getLogoUrl().contains(logoFile.getOriginalFilename())) {
                    throw new Exception("This logo is already assigned to another team!");
                }
            }

            if (existingTeam.getLogoUrl() != null && !existingTeam.getLogoUrl().isEmpty()) {
                cloudinaryService.deleteFile(existingTeam.getLogoUrl());
            }

            String logoUrl = cloudinaryService.uploadFile(logoFile, "team_logos");
            existingTeam.setLogoUrl(logoUrl);
        }

        return teamRepository.save(existingTeam);
    }

    // ======= DELETE TEAM BY ID =======
    public void deleteTeamById(String token, String id) throws Exception {
        String adminId = validateToken(token);

        Team team = teamRepository.findById(id)
                .orElseThrow(() -> new Exception("Team not found"));

        League league = leagueRepository.findById(team.getLeagueId())
                .orElseThrow(() -> new Exception("League not found!"));

        if (!league.getAdminId().equalsIgnoreCase(adminId))
            throw new Exception("You are not authorized to delete this team!");

        // Delete logo from Cloudinary if exists
        if (team.getLogoUrl() != null && !team.getLogoUrl().isEmpty()) {
            cloudinaryService.deleteFile(team.getLogoUrl());
        }

        // Remove team from league's teams array safely
        List<String> leagueTeams = league.getTeams();
        if (leagueTeams != null) {
            leagueTeams.removeIf(t -> {
                String[] parts = t.split(":");
                return parts.length > 1 && parts[1].equalsIgnoreCase(team.getId());
            });
            league.setTeams(leagueTeams);
            leagueRepository.save(league);
        }

        // Delete team from repository
        teamRepository.delete(team);
    }

    // ======= DELETE ALL TEAMS FOR ADMIN =======
    public void deleteAllTeamsByAdmin(String token) throws Exception {
        String adminId = validateToken(token);

        List<League> adminLeagues = leagueRepository.findByAdminId(adminId);

        for (League league : adminLeagues) {
            List<Team> teams = teamRepository.findByLeagueId(league.getId());

            // Delete each team's logo safely
            for (Team team : teams) {
                if (team.getLogoUrl() != null && !team.getLogoUrl().isEmpty()) {
                    cloudinaryService.deleteFile(team.getLogoUrl());
                }
            }

            // Clear league's teams array safely
            if (league.getTeams() != null) {
                league.setTeams(List.of());
                leagueRepository.save(league);
            }

            // Delete all teams from repository
            if (teams != null && !teams.isEmpty()) {
                teamRepository.deleteAll(teams);
            }
        }
    }

    // ======= GET TEAM =======
    public Team getTeamById(String id) throws Exception {
        return teamRepository.findById(id)
                .orElseThrow(() -> new Exception("Team not found"));
    }

    public Team getTeamByName(String name) throws Exception {
        Team team = teamRepository.findByName(name);
        if (team == null)
            throw new Exception("Team not found");
        return team;
    }

    public List<Team> getAllTeams() {
        return teamRepository.findAll();
    }

    // ======= VALIDATION LOGIC =======
    private void validateTeam(Team team, Team existingTeam) throws Exception {

        // ========== CHECK DUPLICATE TEAM NAME ==========
        Team duplicate = teamRepository.findByName(team.getName());
        if (duplicate != null && (existingTeam == null || !duplicate.getId().equalsIgnoreCase(existingTeam.getId())))
            throw new Exception("Another team with this name already exists!");

        // ========== CHECK VALID LEAGUE ==========
        if (team.getLeagueId() == null || leagueRepository.findById(team.getLeagueId()).isEmpty())
            throw new Exception("Team must belong to a valid league!");

        // ========== CHECK SQUAD SIZE ==========
        if (team.getSquad() == null || team.getSquad().size() < 15)
            throw new Exception("Squad must have at least 15 players!");

        // ========== CHECK DUPLICATE PLAYER NAMES ==========
        List<String> names = team.getSquad().stream()
                .map(p -> p.getName().trim().toLowerCase())
                .toList();

        long uniqueCount = names.stream().distinct().count();

        if (uniqueCount != names.size()) {
            throw new Exception("Duplicate player names found! Each player name must be unique in the squad.");
        }

        // ========== COACH SHOULD NOT BE IN SQUAD ==========
        boolean coachInSquad = team.getSquad().stream()
                .anyMatch(p -> p.getName().trim().equalsIgnoreCase(team.getCoach().trim()));
        if (coachInSquad)
            throw new Exception("Coach cannot be part of the squad!");

        // ========== CAPTAIN / VICE CAPTAIN VALIDATION ==========
        if (team.getCaptain() == null || team.getViceCaptain() == null)
            throw new Exception("Captain and Vice-Captain must be assigned!");

        boolean captainInSquad = team.getSquad().stream()
                .anyMatch(p -> p.getName().trim().equalsIgnoreCase(team.getCaptain().trim()));
        boolean viceCaptainInSquad = team.getSquad().stream()
                .anyMatch(p -> p.getName().trim().equalsIgnoreCase(team.getViceCaptain().trim()));

        if (!captainInSquad)
            throw new Exception("Captain must be part of the squad!");
        if (!viceCaptainInSquad)
            throw new Exception("Vice-Captain must be part of the squad!");
        if (team.getCaptain().trim().equalsIgnoreCase(team.getViceCaptain().trim()))
            throw new Exception("Captain and Vice-Captain must be different!");
    }

    // ======= ASSIGN PLAYER IDS =======
    private void assignPlayerIds(Team team) {
        for (Player p : team.getSquad()) {
            if (p.getId() == null || p.getId().isEmpty()) {
                p.setId(UUID.randomUUID().toString());
            }
        }
    }

    // ======= VALIDATE TOKEN =======
    private String validateToken(String token) throws Exception {
        if (token == null || !token.startsWith("Bearer "))
            throw new Exception("Authorization header missing or invalid!");

        String jwt = token.substring(7);
        if (blacklistService.isBlacklisted(jwt))
            throw new Exception("Token is invalid or logged out. Please login again!");

        return jwtUtil.extractEmail(jwt); // adminId/email
    }
}
