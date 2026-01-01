package com.cricriser.cricriser.ballbyball.ballservice;

import org.springframework.stereotype.Service;

import com.cricriser.cricriser.ballbyball.BallByBall;

@Service
public class ExtraService {

    public void applyExtras(BallByBall ball) {

        if (ball.getExtraType() == null) {
            ball.setLegalBall(true);
            return;
        }

        switch (ball.getExtraType()) {

            case "WIDE":
                ball.setLegalBall(false);
                ball.setExtraRuns(ball.getExtraRuns() + 1);
                break;

            case "NO_BALL":
                ball.setLegalBall(false);
                ball.setExtraRuns(ball.getExtraRuns() + 1);
                ball.setFreeHit(true);
                break;

            case "BYE":
            case "LEG_BYE":
                ball.setLegalBall(true);
                break;

            default:
                ball.setLegalBall(true);
        }
    }
}
