package com.fyp.fypsystem.controller;

import com.fyp.fypsystem.model.Puzzle;
import com.fyp.fypsystem.model.PuzzleMove;
import com.fyp.fypsystem.repository.PuzzleRepository;
import com.fyp.fypsystem.service.LichessPuzzleImportService;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/puzzles")
@CrossOrigin(origins = "*")
public class PuzzleController {

    private final PuzzleRepository puzzleRepository;
    private final LichessPuzzleImportService importService;
    private static final String ROLE_HEADER = "X-User-Role";
    private static final List<String> SORT_FIELDS = List.of("id", "title", "theme", "difficulty");

    public PuzzleController(PuzzleRepository puzzleRepository, LichessPuzzleImportService importService) {
        this.puzzleRepository = puzzleRepository;
        this.importService = importService;
    }

    @PostMapping
    public Puzzle create(@RequestBody Puzzle puzzle,
                         @RequestHeader(value = ROLE_HEADER, required = false) String role) {
        requireCoachOrAdmin(role);
        syncPuzzleMoves(puzzle);
        return puzzleRepository.save(puzzle);
    }

    @GetMapping
    public List<Puzzle> getAll(@RequestParam(required = false) String topic,
                               @RequestParam(required = false) Integer level,
                               @RequestParam(required = false) String theme,
                               @RequestParam(required = false) Integer difficulty,
                               @RequestParam(required = false) Boolean published,
                               @RequestParam(required = false) String sortBy,
                               @RequestParam(required = false) String sortDir,
                               @RequestHeader(value = ROLE_HEADER, required = false) String role) {
        String resolvedTopic = topic != null ? topic : theme;
        Integer resolvedLevel = level != null ? level : difficulty;
        String normalizedRole = normalizeRole(role);
        boolean allowAll = isCoachOrAdmin(normalizedRole);
        boolean publishedOnly = !allowAll || Boolean.TRUE.equals(published);
        Sort sort = buildSort(sortBy, sortDir);

        if (publishedOnly) {
            if (resolvedTopic != null && resolvedLevel != null) {
                return puzzleRepository.findByPublishedTrueAndThemeContainingIgnoreCaseAndDifficulty(resolvedTopic, resolvedLevel, sort);
            }
            if (resolvedTopic != null) {
                return puzzleRepository.findByPublishedTrueAndThemeContainingIgnoreCase(resolvedTopic, sort);
            }
            if (resolvedLevel != null) {
                return puzzleRepository.findByPublishedTrueAndDifficulty(resolvedLevel, sort);
            }
            return puzzleRepository.findByPublishedTrue(sort);
        }

        if (resolvedTopic != null && resolvedLevel != null) {
            return puzzleRepository.findByThemeContainingIgnoreCaseAndDifficulty(resolvedTopic, resolvedLevel, sort);
        }
        if (resolvedTopic != null) {
            return puzzleRepository.findByThemeContainingIgnoreCase(resolvedTopic, sort);
        }
        if (resolvedLevel != null) {
            return puzzleRepository.findByDifficulty(resolvedLevel, sort);
        }
        return sort.isUnsorted() ? puzzleRepository.findAll() : puzzleRepository.findAll(sort);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Puzzle> getPuzzle(@PathVariable Long id,
                                            @RequestHeader(value = ROLE_HEADER, required = false) String role) {
        Puzzle puzzle = puzzleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Puzzle not found"));

        String normalizedRole = normalizeRole(role);
        if (!isCoachOrAdmin(normalizedRole) && !Boolean.TRUE.equals(puzzle.getPublished())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Puzzle not available");
        }

        return ResponseEntity.ok(puzzle);
    }

    @PutMapping("/{id}")
    public Puzzle update(@PathVariable Long id,
                         @RequestBody Puzzle payload,
                         @RequestHeader(value = ROLE_HEADER, required = false) String role) {
        requireCoachOrAdmin(role);
        Puzzle existing = puzzleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Puzzle not found"));

        if (payload.getTitle() != null) {
            existing.setTitle(payload.getTitle());
        }
        if (payload.getTheme() != null) {
            existing.setTheme(payload.getTheme());
        }
        if (payload.getDifficulty() != null) {
            existing.setDifficulty(payload.getDifficulty());
        }
        if (payload.getFen() != null) {
            existing.setFen(payload.getFen());
        }
        if (payload.getSide() != null) {
            existing.setSide(payload.getSide());
        }
        if (payload.getSolutionMove() != null) {
            existing.setSolutionMove(payload.getSolutionMove());
        }
        if (payload.getPublished() != null) {
            existing.setPublished(payload.getPublished());
        }
        if (hasMoves(payload)) {
            existing.getMoves().clear();
            int order = 1;
            for (PuzzleMove move : payload.getMoves()) {
                if (move == null || move.getMoveUci() == null || move.getMoveUci().isBlank()) {
                    continue;
                }
                move.setPuzzle(existing);
                if (move.getMoveOrder() == null) {
                    move.setMoveOrder(order);
                }
                existing.getMoves().add(move);
                order++;
            }
        }

        syncPuzzleMoves(existing);
        return puzzleRepository.save(existing);
    }

    @PutMapping("/{id}/publish")
    public Puzzle publish(@PathVariable Long id,
                          @RequestBody PublishRequest payload,
                          @RequestHeader(value = ROLE_HEADER, required = false) String role) {
        requireCoachOrAdmin(role);
        if (payload == null || payload.published() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Published flag is required");
        }
        Puzzle puzzle = puzzleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Puzzle not found"));
        puzzle.setPublished(payload.published());
        return puzzleRepository.save(puzzle);
    }

    public record PublishRequest(Boolean published) {
    }

    @PostMapping("/import")
    public ResponseEntity<?> importCsv(@RequestBody ImportRequest request,
                                       @RequestHeader(value = ROLE_HEADER, required = false) String role) {
        requireCoachOrAdmin(role);
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Import request is required");
        }
        LichessPuzzleImportService.ImportResult result = importService.importFromCsv(
            request.path(),
            request.limit(),
            request.offset(),
            request.published(),
            request.skipExisting()
        );
        return ResponseEntity.ok(result);
    }

