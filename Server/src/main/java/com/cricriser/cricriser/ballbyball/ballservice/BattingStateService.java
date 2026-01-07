package com.cricriser.cricriser.ballbyball.ballservice;

import org.springframework.stereotype.Service;

import com.cricriser.cricriser.ballbyball.BallByBall;
import com.cricriser.cricriser.match.matchscoring.MatchScore;

@Service
public class BattingStateService {

    public void applyWicketState(BallByBall ball, MatchScore score) {

        if (!ball.isWicket()) {
            return;
        }

        String wicketType = ball.getWicketType();

        // ❌ RETIRED HURT → NOT OUT
        if ("RETIRED_HURT".equalsIgnoreCase(wicketType)) {
            return;
        }

        String outBatterId = ball.getOutBatterId();
        String newBatterId = ball.getNewBatterId();

        if (outBatterId == null || newBatterId == null) {
            throw new RuntimeException("outBatterId and newBatterId are mandatory");
        }

        // =================================================
        // 1️⃣ MOVE OUT BATTER → OUT LIST
        // =================================================
        if (score.getBattingTeamId().equals(score.getTeam1Id())) {
            score.getTeam1OutBatters().add(outBatterId);
        } else {
            score.getTeam2OutBatters().add(outBatterId);
        }

        // =================================================
        // 2️⃣ REMOVE *NEW BATTER* FROM YET-TO-BAT
        // =================================================
        if (score.getBattingTeamId().equals(score.getTeam1Id())) {
            score.getTeam1YetToBat().remove(newBatterId);
        } else {
            score.getTeam2YetToBat().remove(newBatterId);
        }

        // =================================================
        // 3️⃣ REPLACE BATTER (NO STRIKE ROTATION HERE)
        // =================================================
        replaceBatter(ball, score, newBatterId);
    }

    private void replaceBatter(
            BallByBall ball,
            MatchScore score,
            String newBatterId
    ) {
        String outBatterId = ball.getOutBatterId();

        String striker = score.getStrikerId();
        String nonStriker = score.getNonStrikerId();

        // ================= RUN OUT =================
        if ("RUN_OUT".equalsIgnoreCase(ball.getWicketType())) {

            if (outBatterId.equals(striker)) {
                // striker got out → new batter takes striker position
                score.setStrikerId(newBatterId);

            } else if (outBatterId.equals(nonStriker)) {
                // non-striker got out → new batter takes non-striker position
                score.setNonStrikerId(newBatterId);

            } else {
                throw new RuntimeException(
                        "Out batter is neither striker nor non-striker"
                );
            }

            return; // 🚫 NO strike rotation here
        }

        // ================= OTHER WICKETS =================
        // Bowled, Caught, LBW, Stumped → striker always out
        score.setStrikerId(newBatterId);
    }

    private void swap(MatchScore score) {
        String temp = score.getStrikerId();
        score.setStrikerId(score.getNonStrikerId());
        score.setNonStrikerId(temp);
    }

}
