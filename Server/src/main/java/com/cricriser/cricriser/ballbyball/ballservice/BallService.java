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

        // ❌ boundaryRuns cannot exist without boundary
        if (!ball.isBoundary() && ball.getBoundaryRuns() > 0) {
            throw new RuntimeException(
                    "boundaryRuns cannot be greater than 0 when boundary is false"
            );
        }

        if (!ball.isBoundary()) {
            return;
        }

        // Boundary must be 4 or 6
        if (ball.getBoundaryRuns() != 4 && ball.getBoundaryRuns() != 6) {
            throw new RuntimeException("Invalid boundary runs");
        }

        // ❌ Overthrow not allowed with six
        if (ball.getBoundaryRuns() == 6 && ball.isOverthrowBoundary()) {
            throw new RuntimeException(
                    "Overthrow cannot occur on a six"
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

    public int calculateTotalRuns(BallByBall ball) {

        int total = 0;

        // ================= EXTRAS =================
        if ("NO_BALL".equalsIgnoreCase(ball.getExtraType())
                || "WIDE".equalsIgnoreCase(ball.getExtraType())) {

            total += ball.getExtraRuns(); // +1 no-ball / wide
        }

        // ================= RUNNING RUNS =================
        total += ball.getRunningRuns(); // ✅ THIS WAS MISSING

        // ================= BOUNDARY =================
        if (ball.isBoundary()) {
            total += ball.getBoundaryRuns();
            return total;
        }

        // ================= BAT RUNS (NORMAL BALL) =================
        if (!"WIDE".equalsIgnoreCase(ball.getExtraType())) {
            total += ball.getRuns();
        }

        return total;
    }

    public void normalizeWicketState(BallByBall ball) {

        if (!ball.isWicket()) {
            ball.setWicketType(null);
            ball.setOutBatterId(null);
            ball.setRunOutEnd(null);
            return;
        }

        String wicketType = ball.getWicketType().toUpperCase();
        ball.setWicketType(wicketType);

        // ✅ RUN OUT → outBatterId MUST come from request
        if ("RUN_OUT".equals(wicketType)) {

            if (ball.getOutBatterId() == null) {
                throw new RuntimeException(
                        "outBatterId is mandatory for Run Out"
                );
            }

            return; // 🚨 VERY IMPORTANT
        }

        // ✅ NON–RUN OUT → striker is ALWAYS out
        ball.setOutBatterId(null); // will be set later from score
        ball.setRunOutEnd(null);
    }

}
