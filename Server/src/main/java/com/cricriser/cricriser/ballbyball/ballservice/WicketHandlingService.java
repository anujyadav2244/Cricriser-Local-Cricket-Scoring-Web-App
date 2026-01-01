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

        String wicketType = ball.getWicketType().toUpperCase();

        // 🟡 RETIRED HURT → NOT OUT
        if (wicketType.equals("RETIRED_HURT")) {
            return;
        }

        String outBatterId = resolveOutBatter(ball, score);
        ball.setOutBatterId(outBatterId);

        // 🟢 Update Match Player Stats
        matchStatsService.markBatterOut(
                ball.getMatchId(),
                outBatterId,
                wicketType,
                ball.getBowlerId(),
                ball.getFielderId()
        );

        // 🟢 Remove from Yet-To-Bat already done earlier
    }

    private String resolveOutBatter(BallByBall ball, MatchScore score) {

        if (ball.getWicketType().equalsIgnoreCase("RUN_OUT")) {
            return ball.getOutBatterId();
        }

        return score.getStrikerId();
    }


}
