package com.cricriser.cricriser.points;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Data

@Document(collection = "points_table")
public class PointsTable {

    @Id
    private String id;           
    private String leagueId;
    private String teamName;
    private String teamId;       // optional (if you have it), else null
    private int played;
    private int won;
    private int lost;
    private int tied;
    private int noResult;
    private int points;
    private double netRunRate;   // computed
    private int runsFor;
    private int runsAgainst;
    private int ballsFaced;      // total balls faced (to compute overs)
    private int ballsBowled;     // total balls bowled

    
}
