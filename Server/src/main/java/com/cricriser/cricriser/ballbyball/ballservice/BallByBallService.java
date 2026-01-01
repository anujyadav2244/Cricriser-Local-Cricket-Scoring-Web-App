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

        // =====================================================
        // 1️⃣ FETCH & VALIDATE MATCH STATE
        // =====================================================
        MatchScore score = matchScoreRepository.findByMatchId(ball.getMatchId());
        if (score == null) {
            throw new RuntimeException("Match score not found");
        }

        matchScoreUpdateService.validateBeforeBall(
                ball.getMatchId(),
                ball.getInnings()
        );

        // =====================================================
        // 2️⃣ HANDLE NEW OVER (BOWLER SELECTION)
        // =====================================================
        playerValidationService.validateAndSetNewBowler(ball, score);

        // =====================================================
        // 3️⃣ FREEZE PRE-BALL LIVE STATE
        // =====================================================
        ball.setBattingTeamId(score.getBattingTeamId());
        ball.setBatterId(score.getStrikerId());
        ball.setNonStrikerId(score.getNonStrikerId());
        ball.setBowlerId(score.getCurrentBowlerId());

        // =====================================================
        // 4️⃣ ASSIGN OVER / BALL NUMBER
        // =====================================================
        ballService.assignBallNumber(ball);

        // =====================================================
        // 5️⃣ VALIDATIONS BASED ON ASSIGNED BALL
        // =====================================================
        playerValidationService.validateBowler(ball);
        playerValidationService.validateBatters(ball, score);

        // =====================================================
        // 6️⃣ APPLY BALL EVENTS (NO STRIKE ROTATION)
        // =====================================================
        extraService.applyExtras(ball);

        // 👉 Decide WHO is out (outBatterId)
        wicketService.handleWicket(ball, score, matchPlayerStatsService);

        // 👉 Validate new batter if wicket
        playerValidationService.validateNewBatter(ball, score);

        // =====================================================
        // 7️⃣ APPLY BATTING STATE CHANGES
        //     (out list, yet-to-bat, batter replacement)
        // =====================================================
        battingStateService.applyWicketState(ball, score);

        // =====================================================
        // 8️⃣ OVER COMPLETION LOGIC
        // =====================================================
        overService.checkOverCompletion(ball, score);

        // =====================================================
        // 9️⃣ STRIKE ROTATION (FINAL & ONLY PLACE)
        // =====================================================
        strikeService.rotateStrike(ball, score);

        // =====================================================
        // 🔟 PERSIST BALL & MATCH STATE
        // =====================================================
        BallByBall savedBall = ballRepo.save(ball);
        matchScoreRepository.save(score);

        // =====================================================
        // 1️⃣1️⃣ UPDATE MATCH SCORE (RUNS / OVERS / WICKETS)
        // =====================================================
        matchScoreUpdateService.updateMatchScore(savedBall, score);

        // =====================================================
        // 1️⃣2️⃣ UPDATE PLAYER STATS
        // =====================================================
        playerStatsService.updatePlayerStats(savedBall);
        matchPlayerStatsService.updateMatchPlayerStats(savedBall, score);

        // =====================================================
        // 1️⃣3️⃣ SNAPSHOT (OPTIONAL)
        // =====================================================
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
