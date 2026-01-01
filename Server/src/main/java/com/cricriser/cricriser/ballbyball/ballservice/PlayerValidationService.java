package com.cricriser.cricriser.ballbyball.ballservice;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.cricriser.cricriser.ballbyball.BallByBall;
import com.cricriser.cricriser.ballbyball.BallByBallRepository;
import com.cricriser.cricriser.match.matchscoring.MatchScore;
import com.cricriser.cricriser.player.matchplayerstats.MatchPlayerStatsService;

@Service
public class PlayerValidationService {

    @Autowired
    private BallByBallRepository ballRepo;

    @Autowired
    private MatchPlayerStatsService matchPlayerStatsService;

    // ===================== BOWLER VALIDATION =====================
    public void validateBowler(BallByBall ball) {

        BallByBall lastBall
                = ballRepo.findTopByMatchIdAndInningsOrderByBallSequenceDesc(
                        ball.getMatchId(), ball.getInnings()
                );

        // First ball of innings
        if (lastBall == null) {
            return;
        }

        if (ball.getBowlerId() == null) {
            throw new RuntimeException("Bowler not set for this ball");
        }

        // Same over → same bowler only
        if (ball.getOver() == lastBall.getOver()) {

            if (!ball.getBowlerId().equals(lastBall.getBowlerId())) {
                throw new RuntimeException(
                        "Same bowler must complete the over"
                );
            }
        }
    }

    // ===================== CURRENT BATTERS VALIDATION =====================
    public void validateBatters(BallByBall ball, MatchScore score) {

        String striker = score.getStrikerId();
        String nonStriker = score.getNonStrikerId();

        if (striker == null || nonStriker == null) {
            throw new RuntimeException("Batters not set");
        }

        if (striker.equals(nonStriker)) {
            throw new RuntimeException(
                    "Striker and non-striker cannot be same"
            );
        }

        boolean isTeam1Batting
                = score.getBattingTeamId().equals(score.getTeam1Id());

        List<String> playingXI = isTeam1Batting
                ? score.getTeam1PlayingXI()
                : score.getTeam2PlayingXI();

        List<String> outBatters = isTeam1Batting
                ? score.getTeam1OutBatters()
                : score.getTeam2OutBatters();

        // Must be in Playing XI
        if (!playingXI.contains(striker)) {
            throw new RuntimeException(
                    "Striker is not part of Playing XI"
            );
        }

        if (!playingXI.contains(nonStriker)) {
            throw new RuntimeException(
                    "Non-striker is not part of Playing XI"
            );
        }

        // Must NOT be out
        if (outBatters.contains(striker)) {
            throw new RuntimeException(
                    "Striker is already out"
            );
        }

        if (outBatters.contains(nonStriker)) {
            throw new RuntimeException(
                    "Non-striker is already out"
            );
        }

        // ❌ DO NOT check yetToBat here
    }

    // ===================== NEW BATTER VALIDATION (ON WICKET) =====================
    public void validateNewBatter(BallByBall ball, MatchScore score) {

        // -------- NO WICKET --------
        if (!ball.isWicket()) {
            if (ball.getNewBatterId() != null) {
                throw new RuntimeException(
                        "New batter allowed only when wicket falls"
                );
            }
            return;
        }

        String striker = score.getStrikerId();
        String nonStriker = score.getNonStrikerId();

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

        // -------- RUN OUT VALIDATION --------
        if ("RUN_OUT".equalsIgnoreCase(ball.getWicketType())) {

            if (ball.getOutBatterId() == null
                    || ball.getRunOutEnd() == null) {
                throw new RuntimeException(
                        "Run out requires outBatterId and runOutEnd"
                );
            }

            String out = ball.getOutBatterId();

            if (!out.equals(striker) && !out.equals(nonStriker)) {
                throw new RuntimeException(
                        "Out batter must be striker or non-striker"
                );
            }
        }

        // -------- NORMAL WICKET --------
        // striker auto-out → no outBatterId needed
        // -------- NEW BATTER REQUIRED --------
        String newBatter = ball.getNewBatterId();

        if (newBatter == null || newBatter.isBlank()) {
            throw new RuntimeException(
                    "New batter is mandatory after wicket"
            );
        }

        if (!playingXI.contains(newBatter)) {
            throw new RuntimeException(
                    "New batter must belong to Playing XI"
            );
        }

        if (!yetToBat.contains(newBatter)) {
            throw new RuntimeException(
                    "New batter must be from Yet-To-Bat list"
            );
        }

        if (outBatters.contains(newBatter)) {
            throw new RuntimeException(
                    "New batter is already out"
            );
        }

        if (newBatter.equals(striker)
                || newBatter.equals(nonStriker)) {
            throw new RuntimeException(
                    "New batter cannot be current batter"
            );
        }
    }

    public void validateAndSetNewBowler(BallByBall ball, MatchScore score) {

        if (score.getCurrentBowlerId() != null) {
            return;
        }

        if (ball.getNewBowlerId() == null || ball.getNewBowlerId().isBlank()) {
            throw new RuntimeException(
                    "New bowler must be provided at start of new over"
            );
        }

        if (ball.getNewBowlerId().equals(score.getLastOverBowlerId())) {
            throw new RuntimeException(
                    "Bowler cannot bowl consecutive overs"
            );
        }

        score.setCurrentBowlerId(ball.getNewBowlerId());
    }

}
