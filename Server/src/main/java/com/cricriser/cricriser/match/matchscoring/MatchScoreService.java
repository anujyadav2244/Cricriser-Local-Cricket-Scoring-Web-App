package com.cricriser.cricriser.match.matchscoring;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cricriser.cricriser.ballbyball.BallByBall;
import com.cricriser.cricriser.ballbyball.BallByBallRepository;
import com.cricriser.cricriser.league.League;
import com.cricriser.cricriser.league.LeagueRepository;
import com.cricriser.cricriser.match.matchscheduling.MatchSchedule;
import com.cricriser.cricriser.match.matchscheduling.MatchScheduleRepository;
import com.cricriser.cricriser.player.matchplayerstats.MatchPlayerStats;
import com.cricriser.cricriser.player.matchplayerstats.MatchPlayerStatsRepository;
import com.cricriser.cricriser.points.PointsTableService;
import com.cricriser.cricriser.team.Team;
import com.cricriser.cricriser.team.TeamRepository;

@Service
public class MatchScoreService {

    @Autowired
    private MatchScoreRepository matchScoreRepository;

    @Autowired
    private MatchScheduleRepository matchScheduleRepository;

    @Autowired
    private MatchPlayerStatsRepository repo;

    @Autowired
    private LeagueRepository leagueRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private PointsTableService pointsTableService;

    @Autowired
    private BallByBallRepository ballByBallRepository;

    // Initiate innings
    public MatchScore startInnings(
            String matchId,
            int innings,
            String strikerId,
            String nonStrikerId,
            String bowlerId
    ) {
        if (innings <= 0) {
            throw new RuntimeException("Invalid innings number");
        }

        MatchScore score = matchScoreRepository.findByMatchId(matchId);
        if (score == null) {
            throw new RuntimeException("Match score not found");
        }

        // INNINGS ALREADY STARTED → CONTINUE
        if (score.getInnings() == innings
                && score.getStrikerId() != null
                && score.getNonStrikerId() != null
                && score.getCurrentBowlerId() != null) {

            return score;
        }

        // ---------- FIRST TIME START ----------
        if (strikerId.equals(nonStrikerId)) {
            throw new RuntimeException("Striker and non-striker cannot be same");
        }

        // Decide batting & bowling teams
        String battingTeamId;
        String bowlingTeamId;

        if (innings == 1) {

            boolean tossWinnerBats
                    = score.getTossDecision().equalsIgnoreCase("BAT");

            if (tossWinnerBats) {
                battingTeamId = score.getTossWinner();
            } else {
                battingTeamId = score.getTossWinner().equals(score.getTeam1Id())
                        ? score.getTeam2Id()
                        : score.getTeam1Id();
            }

            bowlingTeamId = battingTeamId.equals(score.getTeam1Id())
                    ? score.getTeam2Id()
                    : score.getTeam1Id();
        } else {

            battingTeamId = score.getBattingTeamId().equals(score.getTeam1Id())
                    ? score.getTeam2Id()
                    : score.getTeam1Id();

            bowlingTeamId = battingTeamId.equals(score.getTeam1Id())
                    ? score.getTeam2Id()
                    : score.getTeam1Id();
        }

        score.setBattingTeamId(battingTeamId);

        // Validate opening batters
        List<String> yetToBat = battingTeamId.equals(score.getTeam1Id())
                ? score.getTeam1YetToBat()
                : score.getTeam2YetToBat();

        if (!yetToBat.contains(strikerId) || !yetToBat.contains(nonStrikerId)) {
            throw new RuntimeException("Opening batters must be from Yet-To-Bat list");
        }

        yetToBat.remove(strikerId);
        yetToBat.remove(nonStrikerId);

        // Validate bowler
        List<String> bowlingXI = bowlingTeamId.equals(score.getTeam1Id())
                ? score.getTeam1PlayingXI()
                : score.getTeam2PlayingXI();

        if (!bowlingXI.contains(bowlerId)) {
            throw new RuntimeException("Bowler must belong to bowling team");
        }

        // Set state
        score.setInnings(innings);
        score.setStrikerId(strikerId);
        score.setNonStrikerId(nonStrikerId);
        score.setCurrentBowlerId(bowlerId);
        score.setLastOverBowlerId(null);
        score.setMatchStatus("Match In Progress");

        createIfNotExists(matchId, strikerId);
        createIfNotExists(matchId, nonStrikerId);
        createIfNotExists(matchId, bowlerId);

        return matchScoreRepository.save(score);
    }

