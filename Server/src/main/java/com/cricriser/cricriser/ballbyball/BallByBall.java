package com.cricriser.cricriser.ballbyball;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "ball_by_ball")
public class BallByBall {

    @Id
    private String id; // Match context
    private String leagueId;
    private String matchId;
    private String battingTeamId;
    private String bowlingTeamId; // Innings & ball info
    private int innings;
    private int over;
    private int ball;
    private long ballSequence; // global ball count

    // Players
    private String batterId;
    private String nonStrikerId;
    private String bowlerId;
    // Runs & extras
    private int runs;
    private String extraType;

    // WIDE, NO_BALL, BYE, LEG_BYE
    private int extraRuns;

    // Ball state
    private boolean legalBall;
    private boolean freeHit;
    private boolean overCompleted;

    // Wicket info
    private boolean isWicket;
    private String wicketType;
    private String outBatterId;
    private String newBatterId;
    private String runOutEnd;

    // STRIKER / NON_STRIKER
    private String fielderId;
    // Bowler events
    private boolean bowlerInjured;
    private boolean highFullToss;

    // Match snapshot
    private int totalRunsAtBall;
    private int totalWicketsAtBall;
    private double oversAtBall;

    // Meta
    private String phase; // POWERPLAY, MIDDLE, DEATH 
    private String commentary;
    private long timestamp;

    // Validation
    private boolean validDelivery = true;
    private String invalidReason;

    // Boundary info
    private boolean boundary;     // true only if boundary scored
    private int boundaryRuns;     // 4 or 6 (valid only if boundary = true)
    private boolean overthrowBoundary; // true if boundary came via overthrow

    private String newBowlerId; // ONLY for first ball of new over

    private int runningRuns; // runs completed by batters


}
