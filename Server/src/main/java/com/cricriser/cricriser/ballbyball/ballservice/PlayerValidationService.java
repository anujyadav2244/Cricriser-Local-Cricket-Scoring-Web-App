package com.cricriser.cricriser.ballbyball.ballservice;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.cricriser.cricriser.ballbyball.BallByBall;
import com.cricriser.cricriser.ballbyball.BallByBallRepository;
import com.cricriser.cricriser.match.matchscoring.MatchScore;

@Service
public class PlayerValidationService {

    @Autowired
    private BallByBallRepository ballRepo;

    // =====================================================
    // 1️⃣ BOWLER VALIDATION
    // =====================================================
    public void validateBowler(BallByBall ball) {

        BallByBall lastBall
                = ballRepo.findTopByMatchIdAndInningsOrderByBallSequenceDesc(
                        ball.getMatchId(), ball.getInnings()
                );

        // First ball of innings
        if (lastBall == null) {
            if (ball.getBowlerId() == null) {
                throw new RuntimeException("Bowler must be set for first ball");
            }
            return;
        }

        if (ball.getBowlerId() == null) {
            throw new RuntimeException("Bowler not set");
        }

        // Same over → same bowler
        if (ball.getOver() == lastBall.getOver()) {
            if (!ball.getBowlerId().equals(lastBall.getBowlerId())) {
                throw new RuntimeException(
                        "Same bowler must complete the over"
                );
            }
        }
    }

    // =====================================================
    // 2️⃣ CURRENT BATTERS VALIDATION
    // =====================================================
    public void validateBatters(BallByBall ball, MatchScore score) {

        String striker = score.getStrikerId();
        String nonStriker = score.getNonStrikerId();

        if (striker == null || nonStriker == null) {
            throw new RuntimeException("Striker / Non-striker not set");
        }

        if (striker.equals(nonStriker)) {
            throw new RuntimeException("Striker and non-striker cannot be same");
        }

        boolean team1Batting
                = score.getBattingTeamId().equals(score.getTeam1Id());

        List<String> playingXI = team1Batting
                ? score.getTeam1PlayingXI()
                : score.getTeam2PlayingXI();

        List<String> outBatters = team1Batting
                ? score.getTeam1OutBatters()
                : score.getTeam2OutBatters();

        if (!playingXI.contains(striker)) {
            throw new RuntimeException("Striker not in Playing XI");
        }

        if (!playingXI.contains(nonStriker)) {
            throw new RuntimeException("Non-striker not in Playing XI");
        }

        // ✅ NORMAL BALL
        if (!ball.isWicket()) {

            if (outBatters.contains(striker)) {
                throw new RuntimeException("Striker is already out");
            }

            if (outBatters.contains(nonStriker)) {
                throw new RuntimeException("Non-striker is already out");
            }
        }
    }

    // =====================================================
    // 3️⃣ NEW BATTER VALIDATION (ON WICKET)
    // =====================================================
    public void validateNewBatter(BallByBall ball, MatchScore score) {

        // -------------------------------------------------
        // NO WICKET → new batter NOT allowed
        // -------------------------------------------------
        if (!ball.isWicket()) {
            if (ball.getNewBatterId() != null) {
                throw new RuntimeException(
                        "New batter allowed only when wicket falls"
                );
            }
            return;
        }

        boolean team1Batting
                = score.getBattingTeamId().equals(score.getTeam1Id());

        List<String> playingXI = team1Batting
                ? score.getTeam1PlayingXI()
                : score.getTeam2PlayingXI();

        List<String> yetToBat = team1Batting
                ? score.getTeam1YetToBat()
                : score.getTeam2YetToBat();

        List<String> outBatters = team1Batting
                ? score.getTeam1OutBatters()
                : score.getTeam2OutBatters();

        String wicketType = ball.getWicketType().toUpperCase();
        String newBatter = ball.getNewBatterId();

        // -------------------------------------------------
        // BASIC CHECKS
        // -------------------------------------------------
        if (newBatter == null || newBatter.isBlank()) {
            throw new RuntimeException("New batter is mandatory after wicket");
        }

        if (!playingXI.contains(newBatter)) {
            throw new RuntimeException("New batter must be from Playing XI");
        }

        if (!yetToBat.contains(newBatter)) {
            throw new RuntimeException("New batter must be from Yet-To-Bat list");
        }

        if (outBatters.contains(newBatter)) {
            throw new RuntimeException("New batter is already out");
        }

        // -------------------------------------------------
        // RUN OUT
        // -------------------------------------------------
        if ("RUN_OUT".equals(wicketType)) {

            String out = ball.getOutBatterId();

            if (out == null || ball.getRunOutEnd() == null) {
                throw new RuntimeException(
                        "Run out requires outBatterId and runOutEnd"
                );
            }

            if (!out.equals(score.getStrikerId())
                    && !out.equals(score.getNonStrikerId())) {
                throw new RuntimeException(
                        "Out batter must be striker or non-striker"
                );
            }

            if (newBatter.equals(out)) {
                throw new RuntimeException(
                        "New batter cannot be the out batter"
                );
            }

            return; // ✅ VALID RUN OUT
        }

        // -------------------------------------------------
        // NON-RUN OUT WICKETS (BOWLED, CAUGHT, LBW, STUMPED)
        // -------------------------------------------------
        // Only rule: new batter must NOT be the striker who got out
        if (newBatter.equals(score.getStrikerId())) {
            throw new RuntimeException(
                    "New batter cannot be the out batter"
            );
        }
    }

    // =====================================================
    // 4️⃣ NEW BOWLER VALIDATION (NEW OVER)
    // =====================================================
    public void validateAndSetNewBowler(
            BallByBall ball,
            MatchScore score
    ) {

        // Already set → nothing to do
        if (score.getCurrentBowlerId() != null) {
            return;
        }

        if (ball.getNewBowlerId() == null
                || ball.getNewBowlerId().isBlank()) {
            throw new RuntimeException(
                    "New bowler must be provided at start of new over"
            );
        }

        if (ball.getNewBowlerId()
                .equals(score.getLastOverBowlerId())) {
            throw new RuntimeException(
                    "Bowler cannot bowl consecutive overs"
            );
        }

        score.setCurrentBowlerId(ball.getNewBowlerId());
    }
}
