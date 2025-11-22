package com.cricriser.cricriser.league;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.cricriser.cricriser.cloudinary.CloudinaryService;
import com.cricriser.cricriser.match.MatchSchedule;
import com.cricriser.cricriser.match.MatchScheduleRepository;
import com.cricriser.cricriser.match.MatchScoreRepository;
import com.cricriser.cricriser.points.PointsTableRepository;
import com.cricriser.cricriser.team.Team;
import com.cricriser.cricriser.team.TeamRepository;

@Service
public class LeagueService {

    @Autowired
    private LeagueRepository leagueRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private MatchScheduleRepository matchScheduleRepository;

    @Autowired
    private MatchScoreRepository matchScoreRepository;

    @Autowired
    private CloudinaryService cloudinaryService;

    @Autowired
    private PointsTableRepository pointsTableRepository;

    private String getLoggedInAdminId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null
                || auth.getPrincipal().toString().equalsIgnoreCase("anonymousUser")) {
            throw new RuntimeException("Unauthorized! Please log in.");
        }
        return auth.getPrincipal().toString();
    }

    public List<MatchSchedule> createLeagueAndScheduleMatches(
            League league, MultipartFile logoFile,
            boolean includeEliminator, boolean includeKnockouts) {

        String adminId = getLoggedInAdminId();

        if (leagueRepository.existsByName(league.getName())) {
            throw new RuntimeException("League name already exists!");
        }

        if (league.getTeams() == null || league.getTeams().isEmpty()) {
            throw new RuntimeException("Teams cannot be null or empty!");
        }

        if (league.getNoOfTeams() != league.getTeams().size()) {
            throw new RuntimeException("noOfTeams must match the size of teams list!");
        }

        league.setAdminId(adminId);

        if (logoFile != null && !logoFile.isEmpty()) {
            try {
                String logoUrl = cloudinaryService.uploadFile(logoFile, "leagues");
                league.setLogoUrl(logoUrl);
            } catch (IOException e) {
                throw new RuntimeException("League logo upload failed: " + e.getMessage());
            }
        }

        League savedLeague = leagueRepository.save(league);

        List<MatchSchedule> matches;
        String formatType = league.getLeagueFormatType();
        switch (formatType) {
            case "SINGLE_ROUND_ROBIN":
                matches = generateRoundRobinMatches(savedLeague, false);
                break;
            case "DOUBLE_ROUND_ROBIN":
                matches = generateRoundRobinMatches(savedLeague, true);
                break;
            case "GROUP":
                matches = generateGroupFormatMatches(savedLeague);
                break;
            default:
                throw new RuntimeException(
                        "Invalid leagueFormatType! Choose SINGLE ROUND ROBIN, DOUBLE ROUND ROBIN, or GROUP.");
        }

        if (includeKnockouts) {
            matches.addAll(generateKnockoutMatches(savedLeague, includeEliminator));
        }

        assignMatchDates(savedLeague, matches);
        assignMatchNumbersAndTypes(matches);
        matchScheduleRepository.saveAll(matches);

        savedLeague.setNoOfMatches(matches.size());
        leagueRepository.save(savedLeague);

        return matches;
    }

    public League updateLeague(String leagueId, League updatedLeague, MultipartFile logoFile) throws Exception {

        String adminId = getLoggedInAdminId();

        League existingLeague = leagueRepository.findById(leagueId)
                .orElseThrow(() -> new RuntimeException("League not found"));

        if (!existingLeague.getAdminId().equalsIgnoreCase(adminId)) {
            throw new RuntimeException("You are not authorized to update this league!");
        }

        // ---------- UPDATE FIELDS ----------
        if (updatedLeague.getName() != null && !updatedLeague.getName().isEmpty()) {
            leagueRepository.findByName(updatedLeague.getName())
                    .filter(l -> !l.getId().equalsIgnoreCase(leagueId))
                    .ifPresent(l -> {
                        throw new RuntimeException("Another league with this name already exists!");
                    });
            existingLeague.setName(updatedLeague.getName());
        }

        if (updatedLeague.getNoOfTeams() > 0)
            existingLeague.setNoOfTeams(updatedLeague.getNoOfTeams());

        if (updatedLeague.getTeams() != null && !updatedLeague.getTeams().isEmpty())
            existingLeague.setTeams(updatedLeague.getTeams());

        if (updatedLeague.getNoOfMatches() > 0)
            existingLeague.setNoOfMatches(updatedLeague.getNoOfMatches());

        if (updatedLeague.getStartDate() != null)
            existingLeague.setStartDate(updatedLeague.getStartDate());

        if (updatedLeague.getEndDate() != null)
            existingLeague.setEndDate(updatedLeague.getEndDate());

        if (updatedLeague.getVenue() != null && !updatedLeague.getVenue().isEmpty())
            existingLeague.setVenue(updatedLeague.getVenue());

        if (updatedLeague.getLeagueFormat() != null && !updatedLeague.getLeagueFormat().isEmpty())
            existingLeague.setLeagueFormat(updatedLeague.getLeagueFormat());

        if (updatedLeague.getUmpires() != null && !updatedLeague.getUmpires().isEmpty())
            existingLeague.setUmpires(updatedLeague.getUmpires());

        // ---------- UPDATE LOGO ----------
        if (logoFile != null && !logoFile.isEmpty()) {
            // Delete old logo
            if (existingLeague.getLogoUrl() != null && !existingLeague.getLogoUrl().isEmpty()) {
                cloudinaryService.deleteFile(existingLeague.getLogoUrl());
            }

            String logoUrl = cloudinaryService.uploadFile(logoFile, "leagues");
            existingLeague.setLogoUrl(logoUrl);
        }

        return leagueRepository.save(existingLeague);
    }

    private List<MatchSchedule> generateRoundRobinMatches(League league, boolean doubleRound) {

        List<String> teams = league.getTeams().stream()
                .toList();

        int n = teams.size();

        // If odd add a dummy
        if (n % 2 != 0) {
            teams = new ArrayList<>(teams);
            teams.add("BYE");
            n++;
        }

        List<String> rotating = new ArrayList<>(teams);
        List<MatchSchedule> matches = new ArrayList<>();

        int totalRounds = n - 1;
        int matchesPerRound = n / 2;

        for (int round = 0; round < totalRounds; round++) {

            for (int i = 0; i < matchesPerRound; i++) {

                String team1 = rotating.get(i);
                String team2 = rotating.get(n - 1 - i);

                if (!team1.equalsIgnoreCase("BYE") && !team2.equalsIgnoreCase("BYE")) {

                    // NORMAL MATCH
                    MatchSchedule m = new MatchSchedule();
                    m.setLeagueId(league.getId());
                    m.setTeam1(team1);
                    m.setTeam2(team2);
                    m.setStatus("Scheduled");
                    m.setMatchType("LEAGUE");
                    matches.add(m);

                    // REVERSE FIXTURE - only for double RR
                    if (doubleRound) {
                        MatchSchedule r = new MatchSchedule();
                        r.setLeagueId(league.getId());
                        r.setTeam1(team2);
                        r.setTeam2(team1);
                        r.setStatus("Scheduled");
                        r.setMatchType("LEAGUE");
                        matches.add(r);
                    }
                }
            }

            // ---- CORRECT ROTATION ----
            List<String> next = new ArrayList<>(rotating);

            // Move last team to index 1 (circle rotation)
            String last = next.remove(next.size() - 1);
            next.add(1, last);

            rotating = next;
        }

        return matches;
    }

    private List<MatchSchedule> generateGroupFormatMatches(League league) {
        List<String> teams = league.getTeams();

        List<String> groupA = new ArrayList<>();
        List<String> groupB = new ArrayList<>();
        for (int i = 0; i < teams.size(); i++) {
            if (i % 2 == 0)
                groupA.add(teams.get(i));
            else
                groupB.add(teams.get(i));
        }

        List<MatchSchedule> matches = new ArrayList<>();
        matches.addAll(generateMatchesWithinGroup(league, groupA));
        matches.addAll(generateMatchesWithinGroup(league, groupB));

        return matches;
    }

    private List<MatchSchedule> generateMatchesWithinGroup(League league, List<String> group) {
        List<MatchSchedule> matches = new ArrayList<>();
        for (int i = 0; i < group.size(); i++) {
            for (int j = i + 1; j < group.size(); j++) {
                MatchSchedule match = new MatchSchedule();
                match.setLeagueId(league.getId());
                match.setTeam1(group.get(i));
                match.setTeam2(group.get(j));
                match.setStatus("Scheduled");
                matches.add(match);
            }
        }
        return matches;
    }

    private List<MatchSchedule> generateKnockoutMatches(League league, boolean includeEliminator) {
        List<MatchSchedule> knockouts = new ArrayList<>();
        
        if (includeEliminator) {
            MatchSchedule eliminator = new MatchSchedule();
            eliminator.setLeagueId(league.getId());
            eliminator.setTeam1("Loser3");
            eliminator.setTeam2("Loser4");
            eliminator.setStatus("Scheduled");
            eliminator.setVenue(league.getVenue());
            eliminator.setMatchType("ELIMINATOR");
            knockouts.add(eliminator);
        }

        MatchSchedule semi1 = new MatchSchedule();
        semi1.setLeagueId(league.getId());
        semi1.setTeam1("Winner1");
        semi1.setTeam2("Winner4");
        semi1.setStatus("Scheduled");
        semi1.setVenue(league.getVenue());
        semi1.setMatchType("SEMI FINAL 1");
        knockouts.add(semi1);

        MatchSchedule semi2 = new MatchSchedule();
        semi2.setLeagueId(league.getId());
        semi2.setTeam1("Winner2");
        semi2.setTeam2("Winner3");
        semi2.setStatus("Scheduled");
        semi2.setVenue(league.getVenue());
        semi2.setMatchType("SEMI FINAL 2");
        knockouts.add(semi2);

        MatchSchedule finalMatch = new MatchSchedule();
        finalMatch.setLeagueId(league.getId());
        finalMatch.setTeam1("WinnerSemi1");
        finalMatch.setTeam2("WinnerSemi2");
        finalMatch.setStatus("Scheduled");
        finalMatch.setVenue(league.getVenue());
        finalMatch.setMatchType("FINAL");
        knockouts.add(finalMatch);

        return knockouts;
    }

    private void assignMatchDates(League league, List<MatchSchedule> matches) {
        LocalDateTime start = LocalDateTime.ofInstant(league.getStartDate().toInstant(), ZoneId.systemDefault());
        LocalDateTime end = LocalDateTime.ofInstant(league.getEndDate().toInstant(), ZoneId.systemDefault());

        long totalDays = ChronoUnit.DAYS.between(start, end) + 1;
        long interval = Math.max(totalDays / matches.size(), 1);

        for (int i = 0; i < matches.size(); i++) {
            LocalDateTime matchDate = start.plusDays(i * interval)
                    .withHour(10)
                    .withMinute(0)
                    .withSecond(0)
                    .withNano(0);

            matches.get(i).setScheduledDate(Date.from(matchDate.atZone(ZoneId.systemDefault()).toInstant()));
        }
    }

    private void assignMatchNumbersAndTypes(
        List<MatchSchedule> matches) {

    // 🌟 1. FIX MATCH TYPE FIRST (LEAGUE vs KNOCKOUT)
    for (MatchSchedule match : matches) {

        if (match.getMatchType() == null) {
            match.setMatchType("LEAGUE");
        }

        if (match.getMatchType().equalsIgnoreCase("LEAGUE")) continue;

        // Knockout matches
        if (match.getTeam1().startsWith("Loser") || match.getTeam2().startsWith("Loser")) {
            match.setMatchType("ELIMINATOR");
        } else if (match.getMatchType().contains("SEMI")) {
            match.setMatchType(match.getMatchType().toUpperCase());
        } else if (match.getTeam1().startsWith("WinnerSemi") && match.getTeam2().startsWith("WinnerSemi")) {
            match.setMatchType("FINAL");
        }
    }

    // 🌟 2. SORT MATCHES IN CORRECT ORDER
    // LEAGUE FIRST → ELIMINATOR → SEMI FINAL 1 → SEMI FINAL 2 → FINAL
    matches.sort(Comparator.comparing((MatchSchedule m) -> {
        switch (m.getMatchType().toUpperCase()) {
            case "LEAGUE":
                return 1;
            case "ELIMINATOR":
                return 2;
            case "SEMI FINAL 1":
            case "SEMI_FINAL_1":
                return 3;
            case "SEMI FINAL 2":
            case "SEMI_FINAL_2":
                return 4;
            case "FINAL":
                return 5;
            default:
                return 99;
        }
    }));

    // 🌟 3. ASSIGN MATCH NUMBERS IN ORDER
    int matchNo = 1;
    for (MatchSchedule match : matches) {
        match.setMatchNo(matchNo++);
    }
}

    public void deleteLeague(String leagueId) {
        String adminId = getLoggedInAdminId();

        League league = leagueRepository.findById(leagueId)
                .orElseThrow(() -> new RuntimeException("League not found with ID: " + leagueId));

        if (!league.getAdminId().equalsIgnoreCase(adminId)) {
            throw new RuntimeException("This league does not belong to you!");
        }

        // 1. Delete teams
        List<Team> teams = new ArrayList<>();

        for (String t : league.getTeams()) {
            String[] parts = t.split(":");
            String teamName = parts[0];
            String teamId = parts.length > 1 ? parts[1] : null;

            Team team = null;

            if (teamId != null)
                team = teamRepository.findById(teamId).orElse(null);

            if (team == null)
                team = teamRepository.findByName(teamName);

            if (team != null) {
                if (team.getLogoUrl() != null) {
                    try {
                        cloudinaryService.deleteFile(team.getLogoUrl());
                    } catch (IOException ignored) {
                    }
                }
                teams.add(team);
            }
        }

        teamRepository.deleteAll(teams);

        // 2. Delete matches
        matchScheduleRepository.deleteByLeagueId(leagueId);
        matchScoreRepository.deleteByLeagueId(leagueId);

        // 3. Delete league logo
        if (league.getLogoUrl() != null) {
            try {
                cloudinaryService.deleteFile(league.getLogoUrl());
            } catch (IOException ignored) {
            }
        }

        // 4. Clear league teams list
        league.setTeams(new ArrayList<>());
        leagueRepository.save(league);

        // 5. Delete league
        leagueRepository.deleteById(leagueId);
    }

    public void deleteAllLeagues() {
        String adminId = getLoggedInAdminId();

        List<League> leagues = leagueRepository.findByAdminId(adminId);
        if (leagues.isEmpty()) {
            throw new RuntimeException("No leagues found to delete!");
        }

        for (League league : leagues) {

            String leagueId = league.getId();

            // ==================== 1. DELETE TEAMS ====================
            List<String> teamIds = league.getTeams().stream()
                    .filter(id -> id != null)
                    .toList();

            if (!teamIds.isEmpty()) {
                List<Team> teams = teamRepository.findAllById(teamIds);

                // Delete team logos from cloudinary
                for (Team team : teams) {
                    if (team.getLogoUrl() != null && !team.getLogoUrl().isEmpty()) {
                        try {
                            cloudinaryService.deleteFile(team.getLogoUrl());
                        } catch (IOException e) {
                            System.err.println("Team logo delete failed: " + e.getMessage());
                        }
                    }
                }

                teamRepository.deleteAll(teams);
            }

            // ==================== 2. DELETE MATCH SCHEDULES ====================
            try {
                matchScheduleRepository.deleteByLeagueId(leagueId);
            } catch (Exception e) {
                System.err.println("Match schedule delete failed: " + e.getMessage());
            }

            // ==================== 3. DELETE MATCH SCORES ====================
            try {
                matchScoreRepository.deleteByLeagueId(leagueId);
            } catch (Exception e) {
                System.err.println("Match score delete failed: " + e.getMessage());
            }

            // ==================== 4. DELETE POINTS TABLE ====================
            try {
                pointsTableRepository.deleteByLeagueId(leagueId);
            } catch (Exception e) {
                System.err.println("Points table delete failed: " + e.getMessage());
            }

            // ==================== 5. DELETE LEAGUE LOGO ====================
            if (league.getLogoUrl() != null && !league.getLogoUrl().isEmpty()) {
                try {
                    cloudinaryService.deleteFile(league.getLogoUrl());
                } catch (IOException e) {
                    e.getMessage();
                }
            }

            // ==================== 6. DELETE LEAGUE ITSELF ====================
            leagueRepository.deleteById(leagueId);
        }
    }

    public Optional<League> getLeagueById(String leagueId) {
        getLoggedInAdminId();
        return leagueRepository.findById(leagueId);
    }

    public List<League> getAllLeagues() {
        getLoggedInAdminId();
        return leagueRepository.findAll();
    }

    public Optional<League> getLeagueByName(String name) {
        getLoggedInAdminId();
        return leagueRepository.findByName(name);
    }

    public List<League> getLeaguesByAdmin() {
        String adminId = getLoggedInAdminId();
        return leagueRepository.findByAdminId(adminId);
    }
}