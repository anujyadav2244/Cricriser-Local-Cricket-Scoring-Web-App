package com.cricriser.cricriser.league;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.cricriser.cricriser.cloudinary.CloudinaryService;
import com.cricriser.cricriser.match.matchscheduling.MatchSchedule;
import com.cricriser.cricriser.match.matchscheduling.MatchScheduleRepository;
import com.cricriser.cricriser.match.matchscoring.MatchScoreRepository;
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

// ============================= CREATE LEAGUE / SERIES =============================
    public List<MatchSchedule> createLeagueAndScheduleMatches(
            League league, MultipartFile logoFile,
            boolean includeEliminator, boolean includeKnockouts) {

        String adminId = getLoggedInAdminId();

        if (leagueRepository.existsByName(league.getName())) {
            throw new RuntimeException("League name already exists!");
        }

        if (league.getTeams() == null || league.getTeams().size() < 2) {
            throw new RuntimeException("Minimum 2 teams required!");
        }

        league.setAdminId(adminId);

        //  Validate format, type, overs, testDays
        validateLeagueStructure(league);

        //  Upload Logo
        if (logoFile != null && !logoFile.isEmpty()) {
            try {
                league.setLogoUrl(cloudinaryService.uploadFile(logoFile, "leagues"));
            } catch (IOException e) {
                throw new RuntimeException("Logo upload failed");
            }
        }

        League savedLeague = leagueRepository.save(league);

        List<MatchSchedule> matches;

        // ===================== BILATERAL =====================
        if (league.getLeagueType().equalsIgnoreCase("BILATERAL")) {
            matches = generateBilateralMatches(savedLeague);
        } // ===================== TOURNAMENT =====================
        else {
            matches = generateTournamentMatches(
                    savedLeague, includeEliminator, includeKnockouts);
        }

        //  Validate date range
        validateDateRange(savedLeague, matches.size());

        //  Assign schedule
        assignMatchDates(savedLeague, matches);

        //  Assign numbers
        assignMatchNumbers(matches);

        matchScheduleRepository.saveAll(matches);

        savedLeague.setNoOfMatches(matches.size());
        leagueRepository.save(savedLeague);

        return matches;
    }

    // ============================= VALIDATION =============================
    private void validateLeagueStructure(League league) {

        if (league.getLeagueType() == null) {
            throw new RuntimeException("leagueType required (BILATERAL / TOURNAMENT)");
        }

        String type = league.getLeagueType();
        // LEAGUE FORMAT OPTIONAL FOR LIMITED OVER
        String format = league.getLeagueFormat();

        // If empty → assume LIMITED
        if (format == null || format.isBlank()) {
            format = "LIMITED";
        }

        // normalize
        league.setLeagueFormat(format);

        // ===== BILATERAL =====
        if (type.equalsIgnoreCase("BILATERAL")) {

            if (league.getTeams().size() != 2) {
                throw new RuntimeException("Bilateral series must have exactly 2 teams!");
            }

            if (league.getNoOfMatches() <= 0) {
                throw new RuntimeException("noOfMatches is mandatory for bilateral series!");
            }

            //  TEST SERIES RULE
            if (format.equalsIgnoreCase("TEST")) {

                if (league.getTestDays() == null || league.getTestDays() < 4 || league.getTestDays() > 5) {
                    throw new RuntimeException("Test series must be 4 or 5 days per match!");
                }

                //  Allow 1–7 matches
                if (league.getNoOfMatches() < 1 || league.getNoOfMatches() > 7) {
                    throw new RuntimeException("Test bilateral matches allowed: 1 to 7 only!");
                }

                league.setOversPerInnings(null);
            } //  LIMITED OVERS RULE
            else {

                if (league.getOversPerInnings() == null || league.getOversPerInnings() <= 0) {
                    throw new RuntimeException("oversPerInnings required for bilateral limited overs!");
                }

                //  Allow 1–7 matches
                if (league.getNoOfMatches() < 1 || league.getNoOfMatches() > 7) {
                    throw new RuntimeException("Limited overs bilateral matches allowed: 1 to 7 only!");
                }

                league.setTestDays(null);
            }

            //  No formatType in bilateral
            league.setLeagueFormatType(null);
        } // ===== TOURNAMENT =====
        else if (type.equalsIgnoreCase("TOURNAMENT")) {

            if (league.getTeams().size() < 3) {
                throw new RuntimeException("Tournament must have min 3 teams!");
            }

            if (format.equalsIgnoreCase("TEST")) {
                throw new RuntimeException("Test format not allowed in tournaments");
            }

            if (league.getOversPerInnings() == null || league.getOversPerInnings() <= 0) {
                throw new RuntimeException("oversPerInnings required in tournament");
            }

            if (league.getLeagueFormatType() == null) {
                throw new RuntimeException("leagueFormatType required in tournament");
            }

            league.setTestDays(null);
        } else {
            throw new RuntimeException("leagueType must be BILATERAL or TOURNAMENT");
        }
    }

    // ===================== GENERATORS =====================
    private List<MatchSchedule> generateBilateralMatches(League league) {

        List<MatchSchedule> list = new ArrayList<>();

        String team1 = league.getTeams().get(0);
        String team2 = league.getTeams().get(1);

        int matchCount = league.getNoOfMatches();

        for (int i = 1; i <= matchCount; i++) {
            MatchSchedule m = new MatchSchedule();
            m.setLeagueId(league.getId());
            m.setTeam1Id(team1);
            m.setTeam2Id(team2);
            m.setMatchType(league.getLeagueFormat());
            m.setStatus("Scheduled");
            list.add(m);
        }

        return list;
    }

    private List<MatchSchedule> generateTournamentMatches(
            League league, boolean includeEliminator, boolean includeKnockouts) {

        List<MatchSchedule> matches;
        String type = league.getLeagueFormatType();

        switch (type) {

            case "SINGLE_ROUND_ROBIN":
                matches = generateRoundRobin(league, false);
                break;

            case "DOUBLE_ROUND_ROBIN":
                matches = generateRoundRobin(league, true);
                break;

            case "GROUP":
                matches = generateGroupMatches(league);
                break;

            default:
                throw new RuntimeException("Invalid leagueFormatType");
        }

        if (includeKnockouts) {
            matches.addAll(generateKnockouts(league, includeEliminator));
        }

        return matches;
    }

    // ---------- ROUND ROBIN ----------
    private List<MatchSchedule> generateRoundRobin(League league, boolean doubleRound) {

        List<String> teams = new ArrayList<>(league.getTeams());
        List<MatchSchedule> matches = new ArrayList<>();

        if (teams.size() % 2 != 0) {
            teams.add("BYE");
        }

        int n = teams.size();

        for (int round = 0; round < n - 1; round++) {
            for (int i = 0; i < n / 2; i++) {

                String t1 = teams.get(i);
                String t2 = teams.get(n - i - 1);

                if (!t1.equalsIgnoreCase("") && !t2.equalsIgnoreCase("")) {
                    createMatch(matches, league, t1, t2, "LEAGUE");

                    if (doubleRound) {
                        createMatch(matches, league, t2, t1, "LEAGUE");
                    }
                }
            }

            // rotate
            String last = teams.remove(teams.size() - 1);
            teams.add(1, last);
        }

        return matches;
    }

    // ---------- GROUP ----------
    private List<MatchSchedule> generateGroupMatches(League league) {

        List<String> teams = league.getTeams();
        List<MatchSchedule> list = new ArrayList<>();

        List<String> A = new ArrayList<>();
        List<String> B = new ArrayList<>();

        for (int i = 0; i < teams.size(); i++) {
            if (i % 2 == 0) {
                A.add(teams.get(i));
            } else {
                B.add(teams.get(i));
            }
        }

        list.addAll(generateGroupPairings(league, A));
        list.addAll(generateGroupPairings(league, B));

        return list;
    }

    private List<MatchSchedule> generateGroupPairings(League league, List<String> g) {

        List<MatchSchedule> list = new ArrayList<>();

        for (int i = 0; i < g.size(); i++) {
            for (int j = i + 1; j < g.size(); j++) {
                createMatch(list, league, g.get(i), g.get(j), "GROUP");
            }
        }

        return list;
    }

    // ---------- KNOCKOUT ----------
    private List<MatchSchedule> generateKnockouts(League league, boolean eliminator) {

        List<MatchSchedule> list = new ArrayList<>();

        if (eliminator) {
            createMatch(list, league, "Loser3", "Loser4", "ELIMINATOR");
        }

        createMatch(list, league, "Winner1", "Winner4", "SEMI FINAL 1");
        createMatch(list, league, "Winner2", "Winner3", "SEMI FINAL 2");
        createMatch(list, league, "WinnerSF1", "WinnerSF2", "FINAL");

        return list;
    }

    private void createMatch(List<MatchSchedule> list, League league,
            String t1, String t2, String type) {

        MatchSchedule m = new MatchSchedule();
        m.setLeagueId(league.getId());
        m.setTeam1Id(t1);
        m.setTeam2Id(t2);
        m.setMatchType(type);
        m.setStatus("Scheduled");
        m.setVenue(league.getTour());

        list.add(m);
    }

    // ===================== DATE =====================
    private void validateDateRange(League league, int count) {

        long days = ChronoUnit.DAYS.between(
                league.getStartDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate(),
                league.getEndDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
        ) + 1;

        long needed = league.getLeagueFormat().equalsIgnoreCase("TEST")
                ? count * league.getTestDays()
                : count;

        if (days < needed) {
            throw new RuntimeException("Not enough days for all matches!");
        }
    }

    private void assignMatchDates(League league, List<MatchSchedule> matches) {

        LocalDateTime date = LocalDateTime.ofInstant(
                league.getStartDate().toInstant(), ZoneId.systemDefault())
                .withHour(10).withMinute(0);

        for (MatchSchedule m : matches) {

            m.setScheduledDate(Date.from(date.atZone(ZoneId.systemDefault()).toInstant()));

            if ("TEST".equalsIgnoreCase(league.getLeagueFormat())) {
                date = date.plusDays(league.getTestDays());
            } else {
                date = date.plusDays(1);
            }
        }
    }

    private void assignMatchNumbers(List<MatchSchedule> matches) {
        int no = 1;
        for (MatchSchedule m : matches) {
            m.setMatchNo(no++);
        }
    }

    // ============================= UPDATE =============================
    public League updateLeague(String leagueId, League updatedLeague, MultipartFile logoFile) throws Exception {

        String adminId = getLoggedInAdminId();

        League existingLeague = leagueRepository.findById(leagueId)
                .orElseThrow(() -> new RuntimeException("League not found"));

        //  AUTH CHECK
        if (!existingLeague.getAdminId().equalsIgnoreCase(adminId)) {
            throw new RuntimeException("You are not authorized to update this league!");
        }

        //  UPDATE NAME
        if (updatedLeague.getName() != null && !updatedLeague.getName().isBlank()) {

            leagueRepository.findByName(updatedLeague.getName())
                    .filter(l -> !l.getId().equalsIgnoreCase(leagueId))
                    .ifPresent(l -> {
                        throw new RuntimeException("Another league with this name already exists!");
                    });

            existingLeague.setName(updatedLeague.getName());
        }

        //  UPDATE FORMAT TYPE
        if (updatedLeague.getLeagueFormatType() != null) {
            existingLeague.setLeagueFormatType(updatedLeague.getLeagueFormatType());
        }

        //  UPDATE LEAGUE FORMAT
        if (updatedLeague.getLeagueFormat() != null) {
            existingLeague.setLeagueFormat(updatedLeague.getLeagueFormat());
        }

        //  UPDATE LEAGUE TYPE (BILATERAL / TOURNAMENT)
        if (updatedLeague.getLeagueType() != null) {
            existingLeague.setLeagueType(updatedLeague.getLeagueType());
        }

        //  UPDATE DATES
        if (updatedLeague.getStartDate() != null) {
            existingLeague.setStartDate(updatedLeague.getStartDate());
        }

        if (updatedLeague.getEndDate() != null) {
            existingLeague.setEndDate(updatedLeague.getEndDate());
        }

        //  UPDATE VENUE
        if (updatedLeague.getTour() != null) {
            existingLeague.setTour(updatedLeague.getTour());
        }

        //  UPDATE OVERS / TEST DAYS
        if (updatedLeague.getOversPerInnings() != null) {
            existingLeague.setOversPerInnings(updatedLeague.getOversPerInnings());
        }

        if (updatedLeague.getTestDays() != null) {
            existingLeague.setTestDays(updatedLeague.getTestDays());
        }

        //  UPDATE UMPIRES
        if (updatedLeague.getUmpires() != null && !updatedLeague.getUmpires().isEmpty()) {
            existingLeague.setUmpires(updatedLeague.getUmpires());
        }

        //  UPDATE LOGO
        if (logoFile != null && !logoFile.isEmpty()) {

            if (existingLeague.getLogoUrl() != null && !existingLeague.getLogoUrl().isBlank()) {
                cloudinaryService.deleteFile(existingLeague.getLogoUrl());
            }

            String logoUrl = cloudinaryService.uploadFile(logoFile, "leagues");
            existingLeague.setLogoUrl(logoUrl);
        }

        return leagueRepository.save(existingLeague);
    }

    // ============================= DELETE =============================
    public void deleteLeague(String leagueId) {

        String adminId = getLoggedInAdminId();

        League league = leagueRepository.findById(leagueId)
                .orElseThrow(() -> new RuntimeException("League not found with ID: " + leagueId));

        if (!league.getAdminId().equalsIgnoreCase(adminId)) {
            throw new RuntimeException("This league does not belong to you!");
        }

        // ==================== DELETE TEAMS ====================
        if (league.getTeams() != null && !league.getTeams().isEmpty()) {

            List<Team> teams = teamRepository.findAllById(league.getTeams());

            for (Team team : teams) {

                // delete logo
                if (team.getLogoUrl() != null && !team.getLogoUrl().isEmpty()) {
                    try {
                        cloudinaryService.deleteFile(team.getLogoUrl());
                    } catch (IOException ignored) {
                    }
                }
            }

            teamRepository.deleteAll(teams);
        }

        // ==================== DELETE MATCHES ====================
        matchScheduleRepository.deleteByLeagueId(leagueId);

        // ==================== DELETE SCORES ====================
        matchScoreRepository.deleteByLeagueId(leagueId);

        // ==================== DELETE POINTS TABLE ====================
        pointsTableRepository.deleteByLeagueId(leagueId);

        // ==================== DELETE LEAGUE LOGO ====================
        if (league.getLogoUrl() != null && !league.getLogoUrl().isEmpty()) {
            try {
                cloudinaryService.deleteFile(league.getLogoUrl());
            } catch (IOException ignored) {
            }
        }

        // ==================== DELETE LEAGUE ====================
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

            // ==================== DELETE TEAMS ====================
            if (league.getTeams() != null && !league.getTeams().isEmpty()) {

                List<Team> teams = teamRepository.findAllById(league.getTeams());

                for (Team team : teams) {
                    if (team.getLogoUrl() != null && !team.getLogoUrl().isEmpty()) {
                        try {
                            cloudinaryService.deleteFile(team.getLogoUrl());
                        } catch (IOException ignored) {
                        }
                    }
                }

                teamRepository.deleteAll(teams);
            }

            // ==================== DELETE MATCH SCHEDULE ====================
            matchScheduleRepository.deleteByLeagueId(leagueId);

            // ==================== DELETE MATCH SCORE ====================
            matchScoreRepository.deleteByLeagueId(leagueId);

            // ==================== DELETE POINTS TABLE ====================
            pointsTableRepository.deleteByLeagueId(leagueId);

            // ==================== DELETE LEAGUE LOGO ====================
            if (league.getLogoUrl() != null && !league.getLogoUrl().isEmpty()) {
                try {
                    cloudinaryService.deleteFile(league.getLogoUrl());
                } catch (IOException ignored) {
                }
            }

            // ==================== DELETE LEAGUE ====================
            leagueRepository.deleteById(leagueId);
        }
    }

    public Optional<League> getLeagueById(String leagueId) {
        getLoggedInAdminId();

        Optional<League> opt = leagueRepository.findById(leagueId);

        opt.ifPresent(league -> {
            if (league.getTeams() != null && !league.getTeams().isEmpty()) {

                List<Team> teams = teamRepository.findAllById(league.getTeams());

                List<Map<String, String>> teamDetails = teams.stream()
                        .map(t -> Map.of(
                        "id", t.getId(),
                        "name", t.getName()
                ))
                        .toList();

                // ADD NEW FIELD
                league.setTeamDetails(teamDetails);
            }
        });

        return opt;
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
