package com.cricriser.cricriser.ballbyball.ballservice;

import org.springframework.stereotype.Service;

import com.cricriser.cricriser.ballbyball.BallByBall;
import com.cricriser.cricriser.match.matchscoring.MatchScore;

@Service
public class StrikeRotationService {

    public void rotateStrike(BallByBall ball, MatchScore score) {

        // ❌ NO strike rotation on wicket (handled by rules)
        if (ball.isWicket()) {
            if (ball.isOverCompleted()) {
                swap(score);
            }
            return;
        }

        // 🔁 NORMAL RUN ROTATION
        if (ball.getRuns() % 2 == 1) {
            swap(score);
        }

        // 🔁 OVER END ROTATION
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
