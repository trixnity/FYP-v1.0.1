package com.fyp.fypsystem.controller;

import com.fyp.fypsystem.dto.puzzle.ProcessPuzzleUploadResponse;
import com.fyp.fypsystem.dto.puzzle.PuzzleImportRequest;
import com.fyp.fypsystem.dto.puzzle.PuzzlePublishRequest;
import com.fyp.fypsystem.dto.puzzle.PuzzleUploadOptions;
import com.fyp.fypsystem.dto.puzzle.PuzzleUploadResult;
import com.fyp.fypsystem.dto.puzzle.ThemeCountResponse;
import com.fyp.fypsystem.model.Puzzle;
import com.fyp.fypsystem.model.PuzzleMove;
import com.fyp.fypsystem.repository.PuzzleRepository;
import com.fyp.fypsystem.service.PuzzleCsvImportService;
import com.fyp.fypsystem.service.PuzzleService;
import com.fyp.fypsystem.service.PuzzleUploadProcessingService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
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
    private final PuzzleCsvImportService importService;
    private final PuzzleUploadProcessingService uploadProcessingService;
    private final PuzzleService puzzleService;
    private static final String ROLE_HEADER = "X-User-Role";
    private static final List<String> SORT_FIELDS = List.of("id", "title", "theme", "difficulty");

    public PuzzleController(PuzzleRepository puzzleRepository,
                            PuzzleCsvImportService importService,
                            PuzzleUploadProcessingService uploadProcessingService,
                            PuzzleService puzzleService) {
        this.puzzleRepository = puzzleRepository;
        this.importService = importService;
        this.uploadProcessingService = uploadProcessingService;
        this.puzzleService = puzzleService;
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
                               @RequestParam(required = false) Integer limit,
                               @RequestParam(required = false) Integer offset,
                               @RequestParam(required = false) String sortBy,
                               @RequestParam(required = false) String sortDir,
                               @RequestHeader(value = ROLE_HEADER, required = false) String role) {
        String resolvedTopic = topic != null ? topic : theme;
        Integer resolvedLevel = level != null ? level : difficulty;
        String normalizedRole = normalizeRole(role);
        boolean allowAll = isCoachOrAdmin(normalizedRole);
        boolean publishedOnly = !allowAll || Boolean.TRUE.equals(published);
        Sort sort = buildSort(sortBy, sortDir);

        Pageable pageable = buildPageable(limit, offset, sort);

        if (pageable != null) {
            return fetchPaged(resolvedTopic, resolvedLevel, publishedOnly, pageable);
        }

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

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadRecognizedPuzzle(@RequestParam("file") MultipartFile file,
                                                    @RequestParam(required = false) String title,
                                                    @RequestParam(required = false) String description,
                                                    @RequestParam(required = false) String topic,
                                                    @RequestParam(required = false) Integer difficulty,
                                                    @RequestParam(required = false) String sideToMove,
                                                    @RequestParam(required = false) String solutionMoves,
                                                    @RequestHeader(value = ROLE_HEADER, required = false) String role,
                                                    @RequestHeader(value = "X-User-Email", required = false) String createdBy) {
        requireCoachOrAdmin(role);
        try {
            Puzzle puzzle = puzzleService.uploadAndRecognize(
                    file,
                    title,
                    description,
                    topic,
                    difficulty,
                    sideToMove,
                    solutionMoves,
                    createdBy
            );
            syncPuzzleMoves(puzzle);
            return ResponseEntity.ok(puzzle);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        } catch (IOException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", "Could not save puzzle image: " + ex.getMessage()));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("error", "AI recognition service interrupted"));
        }
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
        if (payload.getDescription() != null) {
            existing.setDescription(payload.getDescription());
        }
        if (payload.getImagePath() != null) {
            existing.setImagePath(payload.getImagePath());
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
        if (payload.getSideToMove() != null) {
            existing.setSideToMove(payload.getSideToMove());
        }
        if (payload.getSolutionMove() != null) {
            existing.setSolutionMove(payload.getSolutionMove());
        }
        if (payload.getSolutionMoves() != null) {
            existing.setSolutionMoves(payload.getSolutionMoves());
        }
        if (payload.getStatus() != null) {
            existing.setStatus(payload.getStatus());
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

    @PutMapping("/{id}/review")
    public Puzzle review(@PathVariable Long id,
                         @RequestBody Puzzle payload,
                         @RequestHeader(value = ROLE_HEADER, required = false) String role) {
        requireCoachOrAdmin(role);
        Puzzle reviewed = puzzleService.review(id, payload);
        syncPuzzleMoves(reviewed);
        return puzzleRepository.save(reviewed);
    }

    @PutMapping("/{id}/publish")
    public Puzzle publish(@PathVariable Long id,
                          @RequestBody(required = false) PuzzlePublishRequest payload,
                          @RequestHeader(value = ROLE_HEADER, required = false) String role) {
        requireCoachOrAdmin(role);
        Puzzle puzzle = puzzleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Puzzle not found"));
        boolean published = payload == null || payload.published() == null || payload.published();
        puzzle.setPublished(published);
        puzzle.setStatus(published ? "PUBLISHED" : "PENDING_REVIEW");
        return puzzleRepository.save(puzzle);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id,
                                       @RequestHeader(value = ROLE_HEADER, required = false) String role) {
        requireCoachOrAdmin(role);
        if (!puzzleRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Puzzle not found");
        }
        puzzleRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/import")
    public ResponseEntity<?> importCsv(@RequestBody PuzzleImportRequest request,
                                       @RequestHeader(value = ROLE_HEADER, required = false) String role) {
        requireCoachOrAdmin(role);
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Import request is required");
        }
        PuzzleCsvImportService.ImportResult result = importService.importFromCsv(
            request.path(),
            request.limit(),
            request.offset(),
            request.published(),
            request.skipExisting()
        );
        return ResponseEntity.ok(result);
    }

    @PostMapping(value = "/process-upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProcessPuzzleUploadResponse> processUpload(@RequestParam(required = false) MultipartFile file,
                                                                     @RequestParam(required = false) String extractedText,
                                                                     @RequestParam(required = false) String title,
                                                                     @RequestParam(required = false) String theme,
                                                                     @RequestParam(required = false) Integer difficulty,
                                                                     @RequestParam(required = false) String side,
                                                                     @RequestParam(required = false) String solutionMove,
                                                                     @RequestParam(required = false) Boolean published,
                                                                     @RequestHeader(value = ROLE_HEADER, required = false) String role) {
        requireCoachOrAdmin(role);
        try {
            PuzzleUploadResult processed = uploadProcessingService.process(
                    file,
                    extractedText,
                    new PuzzleUploadOptions(title, theme, difficulty, side, solutionMove, published)
            );
            Puzzle puzzle = processed.puzzle();
            syncPuzzleMoves(puzzle);
            Puzzle saved = puzzleRepository.save(puzzle);
            return ResponseEntity.ok(new ProcessPuzzleUploadResponse(saved, processed.extractedText(), processed.solutionDetected()));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }
    @GetMapping("/themes")
    public List<ThemeCountResponse> getThemes(@RequestParam(required = false) Boolean published,
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
                .map(entry -> new ThemeCountResponse(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparingInt(ThemeCountResponse::count).reversed().thenComparing(ThemeCountResponse::theme))
                .collect(Collectors.toList());
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
        if ((solutionMove == null || solutionMove.isBlank()) && puzzle.getSolutionMoves() != null) {
            String[] tokens = puzzle.getSolutionMoves().trim().split("[,\\s]+");
            if (tokens.length > 0 && !tokens[0].isBlank()) {
                solutionMove = tokens[0];
                puzzle.setSolutionMove(solutionMove);
            }
        }
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

    private Pageable buildPageable(Integer limit, Integer offset, Sort sort) {
        if (limit == null || limit <= 0) {
            return null;
        }
        int safeOffset = offset != null && offset >= 0 ? offset : 0;
        int page = safeOffset / limit;
        if (sort == null || sort.isUnsorted()) {
            return PageRequest.of(page, limit);
        }
        return PageRequest.of(page, limit, sort);
    }

    private List<Puzzle> fetchPaged(String theme, Integer difficulty, boolean publishedOnly, Pageable pageable) {
        Page<Puzzle> page;
        if (publishedOnly) {
            if (theme != null && difficulty != null) {
                page = puzzleRepository.findByPublishedTrueAndThemeContainingIgnoreCaseAndDifficulty(theme, difficulty, pageable);
            } else if (theme != null) {
                page = puzzleRepository.findByPublishedTrueAndThemeContainingIgnoreCase(theme, pageable);
            } else if (difficulty != null) {
                page = puzzleRepository.findByPublishedTrueAndDifficulty(difficulty, pageable);
            } else {
                page = puzzleRepository.findByPublishedTrue(pageable);
            }
        } else {
            if (theme != null && difficulty != null) {
                page = puzzleRepository.findByThemeContainingIgnoreCaseAndDifficulty(theme, difficulty, pageable);
            } else if (theme != null) {
                page = puzzleRepository.findByThemeContainingIgnoreCase(theme, pageable);
            } else if (difficulty != null) {
                page = puzzleRepository.findByDifficulty(difficulty, pageable);
            } else {
                page = puzzleRepository.findAll(pageable);
            }
        }
        return page.getContent();
    }
}
