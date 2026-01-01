package com.cricriser.cricriser.ballbyball.ballservice;

import org.springframework.beans.factory.annotation.Autowired;
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

        boolean strikerOut
                = ball.getOutBatterId().equals(score.getStrikerId());

        boolean oddRun = ball.getRuns() % 2 == 1;
        boolean lastBallOfOver = ball.isOverCompleted();

        // ================= NORMAL WICKET =================
        if (!"RUN_OUT".equalsIgnoreCase(ball.getWicketType())) {
            score.setStrikerId(newBatterId);
            return;
        }

        // ================= RUN OUT =================
        if (strikerOut) {
            // striker got out → new batter replaces striker
            score.setStrikerId(newBatterId);

            // odd run + crossing → swap
            if (oddRun) {
                swap(score);
            }

        } else {
            // non-striker got out → new batter replaces non-striker
            score.setNonStrikerId(newBatterId);

            // odd run + crossing → swap
            if (oddRun) {
                swap(score);
            }
        }

        // ================= OVER END SWAP =================
        if (lastBallOfOver) {
            swap(score);
        }
    }

    private void swap(MatchScore score) {
        String temp = score.getStrikerId();
        score.setStrikerId(score.getNonStrikerId());
        score.setNonStrikerId(temp);
    }

}
