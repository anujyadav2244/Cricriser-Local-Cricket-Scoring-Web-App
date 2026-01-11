package com.cricriser.cricriser.ballbyball.ballservice;

import java.util.List;

import org.springframework.stereotype.Service;

import com.cricriser.cricriser.ballbyball.BallByBall;
import com.cricriser.cricriser.match.matchscoring.MatchScore;

@Service
public class BattingStateService {

    public void applyWicketState(BallByBall ball, MatchScore score) {

        if (!ball.isWicket()) {
            return;
        }

        if ("RETIRED_HURT".equalsIgnoreCase(ball.getWicketType())) {
            return;
        }

        String outBatterId = ball.getOutBatterId();
        String newBatterId = ball.getNewBatterId();

        if (outBatterId == null || newBatterId == null) {
            throw new RuntimeException(
                    "outBatterId and newBatterId are mandatory"
            );
        }

        boolean team1Batting
                = score.getBattingTeamId().equals(score.getTeam1Id());

        List<String> outBatters = team1Batting
                ? score.getTeam1OutBatters()
                : score.getTeam2OutBatters();

        List<String> yetToBat = team1Batting
                ? score.getTeam1YetToBat()
                : score.getTeam2YetToBat();

        // ✅ mark out batter once
        if (!outBatters.contains(outBatterId)) {
            outBatters.add(outBatterId);
        }

        // ✅ remove new batter from yet-to-bat
        yetToBat.remove(newBatterId);

        replaceBatter(ball, score, outBatterId, newBatterId);
    }

    // =====================================================
    // 🔥 FINAL & CORRECT RUN-OUT LOGIC
    // =====================================================
    private void replaceBatter(
            BallByBall ball,
            MatchScore score,
            String outBatterId,
            String newBatterId
    ) {

        boolean lastBallOfOver = ball.isOverCompleted();

        String striker = score.getStrikerId();
        String nonStriker = score.getNonStrikerId();

        // ================= RUN OUT =================
        if ("RUN_OUT".equalsIgnoreCase(ball.getWicketType())) {

            String survivingBatter;

            // 🔴 Identify surviving batter
            if (outBatterId.equals(striker)) {
                survivingBatter = nonStriker;
            } else if (outBatterId.equals(nonStriker)) {
                survivingBatter = striker;
            } else {
                throw new RuntimeException(
                        "outBatterId is not on crease"
                );
            }

            // 🔴 Place batters strictly by runOutEnd
            if ("STRIKER".equalsIgnoreCase(ball.getRunOutEnd())) {
                score.setStrikerId(newBatterId);
                score.setNonStrikerId(survivingBatter);
            } else if ("NON_STRIKER".equalsIgnoreCase(ball.getRunOutEnd())) {
                score.setNonStrikerId(newBatterId);
                score.setStrikerId(survivingBatter);
            } else {
                throw new RuntimeException("Invalid runOutEnd");
            }

            // 🔁 Last ball → swap AFTER placement
            if (lastBallOfOver) {
                swap(score);
            }

            return;
        }

        // ================= NON–RUN OUT WICKETS =================
        // Striker always out
        score.setStrikerId(newBatterId);

        if (lastBallOfOver) {
            swap(score);
        }
    }

    private void swap(MatchScore score) {
        String temp = score.getStrikerId();
        score.setStrikerId(score.getNonStrikerId());
        score.setNonStrikerId(temp);
    }
}
