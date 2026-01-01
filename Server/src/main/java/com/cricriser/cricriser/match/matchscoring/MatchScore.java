package com.cricriser.cricriser.match.matchscoring;

import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "match_scoring")
public class MatchScore {

    @Id
    private String id;

    private String matchId; // Reference to MatchSchedule
    private String leagueId;     // Reference to League

    private String team1Id;
    private int team1Runs;
    private int team1Wickets;
    private double team1Overs;
    private double team1RunRate;
    private int team1Extras;

    private String team2Id;
    private int team2Runs;
    private int team2Wickets;
    private double team2Overs;
    private double team2RunRate;
    private int team2Extras;

    private String tossWinner;
    private String tossDecision; // "Bat" or "Bowl"

    private String matchStatus; // "Not Started", "In Progress", "Completed"

    private String matchWinner; // auto-calculated

    private String result;      // auto-calculated
    private String playerOfTheMatch;

    private int innings; // 1 or 2 =
    private String strikerId;
    private String nonStrikerId;


    private String battingTeamId;
    private String bowlingTeamId;

    private List<String> team1YetToBat;
    private List<String> team2YetToBat;

    private List<String> team1OutBatters;
    private List<String> team2OutBatters;

    private List<String> team1PlayingXI; // Player IDs
    private List<String> team2PlayingXI; // Player IDs

    private String lastOverBowlerId;
    private int totalOvers; // match overs (e.g. 20)

    private boolean freeHit = false;

    private boolean firstInningsCompleted;
    private boolean secondInningsCompleted;

    private String currentBowlerId;
    private int beamerCountThisOver = 0;

    private boolean forceBowlerChange = false;



}
