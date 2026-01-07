package com.cricriser.cricriser.player.playerstats;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.cricriser.cricriser.ballbyball.BallByBall;
import com.cricriser.cricriser.ballbyball.ballservice.BallService;

@Service
public class PlayerStatsService {

    @Autowired
    private PlayerStatsRepository playerStatsRepository;

    @Autowired
    private BallService ballService;

    // ================= UPDATE CAREER STATS =================
    public void updatePlayerStats(BallByBall ball) {

        // ================= BATTER =================
        String batterId = ball.isWicket()
                ? ball.getOutBatterId()
                : ball.getBatterId();

        PlayerStats batter = getOrCreate(batterId);

// ✅ ADD ONLY BAT RUNS (NO WIDE, NO BYES)
        if (!"WIDE".equalsIgnoreCase(ball.getExtraType())
                && ball.getRuns() > 0) {

            batter.setRunsScored(
                    batter.getRunsScored() + ball.getRuns()
            );
        }

// ✅ BALLS FACED
// - Legal ball
// - No ball
// - NOT wide
        if ((ball.isLegalBall() || "NO_BALL".equalsIgnoreCase(ball.getExtraType()))
                && !"WIDE".equalsIgnoreCase(ball.getExtraType())) {

            batter.setBallsFaced(batter.getBallsFaced() + 1);
        }

// ✅ BOUNDARIES (ONLY OFF BAT)
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
        if (batter.getBallsFaced() > 0) {
            batter.setBattingStrikeRate(
                    (batter.getRunsScored() * 100.0)
                    / batter.getBallsFaced()
            );
        }

        playerStatsRepository.save(batter);

        // ================= BOWLER =================
        PlayerStats bowler = getOrCreate(ball.getBowlerId());

        if (ball.isLegalBall()) {
            bowler.setBallsBowled(bowler.getBallsBowled() + 1);
        }

        // ✅ RUNS CONCEDED (INCLUDING EXTRAS)
        bowler.setRunsConceded(
                bowler.getRunsConceded()
                + ballService.calculateTotalRuns(ball)
        );

        // ✅ EXTRAS
        if ("WIDE".equalsIgnoreCase(ball.getExtraType())) {
            bowler.setWides(bowler.getWides() + ball.getExtraRuns());
        }

        if ("NO_BALL".equalsIgnoreCase(ball.getExtraType())) {
            bowler.setNoBalls(bowler.getNoBalls() + 1);
        }

        // ✅ WICKETS
        if (ball.isWicket() && isBowlerWicket(ball.getWicketType())) {
            bowler.setWickets(bowler.getWickets() + 1);
        }

        if (bowler.getBallsBowled() > 0) {
            bowler.setEconomy(
                    (bowler.getRunsConceded() * 6.0)
                    / bowler.getBallsBowled()
            );
        }

        playerStatsRepository.save(bowler);
    }

    // ================= HELPER =================
    private PlayerStats getOrCreate(String playerId) {

        PlayerStats stats
                = playerStatsRepository.findByPlayerId(playerId);

        if (stats == null) {

            stats = new PlayerStats();
            stats.setPlayerId(playerId);

            // Batting
            stats.setRunsScored(0);
            stats.setBallsFaced(0);
            stats.setFours(0);
            stats.setSixes(0);
            stats.setHighestScore(0);

            // Bowling
            stats.setBallsBowled(0);
            stats.setRunsConceded(0);
            stats.setWickets(0);

            stats.setBattingStrikeRate(0);
            stats.setBowlingAverage(0);
            stats.setEconomy(0);

            playerStatsRepository.save(stats);
        }

        return stats;
    }

    // ================= WICKET TYPE =================
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

    public void incrementMatchIfNotExists(String playerId) {

        PlayerStats stats = getOrCreate(playerId);

        if (stats.getMatches() == 0) {
            stats.setMatches(1);
            playerStatsRepository.save(stats);
        }
    }

    public void incrementInningsIfFirstBall(
            String playerId,
            boolean isBatting,
            boolean isBowling
    ) {
        PlayerStats stats = getOrCreate(playerId);

        if (isBatting && stats.getBallsFaced() == 0) {
            stats.setInnings(stats.getInnings() + 1);
        }

        if (isBowling && stats.getBallsBowled() == 0) {
            stats.setInnings(stats.getInnings() + 1);
        }

        playerStatsRepository.save(stats);
    }

}
