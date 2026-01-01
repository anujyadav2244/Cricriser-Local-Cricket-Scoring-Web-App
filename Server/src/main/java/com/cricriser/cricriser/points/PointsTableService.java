package com.cricriser.cricriser.points;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.cricriser.cricriser.match.matchscoring.MatchScore;
import com.cricriser.cricriser.match.matchscoring.MatchScoreRepository;

@Service
public class PointsTableService {

    @Autowired
    private MatchScoreRepository matchScoreRepository;

    @Autowired
    private PointsTableRepository pointsTableRepository;

    // ================= CONFIG =================
    private static final int WIN_POINTS = 2;
    private static final int TIE_POINTS = 1;
    private static final int NR_POINTS  = 1;

    // FULL REBUILD POINTS TABLE (DELETE & RECALCULATE)
    public List<PointsTable> recalculatePointsTable(String leagueId) {

        // Delete old table
        pointsTableRepository.deleteByLeagueId(leagueId);

        // Get completed matches only
        List<MatchScore> matches = matchScoreRepository.findByLeagueId(leagueId)
                .stream()
                .filter(m -> "Completed".equalsIgnoreCase(m.getMatchStatus()))
                .collect(Collectors.toList());

        Map<String, PointsTable> map = new HashMap<>();

        for (MatchScore m : matches) {
            processMatch(map, leagueId, m);
        }

        // Calculate NRR
        List<PointsTable> finalTable = finalizeTable(map);

        // Save fresh
        pointsTableRepository.saveAll(finalTable);
        return finalTable;
    }

    //  UPDATE POINTS TABLE WITHOUT DELETE
    public List<PointsTable> updatePointsTable(String leagueId) {

        List<PointsTable> oldTable = pointsTableRepository.findByLeagueId(leagueId);

        List<MatchScore> matches = matchScoreRepository.findByLeagueId(leagueId)
                .stream()
                .filter(m -> "Completed".equalsIgnoreCase(m.getMatchStatus()))
                .collect(Collectors.toList());

        Map<String, PointsTable> map = new HashMap<>();

        for (MatchScore m : matches) {
            processMatch(map, leagueId, m);
        }

        // Finalize (NRR + Sort)
        List<PointsTable> newTable = finalizeTable(map);

        // Preserve Mongo IDs
        for (PointsTable fresh : newTable) {
            for (PointsTable old : oldTable) {
                if (fresh.getTeamName().equalsIgnoreCase(old.getTeamName())) {
                    fresh.setId(old.getId());
                }
            }
        }

        // Overwrite
        pointsTableRepository.saveAll(newTable);
        return newTable;
    }

    //GET CURRENT POINTS TABLE
    public List<PointsTable> getPointsTable(String leagueId) {

        List<PointsTable> table = pointsTableRepository.findByLeagueId(leagueId);

        table.sort(Comparator
                .comparingInt(PointsTable::getPoints).reversed()
                .thenComparingDouble(PointsTable::getNetRunRate).reversed()
                .thenComparingInt(PointsTable::getRunsFor).reversed()
        );

        return table;
    }

