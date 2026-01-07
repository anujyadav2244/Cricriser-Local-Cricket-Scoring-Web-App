package com.cricriser.cricriser.match.matchscoring;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.cricriser.cricriser.ballbyball.BallByBall;
import com.cricriser.cricriser.ballbyball.ballservice.BallService;

@Service
public class MatchScoreUpdateService {

    @Autowired
    private MatchScoreRepository matchScoreRepository;

    @Autowired
    private MatchScoreService matchScoreService;

    @Autowired
    private BallService ballService;

    // ===================== PRE BALL VALIDATION =====================
    public MatchScore validateBeforeBall(String matchId, int innings) {

        MatchScore score = matchScoreRepository.findByMatchId(matchId);
        if (score == null) {
            throw new RuntimeException("Match score does not exist");
        }

        // ✅ ONLY this check
        if (!"Match In Progress".equalsIgnoreCase(score.getMatchStatus())) {
            throw new RuntimeException("Match is not in progress");
        }

        if (innings == 1 && score.isFirstInningsCompleted()) {
            throw new RuntimeException("First innings already completed");
        }

        if (innings == 2) {
            if (!score.isFirstInningsCompleted()) {
                throw new RuntimeException("First innings not completed yet");
            }
            if (score.isSecondInningsCompleted()) {
                throw new RuntimeException("Second innings already completed");
            }
        }

        if (score.getStrikerId() == null || score.getStrikerId().isBlank()) {
            throw new RuntimeException("Striker not set");
        }

        if (score.getNonStrikerId() == null || score.getNonStrikerId().isBlank()) {
            throw new RuntimeException("Non-striker not set");
        }

        if (score.getStrikerId().equals(score.getNonStrikerId())) {
            throw new RuntimeException("Striker and non-striker cannot be same");
        }

        return score;
    }

    public void updateMatchScore(BallByBall ball, MatchScore score) {

        boolean team1Batting
                = score.getBattingTeamId().equals(score.getTeam1Id());

        int runs = ballService.calculateTotalRuns(ball);

        // RUNS
        if (team1Batting) {
            score.setTeam1Runs(score.getTeam1Runs() + runs);
        } else {
            score.setTeam2Runs(score.getTeam2Runs() + runs);
        }

        // EXTRAS
        if (ball.getExtraRuns() > 0) {
            if (team1Batting) {
                score.setTeam1Extras(score.getTeam1Extras() + ball.getExtraRuns());
            } else {
                score.setTeam2Extras(score.getTeam2Extras() + ball.getExtraRuns());
            }
        }

        // WICKETS
        if (ball.isWicket()) {
            if (team1Batting) {
                score.setTeam1Wickets(score.getTeam1Wickets() + 1);
            } else {
                score.setTeam2Wickets(score.getTeam2Wickets() + 1);
            }
        }

        // OVERS
        if (ball.isLegalBall()) {
            if (team1Batting) {
                score.setTeam1Overs(incrementOvers(score.getTeam1Overs()));
            } else {
                score.setTeam2Overs(incrementOvers(score.getTeam2Overs()));
            }
        }

        // 🔥 INNINGS + MATCH COMPLETION
        checkInningsCompletion(score, ball);

        matchScoreRepository.save(score);
    }

    private void checkInningsCompletion(MatchScore score, BallByBall ball) {

        boolean team1Batting
                = score.getBattingTeamId().equals(score.getTeam1Id());

        int wickets = team1Batting
                ? score.getTeam1Wickets()
                : score.getTeam2Wickets();

        double overs = team1Batting
                ? score.getTeam1Overs()
                : score.getTeam2Overs();

        int runs = team1Batting
                ? score.getTeam1Runs()
                : score.getTeam2Runs();

        boolean overLimitReached
                = ball.isOverCompleted()
                && overs >= score.getTotalOvers();

        // ================= FIRST INNINGS =================
        if (!score.isFirstInningsCompleted()) {

            if (wickets == 10 || overLimitReached) {

                score.setFirstInningsCompleted(true);

                // Match still continues
                score.setMatchStatus("Match In Progress");
            }
            return;
        }

        // ================= SECOND INNINGS =================
        int target = score.getTeam1Runs() + 1;

        if (!score.isSecondInningsCompleted()) {

            if (wickets == 10
                    || overLimitReached
                    || runs >= target) {

                score.setSecondInningsCompleted(true);
                score.setMatchStatus("Completed");

                // 🔥🔥🔥 DECIDE WINNER HERE 🔥🔥🔥
                matchScoreService.computeWinner(score);
            }
        }
    }

    private double incrementOvers(double overs) {

        int fullOvers = (int) overs;
        int balls = (int) Math.round((overs - fullOvers) * 10);

        balls++;

        if (balls == 6) {
            fullOvers++;
            balls = 0;
        }

        return fullOvers + (balls / 10.0);
    }

}
