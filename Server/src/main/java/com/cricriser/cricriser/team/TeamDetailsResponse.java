package com.cricriser.cricriser.team;

import java.util.List;
import com.cricriser.cricriser.player.PlayerCardDto;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TeamDetailsResponse {

    private String id;
    private String name;
    private String leagueName;

    private String coach;
    private String captainName;
    private String viceCaptainName;

    private List<PlayerCardDto> players;
}
