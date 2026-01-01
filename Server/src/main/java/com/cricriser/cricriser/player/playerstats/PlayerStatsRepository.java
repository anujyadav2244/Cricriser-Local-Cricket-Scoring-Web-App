package com.cricriser.cricriser.player.playerstats;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface PlayerStatsRepository
        extends MongoRepository<PlayerStats, String> {

    PlayerStats findByPlayerId(String playerId);
}

