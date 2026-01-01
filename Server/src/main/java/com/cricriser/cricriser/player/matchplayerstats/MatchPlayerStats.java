package com.cricriser.cricriser.player.matchplayerstats;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "match_player_stats")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MatchPlayerStats {

    @Id
    private String id;

    private String matchId;
    private String teamId;
    private String playerId;

    // ================= BATTING (THIS MATCH) =================
    private int runs;        // e.g. 35
    private int balls;       // e.g. 49
    private int fours;
    private int sixes;
    private double strikeRate;


    private boolean out;     // true / false
    private String dismissalType; // RUN_OUT, CAUGHT, BOWLED
    private String bowlerId; // who dismissed
    private String fielderId;// if applicable

    // ================= BOWLING (THIS MATCH) =================
    private double overs;
    private int ballsBowled;
    private int runsConceded;
    private int wickets;
    private int maidens;
    private double economy;
    private int wides;
    private int noBalls;
}