    public record ImportRequest(String path, Integer limit, Integer offset, Boolean published, Boolean skipExisting) {
    }

    @GetMapping("/themes")
    public List<ThemeCount> getThemes(@RequestParam(required = false) Boolean published,
                                      @RequestHeader(value = ROLE_HEADER, required = false) String role) {
        String normalizedRole = normalizeRole(role);
        boolean allowAll = isCoachOrAdmin(normalizedRole);
        boolean publishedOnly = !allowAll || Boolean.TRUE.equals(published);

        List<Puzzle> puzzles = publishedOnly
                ? puzzleRepository.findByPublishedTrue()
                : puzzleRepository.findAll();

        Map<String, Integer> counts = new HashMap<>();
        for (Puzzle puzzle : puzzles) {
            String theme = puzzle.getTheme();
            if (theme == null || theme.isBlank()) {
                continue;
            }
            String[] tokens = theme.split("[,\\s]+");
            for (String token : tokens) {
                String cleaned = token.trim();
                if (cleaned.isEmpty()) {
                    continue;
                }
                counts.merge(cleaned, 1, Integer::sum);
            }
        }

        return counts.entrySet().stream()
                .map(entry -> new ThemeCount(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparingInt(ThemeCount::count).reversed().thenComparing(ThemeCount::theme))
                .collect(Collectors.toList());
    }

    public record ThemeCount(String theme, int count) {
    }

    private boolean hasMoves(Puzzle puzzle) {
        if (puzzle.getMoves() == null) {
            return false;
        }
        for (PuzzleMove move : puzzle.getMoves()) {
            if (move != null && move.getMoveUci() != null && !move.getMoveUci().isBlank()) {
                return true;
            }
        }
        return false;
    }

    private void syncPuzzleMoves(Puzzle puzzle) {
        if (puzzle.getMoves() != null) {
            int order = 1;
            for (PuzzleMove move : puzzle.getMoves()) {
                if (move == null) {
                    continue;
                }
                move.setPuzzle(puzzle);
                if (move.getMoveOrder() == null) {
                    move.setMoveOrder(order);
                }
                order++;
            }
        }

        String solutionMove = puzzle.getSolutionMove();
        if (solutionMove == null || solutionMove.isBlank()) {
            if (puzzle.getMoves() != null && !puzzle.getMoves().isEmpty()) {
                PuzzleMove first = puzzle.getMoves().get(0);
                if (first != null && first.getMoveUci() != null && !first.getMoveUci().isBlank()) {
                    puzzle.setSolutionMove(first.getMoveUci());
                }
            }
            return;
        }

        if (puzzle.getMoves() == null || puzzle.getMoves().isEmpty()) {
            PuzzleMove move = new PuzzleMove();
            move.setPuzzle(puzzle);
            move.setMoveOrder(1);
            move.setMoveUci(solutionMove);
            List<PuzzleMove> moves = new ArrayList<>();
            moves.add(move);
            puzzle.setMoves(moves);
        }
    }

    private String normalizeRole(String role) {
        if (role == null || role.isBlank()) {
            return "STUDENT";
        }
        return role.trim().toUpperCase();
    }

    private boolean isCoachOrAdmin(String role) {
        return "COACH".equals(role) || "ADMIN".equals(role);
    }

    private void requireCoachOrAdmin(String role) {
        if (!isCoachOrAdmin(normalizeRole(role))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Coach/Admin only");
        }
    }

    private Sort buildSort(String sortBy, String sortDir) {
        if (sortBy == null || sortBy.isBlank()) {
            return Sort.unsorted();
        }
        String normalized = sortBy.trim();
        if (!SORT_FIELDS.contains(normalized)) {
            return Sort.unsorted();
        }
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        return Sort.by(direction, normalized);
    }
}
