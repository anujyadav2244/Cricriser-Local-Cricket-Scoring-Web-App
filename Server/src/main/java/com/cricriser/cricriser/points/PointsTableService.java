package com.cricriser.cricriser.points;

import java.util.*;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.cricriser.cricriser.match.MatchScore;
import com.cricriser.cricriser.match.MatchScoreRepository;

@Service
public class PointsTableService {

    @Autowired
    private MatchScoreRepository matchScoreRepository;

    @Autowired
    private PointsTableRepository pointsTableRepository;

    // Points config
    private final int WIN_POINTS = 2;
    private final int TIE_POINTS = 1;
    private final int NR_POINTS = 1;

    /**
     * Recalculate points table for a league from scratch using completed matches.
     * Saves results to points_table collection and returns the sorted table.
     */
    public List<PointsTable> recalculatePointsTable(String leagueId) {
        // Remove old table entries for this league
        pointsTableRepository.deleteByLeagueId(leagueId);

        // Fetch all completed match scores for league
        List<MatchScore> matches = matchScoreRepository.findByLeagueId(leagueId)
                .stream()
                .filter(ms -> ms.getMatchStatus() != null && ms.getMatchStatus().equalsIgnoreCase("Completed"))
                .collect(Collectors.toList());

        // Map teamName -> PointsTable accumulator
        Map<String, PointsTable> map = new HashMap<>();

        for (MatchScore m : matches) {
            String t1 = m.getTeam1Id();
            String t2 = m.getTeam2Id();

            // ensure entries exist
            map.putIfAbsent(t1, createEmptyEntry(leagueId, t1));
            map.putIfAbsent(t2, createEmptyEntry(leagueId, t2));

            PointsTable p1 = map.get(t1);
            PointsTable p2 = map.get(t2);

            // update played
            p1.setPlayed(p1.getPlayed() + 1);
            p2.setPlayed(p2.getPlayed() + 1);

            // accumulate runs and balls (convert overs to balls)
            int t1BallsFaced = oversToBalls(m.getTeam1Overs());
            int t2BallsFaced = oversToBalls(m.getTeam2Overs());

            p1.setRunsFor(p1.getRunsFor() + m.getTeam1Runs());
            p1.setRunsAgainst(p1.getRunsAgainst() + m.getTeam2Runs());
            p1.setBallsFaced(p1.getBallsFaced() + t1BallsFaced);
            p1.setBallsBowled(p1.getBallsBowled() + t2BallsFaced);

            p2.setRunsFor(p2.getRunsFor() + m.getTeam2Runs());
            p2.setRunsAgainst(p2.getRunsAgainst() + m.getTeam1Runs());
            p2.setBallsFaced(p2.getBallsFaced() + t2BallsFaced);
            p2.setBallsBowled(p2.getBallsBowled() + t1BallsFaced);

            // result -> update wins/ties/noresult/loss and points
            if (m.getMatchWinner() != null) {
                String winner = m.getMatchWinner();
                if (winner.equalsIgnoreCase(t1)) {
                    p1.setWon(p1.getWon() + 1);
                    p1.setPoints(p1.getPoints() + WIN_POINTS);
                    p2.setLost(p2.getLost() + 1);
                } else if (winner.equalsIgnoreCase(t2)) {
                    p2.setWon(p2.getWon() + 1);
                    p2.setPoints(p2.getPoints() + WIN_POINTS);
                    p1.setLost(p1.getLost() + 1);
                } else {
                    // drawn / tie handling if you store "Tie" explicitly as winner
                    p1.setTied(p1.getTied() + 1);
                    p2.setTied(p2.getTied() + 1);
                    p1.setPoints(p1.getPoints() + TIE_POINTS);
                    p2.setPoints(p2.getPoints() + TIE_POINTS);
                }
            } else {
                // No winner (e.g., abandoned/no result)
                p1.setNoResult(p1.getNoResult() + 1);
                p2.setNoResult(p2.getNoResult() + 1);
                p1.setPoints(p1.getPoints() + NR_POINTS);
                p2.setPoints(p2.getPoints() + NR_POINTS);
            }
        }

        // compute NRR and save
        List<PointsTable> result = new ArrayList<>();
        for (PointsTable entry : map.values()) {
            double nrr = computeNRR(entry.getRunsFor(), entry.getBallsFaced(), entry.getRunsAgainst(),
                    entry.getBallsBowled());
            entry.setNetRunRate(nrr);
            result.add(entry);
        }

        // sort: points desc, NRR desc, runsFor desc
        result.sort(Comparator.comparingInt(PointsTable::getPoints).reversed()
                .thenComparingDouble(PointsTable::getNetRunRate).reversed()
                .thenComparingInt(PointsTable::getRunsFor).reversed());

        // save to DB
        pointsTableRepository.saveAll(result);

        return result;
    }

    public List<PointsTable> getPointsTable(String leagueId) {
        List<PointsTable> table = pointsTableRepository.findByLeagueId(leagueId);
        // sort before returning to be safe
        table.sort(Comparator.comparingInt(PointsTable::getPoints).reversed()
                .thenComparingDouble(PointsTable::getNetRunRate).reversed()
                .thenComparingInt(PointsTable::getRunsFor).reversed());
        return table;
    }