    // PROCESS SINGLE MATCH ENTRY
    private void processMatch(Map<String, PointsTable> map,
        String leagueId, MatchScore m) {

        String t1 = m.getTeam1Id();
        String t2 = m.getTeam2Id();

        map.putIfAbsent(t1, createEmptyTeam(leagueId, t1));
        map.putIfAbsent(t2, createEmptyTeam(leagueId, t2));

        PointsTable p1 = map.get(t1);
        PointsTable p2 = map.get(t2);

        // Played
        p1.setPlayed(p1.getPlayed() + 1);
        p2.setPlayed(p2.getPlayed() + 1);

        // Runs & balls
        int t1Balls = oversToBalls(m.getTeam1Overs());
        int t2Balls = oversToBalls(m.getTeam2Overs());

        p1.setRunsFor(p1.getRunsFor() + m.getTeam1Runs());
        p1.setRunsAgainst(p1.getRunsAgainst() + m.getTeam2Runs());
        p1.setBallsFaced(p1.getBallsFaced() + t1Balls);
        p1.setBallsBowled(p1.getBallsBowled() + t2Balls);

        p2.setRunsFor(p2.getRunsFor() + m.getTeam2Runs());
        p2.setRunsAgainst(p2.getRunsAgainst() + m.getTeam1Runs());
        p2.setBallsFaced(p2.getBallsFaced() + t2Balls);
        p2.setBallsBowled(p2.getBallsBowled() + t1Balls);

        // Result
        if (m.getMatchWinner() == null || m.getMatchWinner().isBlank()) {
            // No result
            p1.setNoResult(p1.getNoResult() + 1);
            p2.setNoResult(p2.getNoResult() + 1);
            p1.setPoints(p1.getPoints() + NR_POINTS);
            p2.setPoints(p2.getPoints() + NR_POINTS);
        }

        else if (m.getMatchWinner().equalsIgnoreCase("Tie")) {
            p1.setTied(p1.getTied() + 1);
            p2.setTied(p2.getTied() + 1);
            p1.setPoints(p1.getPoints() + TIE_POINTS);
            p2.setPoints(p2.getPoints() + TIE_POINTS);
        }

        else if (m.getMatchWinner().equalsIgnoreCase(t1)) {
            p1.setWon(p1.getWon() + 1);
            p1.setPoints(p1.getPoints() + WIN_POINTS);
            p2.setLost(p2.getLost() + 1);
        }

        else if (m.getMatchWinner().equalsIgnoreCase(t2)) {
            p2.setWon(p2.getWon() + 1);
            p2.setPoints(p2.getPoints() + WIN_POINTS);
            p1.setLost(p1.getLost() + 1);
        }
    }

    //  FINALIZE: NRR + SORT
    private List<PointsTable> finalizeTable(Map<String, PointsTable> map) {

        for (PointsTable team : map.values()) {
            team.setNetRunRate(
                    computeNRR(
                            team.getRunsFor(),
                            team.getBallsFaced(),
                            team.getRunsAgainst(),
                            team.getBallsBowled()
                    )
            );
        }

        List<PointsTable> result = new ArrayList<>(map.values());

        result.sort(Comparator
                .comparingInt(PointsTable::getPoints).reversed()
                .thenComparingDouble(PointsTable::getNetRunRate).reversed()
                .thenComparingInt(PointsTable::getRunsFor).reversed()
        );

        return result;
    }

    //  CREATE EMPTY ROW
    private PointsTable createEmptyTeam(String leagueId, String teamName) {

        PointsTable p = new PointsTable();

        p.setLeagueId(leagueId);
        p.setTeamName(teamName);
        p.setPlayed(0);
        p.setWon(0);
        p.setLost(0);
        p.setTied(0);
        p.setNoResult(0);
        p.setPoints(0);
        p.setRunsFor(0);
        p.setRunsAgainst(0);
        p.setBallsFaced(0);
        p.setBallsBowled(0);
        p.setNetRunRate(0.0);

        return p;
    }

    //  OVERS TO BALLS FIX
    private int oversToBalls(double overs) {
        int whole = (int) overs;
        int balls = (int) ((overs - whole) * 10);

        if (balls > 5) balls = 0; // safety
        return whole * 6 + balls;
    }

    //  NRR CALCULATION
    private double computeNRR(int runsFor, int ballsFaced,
                              int runsAgainst, int ballsBowled) {

        if (ballsFaced == 0 || ballsBowled == 0)
            return 0;

        double oversFaced = ballsFaced / 6.0;
        double oversBowled = ballsBowled / 6.0;

        double nrr = (runsFor / oversFaced) - (runsAgainst / oversBowled);

        return Math.round(nrr * 1000.0) / 1000.0;
    }
}
