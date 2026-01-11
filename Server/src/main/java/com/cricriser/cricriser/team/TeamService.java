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
import com.cricriser.cricriser.player.PlayerCardDto;
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
    public Team createTeam(String token, String leagueId, String teamJson, MultipartFile logoFile) throws Exception {

        String adminId = validateToken(token);
        Team team = objectMapper.readValue(teamJson, Team.class);

        // FORCE league from path
        team.setLeagueId(leagueId);

        League league = leagueRepository.findById(leagueId)
                .orElseThrow(() -> new Exception("League not found"));

        if (!league.getAdminId().equalsIgnoreCase(adminId)) {
            throw new Exception("Unauthorized");
        }

        if (teamRepository.existsByLeagueIdAndNameIgnoreCase(leagueId, team.getName())) {
            throw new Exception("Team already exists in this league");
        }

        validateTeam(team, league);

        if (logoFile != null && !logoFile.isEmpty()) {
            team.setLogoUrl(cloudinaryService.uploadFile(logoFile, "team_logos"));
        }

        Team savedTeam = teamRepository.save(team);

        league.getTeams().add(savedTeam.getId());
        leagueRepository.save(league);

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
    private void validateTeam(Team team, League league) {

        if (team.getSquadPlayerIds() == null) {
            throw new RuntimeException("Squad is required");
        }

        int size = team.getSquadPlayerIds().size();
        if (size < 15 || size > 18) {
            throw new RuntimeException("Squad must have 15–18 players");
        }

        if (team.getCoach() == null || team.getCoach().isBlank()) {
            throw new RuntimeException("Coach required");
        }

        if (team.getCaptain() == null || team.getViceCaptain() == null) {
            throw new RuntimeException("Captain & Vice Captain required");
        }

        if (team.getCaptain().equals(team.getViceCaptain())) {
            throw new RuntimeException("Captain and Vice Captain cannot be same");
        }

        long distinct = team.getSquadPlayerIds().stream().distinct().count();
        if (distinct != team.getSquadPlayerIds().size()) {
            throw new RuntimeException("Duplicate players in squad");
        }

        validatePlayers(team, league);
    }

    // ================= PLAYER VALIDATION =================
    private void validatePlayers(Team team, League league) {

        LocalDateTime leagueStart = league.getStartDate().toInstant()
                .atZone(ZoneId.systemDefault()).toLocalDateTime();

        LocalDateTime leagueEnd = league.getEndDate().toInstant()
                .atZone(ZoneId.systemDefault()).toLocalDateTime();

        for (String pid : team.getSquadPlayerIds()) {

            Player p = playerRepository.findById(pid)
                    .orElseThrow(() -> new RuntimeException("Invalid player ID: " + pid));

            if (p.getActiveLeagueId() != null) {
                boolean overlap = !(leagueEnd.isBefore(p.getLeagueStartDate())
                        || leagueStart.isAfter(p.getLeagueEndDate()));

                if (overlap) {
                    throw new RuntimeException(p.getName()
                            + " already plays in another league during this time");
                }
            }
        }

        if (!team.getSquadPlayerIds().contains(team.getCaptain())
                || !team.getSquadPlayerIds().contains(team.getViceCaptain())) {
            throw new RuntimeException("Captain & Vice Captain must be in squad");
        }
    }    // ================= PLAYER LINK =================

    private void assignTeamToPlayers(Team team, League league) {

        LocalDateTime leagueStart = league.getStartDate().toInstant()
                .atZone(ZoneId.systemDefault()).toLocalDateTime();

        LocalDateTime leagueEnd = league.getEndDate().toInstant()
                .atZone(ZoneId.systemDefault()).toLocalDateTime();

        for (String pid : team.getSquadPlayerIds()) {

            Player p = playerRepository.findById(pid)
                    .orElseThrow(() -> new RuntimeException("Invalid Player ID"));

            p.setCurrentTeamId(team.getId());
            p.setActiveLeagueId(league.getId());
            p.setLeagueStartDate(leagueStart);
            p.setLeagueEndDate(leagueEnd);

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

    public TeamDetailsResponse getTeamByName(String name) {

        Team team = teamRepository.findByNameIgnoreCase(name)
                .orElseThrow(() -> new RuntimeException("Team not found"));

        League league = leagueRepository.findById(team.getLeagueId())
                .orElseThrow(() -> new RuntimeException("League not found"));

        Player captain = playerRepository.findById(team.getCaptain())
                .orElseThrow(() -> new RuntimeException("Captain not found"));

        Player viceCaptain = playerRepository.findById(team.getViceCaptain())
                .orElseThrow(() -> new RuntimeException("Vice captain not found"));

        List<PlayerCardDto> players = team.getSquadPlayerIds()
                .stream()
                .map(pid -> {
                    Player p = playerRepository.findById(pid)
                            .orElseThrow(() -> new RuntimeException("Player not found"));
                    return new PlayerCardDto(
                            p.getId(),
                            p.getName(),
                            p.getRole(),
                            p.getPhotoUrl()
                    );
                })
                .toList();

        return new TeamDetailsResponse(
                team.getId(),
                team.getName(),
                league.getName(),
                team.getCoach(),
                captain.getName(),
                viceCaptain.getName(),
                players
        );
    }

    public List<Team> getTeamsByLeague(String leagueId) {
        return teamRepository.findByLeagueId(leagueId);
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
