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

        //  FETCH MATCH STATE
        MatchScore score = matchScoreRepository.findByMatchId(ball.getMatchId());
        if (score == null) {
            throw new RuntimeException("Match score not found");
        }

        //  PRE-BALL VALIDATION (match / innings level)
        matchScoreUpdateService.validateBeforeBall(
                ball.getMatchId(),
                ball.getInnings()
        );

        //  HANDLE NEW OVER (SET NEW BOWLER IF REQUIRED)
        playerValidationService.validateAndSetNewBowler(ball, score);

        //  FREEZE CURRENT MATCH STATE INTO BALL
        ball.setBattingTeamId(score.getBattingTeamId());
        ball.setBatterId(score.getStrikerId());
        ball.setNonStrikerId(score.getNonStrikerId());
        ball.setBowlerId(score.getCurrentBowlerId());

        //  NORMALIZE + BASIC BALL VALIDATION
       
        ballService.validate(ball);

        //  ASSIGN OVER / BALL NUMBER
        ballService.assignBallNumber(ball);

        //  PLAYER VALIDATIONS (CURRENT BATTERS + BOWLER)
        playerValidationService.validateBowler(ball);
        playerValidationService.validateBatters(ball, score);

        //  🔥 VALIDATE NEW BATTER (BEFORE ANY MUTATION)
        playerValidationService.validateNewBatter(ball, score);

         ballService.normalizeWicketState(ball);
        // APPLY EXTRAS (NO STATE CHANGE)
        extraService.applyExtras(ball);

        //  HANDLE WICKET (SETS outBatterId, updates score position)
        wicketService.handleWicket(ball, score, matchPlayerStatsService);

        //  APPLY BATTING STATE (OUT / YET-TO-BAT / REPLACEMENT)
        battingStateService.applyWicketState(ball, score);

        //  OVER COMPLETION CHECK
        overService.checkOverCompletion(ball, score);

        //  STRIKE ROTATION
        strikeService.rotateStrike(ball, score);

        //  SAVE BALL + MATCH SCORE
        BallByBall savedBall = ballRepo.save(ball);
        matchScoreRepository.save(score);

        //  UPDATE MATCH SCORE (RUNS / WICKETS / OVERS)
        matchScoreUpdateService.updateMatchScore(savedBall, score);

        //  PLAYER & MATCH STATS
        playerStatsService.updatePlayerStats(savedBall);
        matchPlayerStatsService.updateMatchPlayerStats(savedBall, score);

        //  SNAPSHOT (AUDIT)
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
