package com.cricriser.cricriser.ballbyball.ballservice;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.cricriser.cricriser.ballbyball.BallByBall;
import com.cricriser.cricriser.ballbyball.BallByBallRepository;

@Service
public class BallService {

    @Autowired
    private BallByBallRepository ballRepo;

    public void validate(BallByBall ball) {

        if (ball == null) {
            throw new RuntimeException("Ball data is missing");
        }

        if (ball.getMatchId() == null || ball.getMatchId().isBlank()) {
            throw new RuntimeException("MatchId is missing");
        }

        if (ball.getInnings() <= 0) {
            throw new RuntimeException("Invalid innings");
        }

        if (ball.getBatterId() == null || ball.getBatterId().isBlank()) {
            throw new RuntimeException("Batter not set");
        }

        if (ball.getBowlerId() == null || ball.getBowlerId().isBlank()) {
            throw new RuntimeException("Bowler not set");
        }

        if (ball.getRuns() < 0) {
            throw new RuntimeException("Runs cannot be negative");
        }

        if (ball.getExtraRuns() < 0) {
            throw new RuntimeException("Extra runs cannot be negative");
        }

        // 🔥 Boundary validation (IMPORTANT)
        validateBoundary(ball);
    }

    // ================= BOUNDARY VALIDATION =================
    private void validateBoundary(BallByBall ball) {

        if (!ball.isBoundary()) {
            return;
        }

        if (ball.getBoundaryRuns() != 4
                && ball.getBoundaryRuns() != 6) {
            throw new RuntimeException("Invalid boundary runs");
        }

        if (ball.getRuns() != ball.getBoundaryRuns()) {
            throw new RuntimeException(
                    "Runs must match boundary runs"
            );
        }
    }
    
    public void assignBallNumber(BallByBall ball) {

        BallByBall lastBall
                = ballRepo.findTopByMatchIdAndInningsOrderByBallSequenceDesc(
                        ball.getMatchId(), ball.getInnings()
                );

        // FIRST BALL
        if (lastBall == null) {
            ball.setOver(1);
            ball.setBall(1);
            ball.setBallSequence(1);
            return;
        }

        // Global sequence always increases
        ball.setBallSequence(lastBall.getBallSequence() + 1);

        // Legal delivery
        if (lastBall.isLegalBall()) {

            if (lastBall.getBall() == 6) {
                ball.setOver(lastBall.getOver() + 1);
                ball.setBall(1);
            } else {
                ball.setOver(lastBall.getOver());
                ball.setBall(lastBall.getBall() + 1);
            }

        } else {
            // Illegal delivery → repeat ball
            ball.setOver(lastBall.getOver());
            ball.setBall(lastBall.getBall());
        }
    }

    public BallByBall ballSave(BallByBall ball) {
        return ballRepo.save(ball);
    }

    
}
