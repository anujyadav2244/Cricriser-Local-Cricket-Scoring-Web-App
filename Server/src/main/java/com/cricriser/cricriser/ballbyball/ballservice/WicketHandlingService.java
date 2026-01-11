package com.cricriser.cricriser.ballbyball.ballservice;

import org.springframework.stereotype.Service;

import com.cricriser.cricriser.ballbyball.BallByBall;
import com.cricriser.cricriser.match.matchscoring.MatchScore;
import com.cricriser.cricriser.player.matchplayerstats.MatchPlayerStatsService;

@Service
public class WicketHandlingService {

    public void handleWicket(
            BallByBall ball,
            MatchScore score,
            MatchPlayerStatsService matchStatsService
    ) {

        if (!ball.isWicket()) {
            return;
        }

        String wicketType = ball.getWicketType();

        // RETIRED HURT → NOT OUT
        if ("RETIRED_HURT".equalsIgnoreCase(wicketType)) {
            return;
        }

        String outBatterId;

        // ================= RUN OUT =================
        if ("RUN_OUT".equalsIgnoreCase(wicketType)) {

            outBatterId = ball.getOutBatterId();

            String striker = score.getStrikerId();
            String nonStriker = score.getNonStrikerId();

            // 🔒 FINAL SAFETY CHECK (NO INFERENCE)
            if (!outBatterId.equals(striker)
                    && !outBatterId.equals(nonStriker)) {

                throw new RuntimeException(
                        "Run out batter must be striker or non-striker"
                );
            }

            // 👉 DO NOT set striker here
            // 👉 BattingStateService will handle replacement
            matchStatsService.markBatterOut(
                    ball.getMatchId(),
                    outBatterId,
                    wicketType,
                    ball.getBowlerId(),
                    ball.getFielderId()
            );

            return;
        }

        // ================= OTHER WICKETS =================
        outBatterId = score.getStrikerId();
        ball.setOutBatterId(outBatterId);

        matchStatsService.markBatterOut(
                ball.getMatchId(),
                outBatterId,
                wicketType,
                ball.getBowlerId(),
                ball.getFielderId()
        );
    }

}
