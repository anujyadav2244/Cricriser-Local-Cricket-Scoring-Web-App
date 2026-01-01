package com.cricriser.cricriser.league;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "league")
public class League {

    @Id
    private String id;

    private String adminId;
    private String name;

    private String leagueType;//NEW FIELD → "BILATERAL" or "TOURNAMENT"

    private int noOfTeams;
    private int noOfMatches;
    private List<String> teams = new ArrayList<>();

    private Date startDate;
    private Date endDate;
    private String tour;

    private String leagueFormat;// T20, ODI, TEST

    private List<String> umpires;

    private String leagueFormatType;// SINGLE_ROUND_ROBIN, DOUBLE_ROUND_ROBIN, GROUP

    private Integer oversPerInnings;
    private Integer testDays;

    private String logoUrl;
}
