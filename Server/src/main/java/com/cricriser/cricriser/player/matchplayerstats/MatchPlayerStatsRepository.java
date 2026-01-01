package com.cricriser.cricriser.player.matchplayerstats;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface MatchPlayerStatsRepository
        extends MongoRepository<MatchPlayerStats, String> {

    Optional<MatchPlayerStats>
        findByMatchIdAndPlayerId(String matchId, String playerId);

    void deleteByMatchId(String matchId);

}

