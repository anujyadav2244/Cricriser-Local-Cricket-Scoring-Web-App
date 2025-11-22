package com.cricriser.cricriser.points;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/points")
@CrossOrigin(origins = "*")
public class PointsTableController {

    @Autowired
    private PointsTableService pointsTableService;

    // Recalculate and return
    @PostMapping("/recalculate/{leagueId}")
    public ResponseEntity<?> recalculate(@PathVariable String leagueId) {
        try {
            List<PointsTable> table = pointsTableService.recalculatePointsTable(leagueId);
            return ResponseEntity.ok(table);
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    // Get current table (from DB)
    @GetMapping("/{leagueId}")
    public ResponseEntity<?> getTable(@PathVariable String leagueId) {
        try {
            List<PointsTable> table = pointsTableService.getPointsTable(leagueId);
            return ResponseEntity.ok(table);
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    /**
     * Update existing points table (does not delete the old one).
     * Useful when a single match score is updated.
     */
    @PutMapping("/update/{leagueId}")
    public ResponseEntity<?> updatePoints(@PathVariable String leagueId) {
        try {
            List<PointsTable> updated = pointsTableService.updatePointsTable(leagueId);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error updating points table: " + e.getMessage());
        }
    }

}