    // ---------- helpers ----------
    private PointsTable createEmptyEntry(String leagueId, String teamName) {
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

    // convert overs in decimal like 49.3 to balls (49 overs 3 balls => 49*6 + 3)
    private int oversToBalls(double overs) {
        int whole = (int) overs;
        int fraction = (int) Math.round((overs - whole) * 10); // e.g., 49.3 -> 3
        return whole * 6 + fraction;
    }

    // compute NRR as (runsFor/oversFaced) - (runsAgainst/oversBowled)
    private double computeNRR(int runsFor, int ballsFaced, int runsAgainst, int ballsBowled) {
        if (ballsFaced == 0 || ballsBowled == 0)
            return 0.0;

        double oversFaced = ballsFaced / 6.0;
        double oversBowled = ballsBowled / 6.0;

        double forRate = runsFor / oversFaced;
        double againstRate = runsAgainst / oversBowled;

        // round to 3 decimals
        double nrr = forRate - againstRate;
        return Math.round(nrr * 1000d) / 1000d;
    }

    /**
     * Update the points table for a league WITHOUT deleting old entries.
     * It recalculates from DB but only updates/overwrites entries.
     */
    public List<PointsTable> updatePointsTable(String leagueId) {

    // Fetch old table
    List<PointsTable> oldTable = pointsTableRepository.findByLeagueId(leagueId);

    // Calculate new table WITHOUT deleting old
    List<MatchScore> matches = matchScoreRepository.findByLeagueId(leagueId)
            .stream()
            .filter(ms -> ms.getMatchStatus() != null && ms.getMatchStatus().equalsIgnoreCase("Completed"))
            .collect(Collectors.toList());

    Map<String, PointsTable> map = new HashMap<>();

    // Create fresh stats
    for (MatchScore m : matches) {
        String t1 = m.getTeam1Id();
        String t2 = m.getTeam2Id();

        map.putIfAbsent(t1, createEmptyEntry(leagueId, t1));
        map.putIfAbsent(t2, createEmptyEntry(leagueId, t2));

        PointsTable p1 = map.get(t1);
        PointsTable p2 = map.get(t2);

        p1.setPlayed(p1.getPlayed() + 1);
            p2.setPlayed(p2.getPlayed() + 1);

            // accumulate runs and balls (convert overs to balls)
            int t1BallsFaced = oversToBalls(m.getTeam1Overs());
            int t2BallsFaced = oversToBalls(m.getTeam2Overs());

            p1.setRunsFor(p1.getRunsFor() + m.getTeam1Runs());
            p1.setRunsAgainst(p1.getRunsAgainst() + m.getTeam2Runs());
            p1.setBallsFaced(p1.getBallsFaced() + t1BallsFaced);
            p1.setBallsBowled(p1.getBallsBowled() + t2BallsFaced);

            p2.setRunsFor(p2.getRunsFor() + m.getTeam2Runs());
            p2.setRunsAgainst(p2.getRunsAgainst() + m.getTeam1Runs());
            p2.setBallsFaced(p2.getBallsFaced() + t2BallsFaced);
            p2.setBallsBowled(p2.getBallsBowled() + t1BallsFaced);

            // result -> update wins/ties/noresult/loss and points
            if (m.getMatchWinner() != null) {
                String winner = m.getMatchWinner();
                if (winner.equalsIgnoreCase(t1)) {
                    p1.setWon(p1.getWon() + 1);
                    p1.setPoints(p1.getPoints() + WIN_POINTS);
                    p2.setLost(p2.getLost() + 1);
                } else if (winner.equalsIgnoreCase(t2)) {
                    p2.setWon(p2.getWon() + 1);
                    p2.setPoints(p2.getPoints() + WIN_POINTS);
                    p1.setLost(p1.getLost() + 1);
                } else {
                    // drawn / tie handling if you store "Tie" explicitly as winner
                    p1.setTied(p1.getTied() + 1);
                    p2.setTied(p2.getTied() + 1);
                    p1.setPoints(p1.getPoints() + TIE_POINTS);
                    p2.setPoints(p2.getPoints() + TIE_POINTS);
                }
            } else {
                // No winner (e.g., abandoned/no result)
                p1.setNoResult(p1.getNoResult() + 1);
                p2.setNoResult(p2.getNoResult() + 1);
                p1.setPoints(p1.getPoints() + NR_POINTS);
                p2.setPoints(p2.getPoints() + NR_POINTS);
            }
        
    }

    // Apply NRR
    List<PointsTable> newTable = new ArrayList<>(map.values());

    for (PointsTable p : newTable) {
        double nrr = computeNRR(p.getRunsFor(), p.getBallsFaced(), p.getRunsAgainst(), p.getBallsBowled());
        p.setNetRunRate(nrr);

        // Copy old ID if exists
        for (PointsTable old : oldTable) {
            if (old.getTeamName().equalsIgnoreCase(p.getTeamName())) {
                p.setId(old.getId());
            }
        }
    }

    // Save updated rows (no delete!)
    pointsTableRepository.saveAll(newTable);

    return newTable;
}

}