    // ===================== ADD NEW SCORE =====================
    public MatchScore addScore(MatchScore score) {

        if (score.getLeagueId() == null) {
            throw new RuntimeException("leagueId is missing");
        }

        if (score.getMatchId() == null) {
            throw new RuntimeException("matchId is missing");
        }

        if (score.getTeam1Id() == null) {
            throw new RuntimeException("team1Id is missing");
        }

        if (score.getTeam2Id() == null) {
            throw new RuntimeException("team2Id is missing");
        }

        if (score.getTossWinner() == null) {
            throw new RuntimeException("Toss Winner is missing");
        }

        League league = leagueRepository.findById(score.getLeagueId())
                .orElseThrow(() -> new RuntimeException("League not found"));

        if (!league.getLeagueFormat().equalsIgnoreCase("TEST")) {
            score.setTotalOvers(league.getOversPerInnings());
        } else {
            score.setTotalOvers(Integer.MAX_VALUE); // unlimited for test
        }

        MatchSchedule match = matchScheduleRepository.findById(score.getMatchId())
                .orElseThrow(() -> new RuntimeException("Match not found"));

        if (!match.getLeagueId().equals(score.getLeagueId())) {
            throw new RuntimeException("Match does not belong to this league");
        }

        MatchScore existing = matchScoreRepository.findByMatchId(score.getMatchId());
        if (existing != null) {
            throw new RuntimeException("Score already exists");
        }

        Team team1 = teamRepository.findById(score.getTeam1Id())
                .orElseThrow(() -> new RuntimeException("Invalid team1Id"));

        Team team2 = teamRepository.findById(score.getTeam2Id())
                .orElseThrow(() -> new RuntimeException("Invalid team2Id"));

        if (!match.getTeam1Id().equalsIgnoreCase(team1.getName())) {
            throw new RuntimeException("Team1 mismatch with schedule");
        }

        if (!match.getTeam2Id().equalsIgnoreCase(team2.getName())) {
            throw new RuntimeException("Team2 mismatch with schedule");
        }

        if (!league.getTeams().contains(score.getTeam1Id())
                || !league.getTeams().contains(score.getTeam2Id())) {
            throw new RuntimeException("Both teams must belong to this league");
        }

        if (score.getTeam1PlayingXI() == null || score.getTeam1PlayingXI().size() != 11) {
            throw new RuntimeException("Team1 Playing XI must be exactly 11 players");
        }

        if (score.getTeam2PlayingXI() == null || score.getTeam2PlayingXI().size() != 11) {
            throw new RuntimeException("Team2 Playing XI must be exactly 11 players");
        }

        List<String> squad1 = team1.getSquadPlayerIds();
        List<String> squad2 = team2.getSquadPlayerIds();

        if (!squad1.containsAll(score.getTeam1PlayingXI())) {
            throw new RuntimeException("Team1 Playing XI contains players not in squad");
        }

        if (!squad2.containsAll(score.getTeam2PlayingXI())) {
            throw new RuntimeException("Team2 Playing XI contains players not in squad");
        }

        if (new HashSet<>(score.getTeam1PlayingXI()).size() != 11) {
            throw new RuntimeException("Duplicate player in Team1 XI");
        }

        if (new HashSet<>(score.getTeam2PlayingXI()).size() != 11) {
            throw new RuntimeException("Duplicate player in Team2 XI");
        }

        if (!score.getTossWinner().equals(team1.getId())
                && !score.getTossWinner().equals(team2.getId())) {
            throw new RuntimeException("Invalid toss winner");
        }

        validateOvers(score.getTeam1Overs(), league, team1.getName());
        validateOvers(score.getTeam2Overs(), league, team2.getName());

        if ("Completed".equalsIgnoreCase(score.getMatchStatus())) {
            computeWinner(score);
        }

        // before save
        score.setMatchStatus("Match In Progress");

        // ===================== INIT BATTING LISTS =====================
        // Team 1
        score.setTeam1YetToBat(new ArrayList<>(score.getTeam1PlayingXI()));
        score.setTeam1OutBatters(new ArrayList<>());

        // Team 2
        score.setTeam2YetToBat(new ArrayList<>(score.getTeam2PlayingXI()));
        score.setTeam2OutBatters(new ArrayList<>());
        // NOW SAVE
        MatchScore saved = matchScoreRepository.save(score);

        pointsTableService.updatePointsTable(score.getLeagueId());

        return saved;

    }

