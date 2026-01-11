package com.cricriser.cricriser.team;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface TeamRepository extends MongoRepository<Team, String> {

    // Get all teams in a league
    List<Team> findByLeagueId(String leagueId);

    List<Team> findById(List<String> teamIds);
    // Check duplicate team name ONLY inside a league
    boolean existsByLeagueIdAndNameIgnoreCase(String leagueId, String name);
    Optional<Team> findByNameIgnoreCase(String name);


}
