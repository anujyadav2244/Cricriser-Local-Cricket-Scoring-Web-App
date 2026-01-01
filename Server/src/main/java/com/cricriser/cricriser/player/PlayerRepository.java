package com.cricriser.cricriser.player;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface PlayerRepository extends MongoRepository<Player, String> {
    Optional<Player> findByEmail(String email);
    
}

