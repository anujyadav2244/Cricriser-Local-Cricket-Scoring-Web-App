package com.cricriser.cricriser.ballbyball;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cricriser.cricriser.ballbyball.ballservice.BallByBallService;


@RestController
@RequestMapping("/api/ball-by-ball")
@CrossOrigin
public class BallByBallController {

    @Autowired
    private BallByBallService ballByBallService;


    
    // ================== RECORD A BALL ==================
    @PostMapping("/record")
    public ResponseEntity<?> recordBall(@RequestBody BallByBall ball) {

        try {
            BallByBall savedBall = ballByBallService.recordBall(ball);
            return ResponseEntity.ok(savedBall);

        } catch (RuntimeException ex) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(ex.getMessage());
        }
    }


    
    // ================== GET ALL BALLS OF MATCH ==================
    @GetMapping("/match/{matchId}")
    public ResponseEntity<?> getMatchBalls(@PathVariable String matchId) {

        return ResponseEntity.ok(
                ballByBallService.getBallsByMatch(matchId)
        );
    }

    // ================== GET BALLS BY INNINGS ==================
    @GetMapping("/match/{matchId}/innings/{innings}")
    public ResponseEntity<?> getInningsBalls(
            @PathVariable String matchId,
            @PathVariable int innings) {

        return ResponseEntity.ok(
                ballByBallService.getBallsByInnings(matchId, innings)
        );
    }

    // ================== RESET BALLS (ADMIN / DEBUG) ==================
    @DeleteMapping("/match/{matchId}")
    public ResponseEntity<?> deleteBalls(@PathVariable String matchId) {

        ballByBallService.deleteBallsByMatch(matchId);
        return ResponseEntity.ok("Ball-by-ball data cleared");
    }
}
