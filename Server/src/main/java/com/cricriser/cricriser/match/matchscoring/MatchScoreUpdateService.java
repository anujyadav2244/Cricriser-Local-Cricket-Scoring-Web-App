package com.cricriser.cricriser.match.matchscoring;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.cricriser.cricriser.ballbyball.BallByBall;

@Service
public class MatchScoreUpdateService {

    @Autowired
    private MatchScoreRepository matchScoreRepository;

    // ===================== PRE BALL VALIDATION =====================
    public MatchScore validateBeforeBall(String matchId, int innings) {

        MatchScore score = matchScoreRepository.findByMatchId(matchId);
        if (score == null) {
            throw new RuntimeException("Match score does not exist");
        }

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

        // ✅ Striker checks ONLY
        if (score.getStrikerId() == null || score.getStrikerId().isBlank()) {
            throw new RuntimeException("Striker not set");
        }

        if (score.getNonStrikerId() == null || score.getNonStrikerId().isBlank()) {
            throw new RuntimeException("Non-striker not set");
        }

        if (score.getStrikerId().equals(score.getNonStrikerId())) {
            throw new RuntimeException("Striker and non-striker cannot be same");
        }

        // ❌ NO BOWLER VALIDATION HERE
        return score;
    }

    public void updateMatchScore(BallByBall ball, MatchScore score) {

        boolean team1Batting
                = score.getBattingTeamId().equals(score.getTeam1Id());

        int runs = ball.getRuns() + ball.getExtraRuns();

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

        // OVERS (legal balls only)
        if (ball.isLegalBall()) {

            if (team1Batting) {
                score.setTeam1Overs(
                        incrementOvers(score.getTeam1Overs())
                );
            } else {
                score.setTeam2Overs(
                        incrementOvers(score.getTeam2Overs())
                );
            }
        }

        // INNINGS END
        checkInningsCompletion(score);

        matchScoreRepository.save(score);
    }


    private void checkInningsCompletion(MatchScore score) {

        if (!score.isFirstInningsCompleted()) {

            if (score.getTeam1Wickets() == 10
                    || score.getTeam1Overs() >= score.getTotalOvers()) {

                score.setFirstInningsCompleted(true);
            }

        } else if (!score.isSecondInningsCompleted()) {

            if (score.getTeam2Wickets() == 10
                    || score.getTeam2Overs() >= score.getTotalOvers()
                    || score.getTeam2Runs() > score.getTeam1Runs()) {

                score.setSecondInningsCompleted(true);
                score.setMatchStatus("Completed");
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
