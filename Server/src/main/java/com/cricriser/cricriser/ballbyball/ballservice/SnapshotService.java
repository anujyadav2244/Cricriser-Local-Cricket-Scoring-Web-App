package com.cricriser.cricriser.ballbyball.ballservice;

import org.springframework.stereotype.Service;

import com.cricriser.cricriser.ballbyball.BallByBall;

@Service
public class SnapshotService {

    public void updateSnapshot(BallByBall ball) {

        // ✔ Correct over calculation
        double overs
                = ball.getOver()
                + ((ball.getBall() - 1) / 6.0);

        ball.setOversAtBall(overs);

        // ✔ Accumulative logic must be handled elsewhere
        ball.setTotalRunsAtBall(
                ball.getRuns() + ball.getExtraRuns()
        );

        ball.setTotalWicketsAtBall(
                ball.isWicket() ? 1 : 0
        );

        ball.setTimestamp(System.currentTimeMillis());
    }
}
