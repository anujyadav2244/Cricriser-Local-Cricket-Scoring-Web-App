package com.cricriser.cricriser.ballbyball.ballservice;

import org.springframework.stereotype.Service;

import com.cricriser.cricriser.ballbyball.BallByBall;

@Service
public class SnapshotService {

    public void updateSnapshot(BallByBall ball) {

        ball.setOversAtBall(
            ball.getOver() + (ball.getBall() - 1) / 10.0
        );

        ball.setTotalRunsAtBall(
            ball.getRuns() + ball.getExtraRuns()
        );

        ball.setTotalWicketsAtBall(
            ball.isWicket() ? 1 : 0
        );

        ball.setTimestamp(System.currentTimeMillis());
    }
}

