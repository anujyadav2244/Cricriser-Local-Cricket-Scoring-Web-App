package com.cricriser.cricriser.match.matchscheduling;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface MatchScheduleRepository extends MongoRepository<MatchSchedule, String> {
    List<MatchSchedule> findByLeagueId(String leagueId);
    void deleteLeagueById(String leagueId);
    void deleteByLeagueId(String leagueId);
    long countByLeagueId(String leagueId);



}

