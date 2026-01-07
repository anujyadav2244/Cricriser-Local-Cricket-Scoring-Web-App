package com.cricriser.cricriser.player.matchplayerstats;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.cricriser.cricriser.ballbyball.BallByBall;
import com.cricriser.cricriser.ballbyball.ballservice.BallService;
import com.cricriser.cricriser.ballbyball.ballservice.OverService;
import com.cricriser.cricriser.match.matchscoring.MatchScore;

@Service
public class MatchPlayerStatsService {

    @Autowired
    private MatchPlayerStatsRepository repo;

    @Autowired
    private OverService overService;

    @Autowired
    private BallService ballService;

    public void updateMatchPlayerStats(BallByBall ball, MatchScore score) {

        // ================= BATTER =================
        MatchPlayerStats batter
                = getOrCreate(ball.getMatchId(), ball.getBatterId());

// ✅ BAT RUNS ONLY
        if (!"WIDE".equalsIgnoreCase(ball.getExtraType())
                && ball.getRuns() > 0) {

            batter.setRuns(
                    batter.getRuns() + ball.getRuns()
            );
        }

// ✅ BALLS FACED
        if ((ball.isLegalBall() || "NO_BALL".equalsIgnoreCase(ball.getExtraType()))
                && !"WIDE".equalsIgnoreCase(ball.getExtraType())) {

            batter.setBalls(batter.getBalls() + 1);
        }

// ✅ BOUNDARIES OFF BAT
        if (ball.isBoundary()
                && !"WIDE".equalsIgnoreCase(ball.getExtraType())
                && ball.getRuns() > 0) {

            if (ball.getBoundaryRuns() == 4) {
                batter.setFours(batter.getFours() + 1);
            }
            if (ball.getBoundaryRuns() == 6) {
                batter.setSixes(batter.getSixes() + 1);
            }
        }

// ✅ STRIKE RATE
        if (batter.getBalls() > 0) {
            batter.setStrikeRate(
                    (batter.getRuns() * 100.0)
                    / batter.getBalls()
            );
        }

        repo.save(batter);

        // ================= BOWLER =================
        MatchPlayerStats bowler
                = getOrCreate(ball.getMatchId(), ball.getBowlerId());

        if (ball.isLegalBall()) {
            bowler.setBallsBowled(bowler.getBallsBowled() + 1);
            bowler.setOvers(
                    overService.calculateOversFromBalls(
                            bowler.getBallsBowled()
                    )
            );
        }

        bowler.setRunsConceded(
                bowler.getRunsConceded()
                + ballService.calculateTotalRuns(ball)
        );

        if ("WIDE".equalsIgnoreCase(ball.getExtraType())) {
            bowler.setWides(bowler.getWides() + ball.getExtraRuns());
        }

        if ("NO_BALL".equalsIgnoreCase(ball.getExtraType())) {
            bowler.setNoBalls(bowler.getNoBalls() + 1);
        }

        if (ball.isWicket() && isBowlerWicket(ball.getWicketType())) {
            bowler.setWickets(bowler.getWickets() + 1);
        }

        if (bowler.getBallsBowled() > 0) {
            bowler.setEconomy(
                    (bowler.getRunsConceded() * 6.0)
                    / bowler.getBallsBowled()
            );
        }

        repo.save(bowler);
    }

    // ---------- HELPERS ----------
    public MatchPlayerStats getOrCreate(
            String matchId,
            String playerId
    ) {
        return repo.findByMatchIdAndPlayerId(matchId, playerId)
                .orElseGet(() -> {
                    MatchPlayerStats stats = new MatchPlayerStats();
                    stats.setMatchId(matchId);
                    stats.setPlayerId(playerId);
                    stats.setRuns(0);
                    stats.setBalls(0);
                    stats.setFours(0);
                    stats.setSixes(0);
                    stats.setOut(false);
                    stats.setOvers(0);
                    stats.setBallsBowled(0);
                    stats.setRunsConceded(0);
                    stats.setWickets(0);
                    stats.setMaidens(0);
                    stats.setEconomy(0);
                    stats.setWides(0);
                    stats.setNoBalls(0);
                    return repo.save(stats);
                });
    }

    private boolean isBowlerWicket(String wicketType) {
        if (wicketType == null) {
            return false;
        }

        return switch (wicketType.toUpperCase()) {
            case "BOWLED", "CAUGHT", "LBW", "STUMPED", "HIT_WICKET", "HIT_THE_BALL_TWICE" ->
                true;
            default ->
                false;
        };
    }

    // private double calculateOvers(int balls) {
    //     int overs = balls / 6;
    //     int remBalls = balls % 6;
    //     return overs + (remBalls / 10.0);
    // }
    public void createIfNotExists(String matchId, String playerId) {

        repo.findByMatchIdAndPlayerId(matchId, playerId)
                .orElseGet(() -> {
                    MatchPlayerStats stats = new MatchPlayerStats();
                    stats.setMatchId(matchId);
                    stats.setPlayerId(playerId);
                    stats.setRuns(0);
                    stats.setBalls(0);
                    stats.setWickets(0);
                    stats.setRunsConceded(0);
                    stats.setBallsBowled(0);
                    return repo.save(stats);
                });
    }

    public void markBatterOut(
            String matchId,
            String batterId,
            String wicketType,
            String bowlerId,
            String fielderId
    ) {
        MatchPlayerStats stats = getOrCreate(matchId, batterId);

        stats.setOut(true);
        stats.setDismissalType(wicketType);

        // ✅ Bowler credited ONLY for bowler wickets
        if (isBowlerWicket(wicketType)) {
            stats.setBowlerId(bowlerId);
        } else {
            stats.setBowlerId(null);
        }

        // ✅ Fielder only for applicable dismissals
        stats.setFielderId(fielderId);

        repo.save(stats);
    }

}
