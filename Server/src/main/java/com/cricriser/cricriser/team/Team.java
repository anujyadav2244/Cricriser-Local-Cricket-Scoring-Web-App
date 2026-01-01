package com.cricriser.cricriser.team;

import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "team")
public class Team {
    @Id
    private String id;
    private String name;
    private String leagueId;
    private String coach;
    private String captain;       // store captain name
    private String viceCaptain;   // store vice-captain name
    private String logoUrl;
    private List<String> squadPlayerIds;


}
