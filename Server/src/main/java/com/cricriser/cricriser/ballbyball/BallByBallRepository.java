package com.cricriser.cricriser.ballbyball;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface BallByBallRepository extends MongoRepository<BallByBall, String> {

    List<BallByBall> findByMatchIdOrderByOverAscBallAsc(String matchId);

    void deleteByMatchId(String matchId);

    boolean existsByMatchIdAndInningsAndOverAndBallAndIsWicketTrue(
            String matchId, int innings, int over, int ball
    );

    List<BallByBall> findByMatchIdAndInningsAndBowlerId(
            String matchId, int innings, String bowlerId
    );

    List<BallByBall> findByMatchIdAndInningsOrderByOverAscBallAsc(
            String matchId, int innings
    );

    // ⭐ REQUIRED FOR BALL NUMBER LOGIC
    BallByBall findTopByMatchIdAndInningsOrderByBallSequenceDesc(
            String matchId, int innings
    );
}
