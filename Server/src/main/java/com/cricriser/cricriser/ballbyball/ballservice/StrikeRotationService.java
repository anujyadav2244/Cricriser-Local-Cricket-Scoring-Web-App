package com.cricriser.cricriser.ballbyball.ballservice;

import org.springframework.stereotype.Service;

import com.cricriser.cricriser.ballbyball.BallByBall;
import com.cricriser.cricriser.match.matchscoring.MatchScore;

@Service
public class StrikeRotationService {

    public void rotateStrike(BallByBall ball, MatchScore score) {

        // ❌ Run-out handled completely elsewhere
        if ("RUN_OUT".equalsIgnoreCase(ball.getWicketType())) {
            return;
        }

        // ❌ No rotation on dead ball boundary
        if (ball.isBoundary()) {
            if (ball.isOverCompleted()) {
                swap(score);
            }
            return;
        }

        int runsForRotation = 0;

        // Bat runs (includes NO BALL bat runs)
        if (!"WIDE".equalsIgnoreCase(ball.getExtraType())) {
            runsForRotation += ball.getRuns();
        }

        // Running runs (byes / leg byes)
        runsForRotation += ball.getRunningRuns();

        if (runsForRotation % 2 == 1) {
            swap(score);
        }

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
