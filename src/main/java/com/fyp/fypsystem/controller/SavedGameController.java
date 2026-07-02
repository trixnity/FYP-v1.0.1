package com.fyp.fypsystem.controller;

import com.fyp.fypsystem.model.SavedGame;
import com.fyp.fypsystem.service.SavedGameService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
public class SavedGameController {

    private final SavedGameService savedGameService;

    public SavedGameController(SavedGameService savedGameService) {
        this.savedGameService = savedGameService;
    }

    @GetMapping({"/api/games/my", "/api/saved-games"})
    public ResponseEntity<?> getMyGames(Authentication authentication) {
        String email = requireEmail(authentication);
        if (email == null) return unauthorized();
        try {
            List<SavedGame> games = savedGameService.myGames(email);
            return ResponseEntity.ok(games);
        } catch (AccessDeniedException ex) {
            return unauthorized();
        }
    }

    @GetMapping({"/api/games/{id}", "/api/saved-games/{id}"})
    public ResponseEntity<?> getGame(@PathVariable Long id, Authentication authentication) {
        String email = requireEmail(authentication);
        if (email == null) return unauthorized();
        return savedGameService.getOwned(id, email)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping({"/api/games/save", "/api/saved-games"})
    public ResponseEntity<?> createGame(@RequestBody SavedGame payload, Authentication authentication) {
        String email = requireEmail(authentication);
        if (email == null) return unauthorized();
        try {
            return ResponseEntity.ok(savedGameService.create(payload, email));
        } catch (AccessDeniedException ex) {
            return unauthorized();
        }
    }

    @PutMapping({"/api/games/{id}", "/api/saved-games/{id}"})
    public ResponseEntity<?> updateGame(@PathVariable Long id,
                                        @RequestBody SavedGame payload,
                                        Authentication authentication) {
        String email = requireEmail(authentication);
        if (email == null) return unauthorized();
        return savedGameService.update(id, payload, email)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping({"/api/games/{id}", "/api/saved-games/{id}"})
    public ResponseEntity<?> deleteGame(@PathVariable Long id, Authentication authentication) {
        String email = requireEmail(authentication);
        if (email == null) return unauthorized();
        if (!savedGameService.delete(id, email)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Map.of("deleted", id));
    }

    private String requireEmail(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            return null;
        }
        return authentication.getName();
    }

    private ResponseEntity<Map<String, String>> unauthorized() {
        return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
    }
}
