package com.fyp.fypsystem.controller;

import com.fyp.fypsystem.service.StockfishService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/analysis")
@CrossOrigin(origins = "*")
public class AnalysisController {
    private final StockfishService stockfishService;

    public AnalysisController(StockfishService stockfishService) {
        this.stockfishService = stockfishService;
    }

    @PostMapping
    public ResponseEntity<?> analyze(@RequestBody AnalysisRequest request) {
        if (request == null || request.fen() == null || request.fen().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "FEN is required"));
        }
        try {
            StockfishService.AnalysisResult result = stockfishService.analyze(
                request.fen(),
                request.depth(),
                request.movetime(),
                request.multipv()
            );
            return ResponseEntity.ok(new AnalysisResponse(result.bestMove(), result.evaluation(), result.pv(), result.lines()));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", ex.getMessage()));
        }
    }

    public record AnalysisRequest(String fen, Integer depth, Integer movetime, Integer multipv) {
    }

    public record AnalysisResponse(String bestMove, String evaluation, String pv, List<StockfishService.AnalysisLine> lines) {
    }
}
