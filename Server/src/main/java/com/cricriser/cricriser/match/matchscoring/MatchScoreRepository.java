package com.cricriser.cricriser.match.matchscoring;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface MatchScoreRepository extends MongoRepository<MatchScore, String> {

    MatchScore findByMatchId(String matchId);

    List<MatchScore> findByLeagueId(String leagueId);

    void deleteByLeagueId(String leagueId);

}
