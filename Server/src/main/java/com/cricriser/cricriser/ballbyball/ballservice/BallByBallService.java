package com.cricriser.cricriser.ballbyball.ballservice;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cricriser.cricriser.ballbyball.BallByBall;
import com.cricriser.cricriser.ballbyball.BallByBallRepository;
import com.cricriser.cricriser.match.matchscoring.MatchScore;
import com.cricriser.cricriser.match.matchscoring.MatchScoreRepository;
import com.cricriser.cricriser.match.matchscoring.MatchScoreUpdateService;
import com.cricriser.cricriser.player.matchplayerstats.MatchPlayerStatsService;
import com.cricriser.cricriser.player.playerstats.PlayerStatsService;

@Service
public class BallByBallService {

    @Autowired
    private BallByBallRepository ballRepo;

    @Autowired
    private BallService ballService;

    @Autowired
    private ExtraService extraService;

    @Autowired
    private WicketHandlingService wicketService;

    @Autowired
    private OverService overService;

    @Autowired
    private SnapshotService snapshotService;

    @Autowired
    private PlayerValidationService playerValidationService;

    @Autowired
    private StrikeRotationService strikeService;

    @Autowired
    private MatchScoreRepository matchScoreRepository;

    @Autowired
    private MatchScoreUpdateService matchScoreUpdateService;

    @Autowired
    private PlayerStatsService playerStatsService;

    @Autowired
    private MatchPlayerStatsService matchPlayerStatsService;

    @Autowired
    private BattingStateService battingStateService;

    // ================= RECORD A BALL =================
    @Transactional
    public BallByBall recordBall(BallByBall ball) {

        // 1️⃣ FETCH MATCH STATE
        MatchScore score = matchScoreRepository.findByMatchId(ball.getMatchId());
        if (score == null) {
            throw new RuntimeException("Match score not found");
        }

        // 2️⃣ PRE-BALL VALIDATION (match / innings level)
        matchScoreUpdateService.validateBeforeBall(
                ball.getMatchId(),
                ball.getInnings()
        );

        // 3️⃣ HANDLE NEW OVER (SET NEW BOWLER IF REQUIRED)
        playerValidationService.validateAndSetNewBowler(ball, score);

        // 4️⃣ FREEZE CURRENT MATCH STATE INTO BALL
        ball.setBattingTeamId(score.getBattingTeamId());
        ball.setBatterId(score.getStrikerId());
        ball.setNonStrikerId(score.getNonStrikerId());
        ball.setBowlerId(score.getCurrentBowlerId());

        // 5️⃣ NORMALIZE + BASIC BALL VALIDATION
        ballService.normalizeWicketState(ball);
        ballService.validate(ball);

        // 6️⃣ ASSIGN OVER / BALL NUMBER
        ballService.assignBallNumber(ball);

        // 7️⃣ PLAYER VALIDATIONS (CURRENT BATTERS + BOWLER)
        playerValidationService.validateBowler(ball);
        playerValidationService.validateBatters(ball, score);

        // 8️⃣ APPLY EXTRAS (NO STATE CHANGE)
        extraService.applyExtras(ball);

        // 9️⃣ 🔥 VALIDATE NEW BATTER (BEFORE ANY MUTATION)
        playerValidationService.validateNewBatter(ball, score);

        // 🔟 HANDLE WICKET (SETS outBatterId, updates score position)
        wicketService.handleWicket(ball, score, matchPlayerStatsService);

        // 1️⃣1️⃣ APPLY BATTING STATE (OUT / YET-TO-BAT / REPLACEMENT)
        battingStateService.applyWicketState(ball, score);

        // 1️⃣2️⃣ OVER COMPLETION CHECK
        overService.checkOverCompletion(ball, score);

        // 1️⃣3️⃣ STRIKE ROTATION
        strikeService.rotateStrike(ball, score);

        // 1️⃣4️⃣ SAVE BALL + MATCH SCORE
        BallByBall savedBall = ballRepo.save(ball);
        matchScoreRepository.save(score);

        // 1️⃣5️⃣ UPDATE MATCH SCORE (RUNS / WICKETS / OVERS)
        matchScoreUpdateService.updateMatchScore(savedBall, score);

        // 1️⃣6️⃣ PLAYER & MATCH STATS
        playerStatsService.updatePlayerStats(savedBall);
        matchPlayerStatsService.updateMatchPlayerStats(savedBall, score);

        // 1️⃣7️⃣ SNAPSHOT (AUDIT)
        snapshotService.updateSnapshot(savedBall);

        return savedBall;
    }

    // ================= GET BALLS =================
    public List<BallByBall> getBallsByMatch(String matchId) {
        return ballRepo.findByMatchIdOrderByOverAscBallAsc(matchId);
    }

    public List<BallByBall> getBallsByInnings(String matchId, int innings) {
        return ballRepo.findByMatchIdAndInningsOrderByOverAscBallAsc(
                matchId, innings
        );
    }

    // ================= DELETE =================
    public void deleteBallsByMatch(String matchId) {
        ballRepo.deleteByMatchId(matchId);
    }
}
