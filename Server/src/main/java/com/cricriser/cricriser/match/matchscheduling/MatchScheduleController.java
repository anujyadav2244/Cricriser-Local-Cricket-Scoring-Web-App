package com.cricriser.cricriser.match.matchscheduling;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.Data;

@Data
@RestController
@RequestMapping("/api/match")
@CrossOrigin(origins = "${app.allowed.origins:http://localhost:5173}", allowCredentials = "true")
public class MatchScheduleController {

    @Autowired
    private MatchScheduleService service;




    // ================= MANUAL MATCH CREATION =================
    @PostMapping("/create-manual")
    public ResponseEntity<?> createMatchManually(
            @RequestHeader("Authorization") String token,
            @RequestBody MatchSchedule match) {
        try {
            MatchSchedule saved = service.createMatchManually(token, match);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    // ================= GET ALL MATCHES =================
    @GetMapping("/get-all")
    public ResponseEntity<List<MatchSchedule>> getAllMatches() {
        return ResponseEntity.ok(service.getAllMatches());
    }

    // ================= GET MATCH BY ID =================
    @GetMapping("/{id}")
    public ResponseEntity<?> getMatchById(@PathVariable String id) {
        try {
            MatchSchedule match = service.getMatchById(id);
            return ResponseEntity.ok(match);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    // ================= UPDATE MATCH =================
    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateMatch(
            @RequestHeader("Authorization") String token,
            @PathVariable String id,
            @RequestBody MatchSchedule updatedMatch) {
        try {
            MatchSchedule match = service.updateMatch(token, id, updatedMatch);
            return ResponseEntity.ok(match);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    // ================= DELETE MATCH =================
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteMatch(
            @RequestHeader("Authorization") String token,
            @PathVariable String id) {
        try {
            service.deleteMatch(token, id);
            return ResponseEntity.ok(Map.of("message", "Match deleted successfully!"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    // ================= DELETE ALL MATCHES BY ADMIN =================
    @DeleteMapping("/delete-all")
    public ResponseEntity<?> deleteAllMatchesByAdmin(@RequestHeader("Authorization") String token) {
        try {
            service.deleteAllMatchesByAdmin(token);
            return ResponseEntity.ok(Map.of("message", "All matches deleted successfully!"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        }
    }
}
