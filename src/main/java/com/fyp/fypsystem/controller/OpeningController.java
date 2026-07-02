package com.fyp.fypsystem.controller;

import com.fyp.fypsystem.repository.OpeningRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/openings")
public class OpeningController {

    private final OpeningRepository openingRepository;

    public OpeningController(OpeningRepository openingRepository) {
        this.openingRepository = openingRepository;
    }

    @GetMapping("/lookup")
    public ResponseEntity<?> lookup(@RequestParam("moves") String moves) {
        return openingRepository.findByMoveSequence(moves.trim())
                .map(o -> ResponseEntity.ok(Map.of("name", o.getOpeningName())))
                .orElse(ResponseEntity.notFound().build());
    }
}
