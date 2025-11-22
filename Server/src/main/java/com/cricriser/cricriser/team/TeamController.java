package com.cricriser.cricriser.team;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/teams")
@CrossOrigin(origins = "${app.allowed.origins:http://localhost:5173}", allowCredentials = "true")
public class TeamController {

    @Autowired
    private TeamService teamService;

    // ======= CREATE TEAM with logo upload =======
    @PostMapping(value = "/create", consumes = { "multipart/form-data" })
    public ResponseEntity<?> createTeam(
            @RequestHeader("Authorization") String token,
            @RequestPart("team") String teamJson,
            @RequestPart(value = "logo", required = false) MultipartFile logoFile) {
        try {
            Team savedTeam = teamService.createTeam(token, teamJson, logoFile);
            return ResponseEntity.ok(savedTeam);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    // ======= UPDATE TEAM with logo upload =======
    @PutMapping(value = "/update/{id}", consumes = { "multipart/form-data" })
    public ResponseEntity<?> updateTeam(
            @RequestHeader("Authorization") String token,
            @PathVariable String id,
            @RequestPart("team") String teamJson,
            @RequestPart(value = "logo", required = false) MultipartFile logoFile) {
        try {
            Team updatedTeam = teamService.updateTeam(token, id, teamJson, logoFile);
            return ResponseEntity.ok(updatedTeam);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    // ======= DELETE TEAM =======
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteTeam(
            @RequestHeader("Authorization") String token,
            @PathVariable String id) {
        try {
            teamService.deleteTeamById(token, id);
            return ResponseEntity.ok(Map.of("message", "Team deleted successfully!"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    // ======= DELETE ALL TEAMS =======
    @DeleteMapping("/delete-all")
    public ResponseEntity<?> deleteAllTeams(@RequestHeader("Authorization") String token) {
        try {
            teamService.deleteAllTeamsByAdmin(token);
            return ResponseEntity.ok(Map.of("message", "All teams of your leagues deleted successfully!"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    // ======= GET TEAM BY ID =======
    @GetMapping("/{id}")
    public ResponseEntity<?> getTeamById(@PathVariable String id) {
        try {
            Team team = teamService.getTeamById(id);
            return ResponseEntity.ok(team);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", e.getMessage()));
        }
    }


    // ======= GET ALL TEAMS =======
    @GetMapping("/get-all")
    public ResponseEntity<List<Team>> getAllTeams() {
        return ResponseEntity.ok(teamService.getAllTeams());
    }
}
