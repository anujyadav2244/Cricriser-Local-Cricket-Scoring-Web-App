package com.cricriser.cricriser.ballbyball.ballservice;

import org.springframework.stereotype.Service;

import com.cricriser.cricriser.ballbyball.BallByBall;
import com.cricriser.cricriser.match.matchscoring.MatchScore;

@Service
public class StrikeRotationService {

    public void rotateStrike(BallByBall ball, MatchScore score) {

        // 🚫 NO STRIKE ROTATION ON ANY WICKET EXCEPT RUN OUT
        if (ball.isWicket() && !"RUN_OUT".equalsIgnoreCase(ball.getWicketType())) {

            // Only over-end swap is allowed
            if (ball.isOverCompleted()) {
                swap(score);
            }
            return;
        }

        // 🚫 RUN OUT handled separately
        if ("RUN_OUT".equalsIgnoreCase(ball.getWicketType())) {
            return;
        }

        // 🚫 Boundary = dead ball (no mid-ball rotation)
        if (ball.isBoundary()) {
            if (ball.isOverCompleted()) {
                swap(score);
            }
            return;
        }

        // ================= RUN ROTATION =================
        int runsForRotation = 0;

        // BAT RUNS (ignore wides)
        if (!"WIDE".equalsIgnoreCase(ball.getExtraType())) {
            runsForRotation += ball.getRuns();
        }

        // BYES / LEG BYES
        runsForRotation += ball.getRunningRuns();

        // 🔁 Odd runs → swap
        if (runsForRotation % 2 == 1) {
            swap(score);
        }

        // 🔁 Over end → swap
        if (ball.isOverCompleted()) {
            swap(score);
        }
    }

    private void swap(MatchScore score) {
        String temp = score.getStrikerId();
        score.setStrikerId(score.getNonStrikerId());
        score.setNonStrikerId(temp);
    }
}