    // ===================== UPDATE SCORE =====================
    public MatchScore updateScore(MatchScore score) {

        MatchScore existing = matchScoreRepository.findByMatchId(score.getMatchId());
        if (existing == null) {
            throw new RuntimeException("No score exists for this match");
        }

        score.setId(existing.getId());

        League league = leagueRepository.findById(score.getLeagueId())
                .orElseThrow(() -> new RuntimeException("Invalid leagueId"));

        MatchSchedule match = matchScheduleRepository.findById(score.getMatchId())
                .orElseThrow(() -> new RuntimeException("Invalid matchId"));

        Team team1 = teamRepository.findById(score.getTeam1Id())
                .orElseThrow(() -> new RuntimeException("Invalid team1Id"));

        Team team2 = teamRepository.findById(score.getTeam2Id())
                .orElseThrow(() -> new RuntimeException("Invalid team2Id"));

        if (!match.getTeam1Id().equalsIgnoreCase(team1.getName())
                || !match.getTeam2Id().equalsIgnoreCase(team2.getName())) {
            throw new RuntimeException("Scheduled team mismatch");
        }

        validateOvers(score.getTeam1Overs(), league, team1.getName());
        validateOvers(score.getTeam2Overs(), league, team2.getName());

        if ("Completed".equalsIgnoreCase(score.getMatchStatus())) {
            computeWinner(score);
        }

        MatchScore saved = matchScoreRepository.save(score);
        pointsTableService.updatePointsTable(score.getLeagueId());

        return saved;
    }

    // ===================== OVERS VALIDATION =====================
    private void validateOvers(double overs, League league, String teamName) {

        int full = (int) overs;
        int balls = (int) Math.round((overs - full) * 10);

        if (balls < 0 || balls > 5) {
            throw new RuntimeException("Invalid overs format for " + teamName);
        }

        if (league.getLeagueFormat().equalsIgnoreCase("TEST")) {
            return;
        }

        Integer maxOvers = league.getOversPerInnings();
        if (maxOvers == null) {
            throw new RuntimeException("Overs not configured");
        }

        if (full > maxOvers) {
            throw new RuntimeException(teamName + " exceeded overs limit");
        }
    }

    // ===================== WINNER DECIDER =====================
    private void computeWinner(MatchScore score) {

        String t1 = score.getTeam1Id();
        String t2 = score.getTeam2Id();

        String firstBat = score.getTossWinner().equals(t1)
                ? (score.getTossDecision().equalsIgnoreCase("bat") ? t1 : t2)
                : (score.getTossDecision().equalsIgnoreCase("bat") ? t2 : t1);

        String secondBat = firstBat.equals(t1) ? t2 : t1;

        int firstRuns = firstBat.equals(t1) ? score.getTeam1Runs() : score.getTeam2Runs();
        int secondRuns = secondBat.equals(t1) ? score.getTeam1Runs() : score.getTeam2Runs();
        int secondWickets = secondBat.equals(t1) ? score.getTeam1Wickets() : score.getTeam2Wickets();

        int diff = Math.abs(firstRuns - secondRuns);

        if (firstRuns > secondRuns) {
            score.setMatchWinner(firstBat);
            score.setResult(firstBat + " won by " + diff + " runs");
        } else if (secondRuns > firstRuns) {

            int wicketsLeft = 10 - secondWickets;
            score.setMatchWinner(secondBat);
            score.setResult(secondBat + " won by " + wicketsLeft + " wickets");
        } else {
            score.setMatchWinner("Tie");
            score.setResult("Match tied");
        }
    }

    // ===================== BASIC CRUD =====================
    public MatchScore getMatchScoreByMatchId(String matchId) {
        return matchScoreRepository.findByMatchId(matchId);
    }

    public List<MatchScore> getAllScores() {
        return matchScoreRepository.findAll();
    }

    @Transactional
    public void deleteScoreById(String id) {

        MatchScore score = matchScoreRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Match score not found"));

        String matchId = score.getMatchId();
        String leagueId = score.getLeagueId();

        // DELETE BALL-BY-BALL
        ballByBallRepository.deleteByMatchId(matchId);

        // DELETE MATCH PLAYER STATS
        repo.deleteByMatchId(matchId);

        // DELETE MATCH SCORE
        matchScoreRepository.deleteById(id);

        // UPDATE POINTS TABLE
        pointsTableService.updatePointsTable(leagueId);
    }

    @Transactional
    public void deleteAllScores() {

        ballByBallRepository.deleteAll();
        repo.deleteAll();                 // match_player_stats
        matchScoreRepository.deleteAll();
    }

    public void createIfNotExists(String matchId, String playerId) {

        repo.findByMatchIdAndPlayerId(matchId, playerId)
                .orElseGet(() -> {
                    MatchPlayerStats stats = new MatchPlayerStats();
                    stats.setMatchId(matchId);
                    stats.setPlayerId(playerId);
                    stats.setRuns(0);
                    stats.setBalls(0);
                    stats.setWickets(0);
                    stats.setRunsConceded(0);
                    stats.setBallsBowled(0);
                    stats.setStrikeRate(0);
                    stats.setEconomy(0);
                    return repo.save(stats);
                });
    }


}
