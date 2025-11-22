package com.cricriser.cricriser.match;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.cricriser.cricriser.league.League;
import com.cricriser.cricriser.league.LeagueRepository;
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
    private LeagueRepository leagueRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private PointsTableService pointsTableService;

    // ===================== ADD NEW SCORE =====================
    public MatchScore addScore(MatchScore score) {

        League league = leagueRepository.findById(score.getLeagueId())
                .orElseThrow(() -> new RuntimeException("Invalid leagueId! League not found."));

        MatchSchedule match = matchScheduleRepository.findById(score.getMatchId())
                .orElseThrow(() -> new RuntimeException("Invalid matchId! Match not found."));

        if (!match.getLeagueId().equals(score.getLeagueId())) {
            throw new RuntimeException("This match does NOT belong to the provided leagueId!");
        }

        MatchScore existing = matchScoreRepository.findByMatchId(score.getMatchId());
        if (existing != null) {
            throw new RuntimeException("Score already exists! Use updateScore().");
        }

        Team team1 = teamRepository.findById(score.getTeam1Id())
                .orElseThrow(() -> new RuntimeException("Invalid team1Id provided!"));

        Team team2 = teamRepository.findById(score.getTeam2Id())
                .orElseThrow(() -> new RuntimeException("Invalid team2Id provided!"));

        if (!match.getTeam1().equalsIgnoreCase(team1.getName())) {
            throw new RuntimeException("Team1 mismatch! Scheduled team1 is: " + match.getTeam1());
        }

        if (!match.getTeam2().equalsIgnoreCase(team2.getName())) {
            throw new RuntimeException("Team2 mismatch! Scheduled team2 is: " + match.getTeam2());
        }

        boolean team1InLeague = league.getTeams().stream()
                .map(t -> t.contains(":") ? t.split(":")[1] : t)
                .anyMatch(id -> id.equalsIgnoreCase(team1.getId()));

        boolean team2InLeague = league.getTeams().stream()
                .map(t -> t.contains(":") ? t.split(":")[1] : t)
                .anyMatch(id -> id.equalsIgnoreCase(team2.getId()));

        if (!team1InLeague) {
            throw new RuntimeException(team1.getName() + " is NOT part of league " + league.getName());
        }

        if (!team2InLeague) {
            throw new RuntimeException(team2.getName() + " is NOT part of league " + league.getName());
        }

        if (score.getTeam1PlayingXI() == null || score.getTeam1PlayingXI().size() != 11)
            throw new RuntimeException(team1.getName() + " Playing XI must contain EXACTLY 11 players!");

        if (score.getTeam2PlayingXI() == null || score.getTeam2PlayingXI().size() != 11)
            throw new RuntimeException(team2.getName() + " Playing XI must contain EXACTLY 11 players!");

        List<String> squad1 = team1.getSquad().stream().map(p -> p.getId()).collect(Collectors.toList());
        List<String> squad2 = team2.getSquad().stream().map(p -> p.getId()).collect(Collectors.toList());

        if (!squad1.containsAll(score.getTeam1PlayingXI()))
            throw new RuntimeException("Some players in Team1 Playing XI are NOT in squad!");

        if (!squad2.containsAll(score.getTeam2PlayingXI()))
            throw new RuntimeException("Some players in Team2 Playing XI are NOT in squad!");

        if (new HashSet<>(score.getTeam1PlayingXI()).size() != 11)
            throw new RuntimeException("Duplicate players in Team1 Playing XI!");

        if (new HashSet<>(score.getTeam2PlayingXI()).size() != 11)
            throw new RuntimeException("Duplicate players in Team2 Playing XI!");

        if (!score.getTossWinner().equals(team1.getId()) &&
                !score.getTossWinner().equals(team2.getId())) {
            throw new RuntimeException("Toss winner must be either " + team1.getName() + " or " + team2.getName());
        }

        validateOvers(score.getTeam1Overs(), team1.getName());
        validateOvers(score.getTeam2Overs(), team2.getName());

        if ("Completed".equalsIgnoreCase(score.getMatchStatus())) {
            computeWinner(score, team1, team2);
        }

        MatchScore saved = matchScoreRepository.save(score);
        pointsTableService.updatePointsTable(score.getLeagueId());

        return saved;
    }

    // ===================== UPDATE EXISTING SCORE =====================
    public MatchScore updateScore(MatchScore score) {

        League league = leagueRepository.findById(score.getLeagueId())
                .orElseThrow(() -> new RuntimeException("Invalid leagueId!"));

        MatchSchedule match = matchScheduleRepository.findById(score.getMatchId())
                .orElseThrow(() -> new RuntimeException("Invalid matchId!"));

        MatchScore existing = matchScoreRepository.findByMatchId(score.getMatchId());
        if (existing == null) {
            throw new RuntimeException("No existing score found! Use addScore().");
        }

        score.setId(existing.getId());

        Team team1 = teamRepository.findById(score.getTeam1Id())
                .orElseThrow(() -> new RuntimeException("Invalid team1Id!"));

        Team team2 = teamRepository.findById(score.getTeam2Id())
                .orElseThrow(() -> new RuntimeException("Invalid team2Id!"));

        if (!match.getTeam1().equalsIgnoreCase(team1.getName()))
            throw new RuntimeException("Team1 mismatch! Scheduled team1 is: " + match.getTeam1());

        if (!match.getTeam2().equalsIgnoreCase(team2.getName()))
            throw new RuntimeException("Team2 mismatch! Scheduled team2 is: " + match.getTeam2());

        if (score.getTeam1PlayingXI() == null || score.getTeam1PlayingXI().size() != 11)
            throw new RuntimeException(team1.getName() + " Playing XI must contain EXACTLY 11 players!");

        if (score.getTeam2PlayingXI() == null || score.getTeam2PlayingXI().size() != 11)
            throw new RuntimeException(team2.getName() + " Playing XI must contain EXACTLY 11 players!");

        List<String> squad1 = team1.getSquad().stream().map(p -> p.getId()).collect(Collectors.toList());
        List<String> squad2 = team2.getSquad().stream().map(p -> p.getId()).collect(Collectors.toList());

        if (!squad1.containsAll(score.getTeam1PlayingXI()))
            throw new RuntimeException("Some players in Team1 Playing XI are NOT in squad!");

        if (!squad2.containsAll(score.getTeam2PlayingXI()))
            throw new RuntimeException("Some players in Team2 Playing XI are NOT in squad!");

        if (new HashSet<>(score.getTeam1PlayingXI()).size() != 11)
            throw new RuntimeException("Duplicate players in Team1 Playing XI!");

        if (new HashSet<>(score.getTeam2PlayingXI()).size() != 11)
            throw new RuntimeException("Duplicate players in Team2 Playing XI!");

        if (!score.getTossWinner().equals(team1.getId()) &&
                !score.getTossWinner().equals(team2.getId())) {
            throw new RuntimeException("Toss winner must be either " + team1.getName() + " or " + team2.getName());
        }

        validateOvers(score.getTeam1Overs(), team1.getName());
        validateOvers(score.getTeam2Overs(), team2.getName());

        if ("Completed".equalsIgnoreCase(score.getMatchStatus())) {
            computeWinner(score, team1, team2);
        }

        MatchScore saved = matchScoreRepository.save(score);
        pointsTableService.updatePointsTable(score.getLeagueId());

        return saved;
    }

    // ---------------------- Overs Validation --------------------------
    private void validateOvers(double overs, String teamName) {
        int whole = (int) overs;
        int balls = (int) Math.round((overs - whole) * 10);

        if (balls < 0 || balls > 5)
            throw new RuntimeException("Invalid overs for " + teamName + "! Ball count must be 0–5.");

        if (whole > 50)
            throw new RuntimeException(teamName + " cannot bowl more than 50 overs!");
    }

    // ---------------------- Winner Calculation ------------------------
    private void computeWinner(MatchScore score, Team t1, Team t2) {

        boolean tossWonByTeam1 = score.getTossWinner().equals(score.getTeam1Id());
        boolean choseBat = score.getTossDecision().equalsIgnoreCase("bat");

        String battingFirst = tossWonByTeam1
                ? (choseBat ? score.getTeam1Id() : score.getTeam2Id())
                : (choseBat ? score.getTeam2Id() : score.getTeam1Id());

        String battingSecond = battingFirst.equals(score.getTeam1Id())
                ? score.getTeam2Id()
                : score.getTeam1Id();

        int first = battingFirst.equals(score.getTeam1Id()) ? score.getTeam1Runs() : score.getTeam2Runs();
        int second = battingSecond.equals(score.getTeam1Id()) ? score.getTeam1Runs() : score.getTeam2Runs();
        int secondWkts = battingSecond.equals(score.getTeam1Id()) ? score.getTeam1Wickets() : score.getTeam2Wickets();

        if (first > second) {
            score.setMatchWinner(battingFirst);
            score.setResult(battingFirst + " won by " + (first - second) + " runs");
        } else if (second > first) {
            score.setMatchWinner(battingSecond);
            score.setResult(battingSecond + " won by " + (10 - secondWkts) + " wickets");
        } else {
            score.setMatchWinner("Tie");
            score.setResult("Match tied");
        }
    }

    // ---------------------- Basic CRUD ------------------------
    public MatchScore getMatchScoreByMatchId(String matchId) {
        return matchScoreRepository.findByMatchId(matchId);
    }

    public List<MatchScore> getAllScores() {
        return matchScoreRepository.findAll();
    }

    public void deleteScoreById(String id) {
        matchScoreRepository.deleteById(id);
    }

    public void deleteAllScores() {
    matchScoreRepository.deleteAll();
}

}
