package com.cricriser.cricriser.ballbyball.ballservice;

import org.springframework.stereotype.Service;

import com.cricriser.cricriser.ballbyball.BallByBall;
import com.cricriser.cricriser.match.matchscoring.MatchScore;

@Service
public class OverService {

    public void checkOverCompletion(BallByBall ball, MatchScore score) {

        if (!ball.isLegalBall()) {
            return;
        }

        if (ball.getBall() == 6) {
            ball.setOverCompleted(true);

            score.setLastOverBowlerId(score.getCurrentBowlerId());
            score.setCurrentBowlerId(null); // force new bowler next ball
        } else {
            ball.setOverCompleted(false);
        }
    }

    public double calculateOversFromBalls(int balls) {
        int fullOvers = balls / 6;
        int remainingBalls = balls % 6;
        return fullOvers + (remainingBalls / 10.0);
    }

}
