package com.cricriser.cricriser.match;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/match/score")
@CrossOrigin(origins = "*")
public class MatchScoreController {

    @Autowired
    private MatchScoreService matchScoreService;

    // ================= ADD MATCH SCORE (first time) =================
    @PostMapping("/add")
    public ResponseEntity<?> addScore(@RequestBody MatchScore score) {
        try {
            MatchScore saved = matchScoreService.addScore(score);
            return ResponseEntity.ok(saved);
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    // ================= UPDATE MATCH SCORE =================
    @PutMapping("/update/{matchId}")
    public ResponseEntity<?> updateScore(@PathVariable String matchId, @RequestBody MatchScore updatedScore) {
        try {
            updatedScore.setMatchId(matchId);
            MatchScore saved = matchScoreService.updateScore(updatedScore);
            return ResponseEntity.ok(saved);
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }
    // GET SCORE BY MATCH ID

    @GetMapping("/{matchId}")
    public ResponseEntity<?> getByMatchId(@PathVariable String matchId) {
        MatchScore score = matchScoreService.getMatchScoreByMatchId(matchId);
        if (score == null)
            return ResponseEntity.notFound().build();
        return ResponseEntity.ok(score);
    }

    // GET ALL

    @GetMapping("/all")
    public ResponseEntity<List<MatchScore>> allScores() {
        return ResponseEntity.ok(matchScoreService.getAllScores());
    }

    // DELETE

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteScore(@PathVariable String id) {
        matchScoreService.deleteScoreById(id);
        return ResponseEntity.ok("Match score deleted");
    }

    // ================= DELETE ALL MATCH SCORES =================
    @DeleteMapping("/delete/all")
    public ResponseEntity<?> deleteAllScores() {
        try {
            matchScoreService.deleteAllScores();
            return ResponseEntity.ok("All match scores deleted successfully.");
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

}
