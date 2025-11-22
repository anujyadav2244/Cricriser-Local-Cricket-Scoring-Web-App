package com.cricriser.cricriser.points;

import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PointsTableRepository extends MongoRepository<PointsTable, String> {
    List<PointsTable> findByLeagueId(String leagueId);
    void deleteByLeagueId(String leagueId);
}
