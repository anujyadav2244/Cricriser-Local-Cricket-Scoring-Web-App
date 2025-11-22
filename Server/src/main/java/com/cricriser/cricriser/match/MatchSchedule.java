package com.cricriser.cricriser.match;

import java.util.Date;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Data
@Document(collection = "match_schedule")
public class MatchSchedule {
    @Id
    private String id;
    private String leagueId;

    private String team1;
    private String team2;

    private int matchNo;         // Match sequence number
    private String matchType;    // "LEAGUE", "ELIMINATOR", "SEMI_FINAL_1", "SEMI_FINAL_2", "FINAL"

    private Date scheduledDate;  // Date & time of the match
    private String venue;

    private String status;       // "Scheduled", "Completed"

    // Score fields (only admin can update if status == Scheduled)
    private Integer team1Score;
    private Integer team2Score;
    private String result;       // "Team1 won by X runs", "Team2 won by Y wickets", "Draw"

    private Integer matchOvers;  // NEW: number of overs for the match
}
