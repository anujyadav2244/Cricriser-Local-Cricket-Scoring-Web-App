package com.cricriser.cricriser.player.playerstats;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "playerStats")
public class PlayerStats {

    @Id
    private String playerId;   // SAME as Player.id

    private int matches;
    private int innings;

    private double battingStrikeRate;
    private double battingAverage;

    // Batting
    private int runsScored;
    private int ballsFaced;
    private int fours;
    private int sixes;

    private int hundreds;
    private int fifties;
    private int highestScore;

    // Bowling
    private int runsConceded;
    private int ballsBowled;
    private int wickets;
    private double bowlingStrikeRate;
    private double bowlingAverage;
    private double economy;
    private int fiveWicketHauls;
    private int wides;
    private int noBalls;

    private String bestBowlingFigures;
    private String bestMatchFigures;
}
