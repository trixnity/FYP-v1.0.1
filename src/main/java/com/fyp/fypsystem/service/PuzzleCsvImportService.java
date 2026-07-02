package com.fyp.fypsystem.service;

import com.fyp.fypsystem.model.Puzzle;
import com.fyp.fypsystem.model.PuzzleMove;
import com.fyp.fypsystem.repository.PuzzleRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Service
public class PuzzleCsvImportService {
    private static final int DEFAULT_LIMIT = 500;
    private static final int BATCH_SIZE = 200;

    private final PuzzleRepository puzzleRepository;

    @Value("${puzzles.import.path:}")
    private String defaultPath;

    public PuzzleCsvImportService(PuzzleRepository puzzleRepository) {
        this.puzzleRepository = puzzleRepository;
    }

    public ImportResult importFromCsv(String pathOverride,
                                      Integer limit,
                                      Integer offset,
                                      Boolean published,
                                      Boolean skipExisting) {
        Path csvPath = resolvePath(pathOverride);
        if (!Files.exists(csvPath)) {
            throw new IllegalArgumentException("CSV path not found: " + csvPath);
        }

        int max = limit != null && limit > 0 ? limit : DEFAULT_LIMIT;
        int skip = offset != null && offset > 0 ? offset : 0;
        boolean publish = Boolean.TRUE.equals(published);
        boolean skipDupes = skipExisting == null || skipExisting;

        int imported = 0;
        int skipped = 0;
        int processed = 0;
        List<Puzzle> batch = new ArrayList<>();

        try (BufferedReader reader = Files.newBufferedReader(csvPath, StandardCharsets.UTF_8)) {
            String header = reader.readLine();
            if (header == null) {
                return new ImportResult(0, 0, 0);
            }

            String line;
            while ((line = reader.readLine()) != null) {
                if (skip > 0) {
                    skip--;
                    continue;
                }
                processed++;

                List<String> cols = parseCsvLine(line);
                if (cols.size() < 8) {
                    skipped++;
                    continue;
                }

                String puzzleId = cols.get(0).trim();
                String fen = cols.get(1).trim();
                String movesRaw = cols.get(2).trim();
                String ratingRaw = cols.get(3).trim();
                String themes = cols.get(7).trim();

                if (fen.isBlank() || movesRaw.isBlank()) {
                    skipped++;
                    continue;
                }

                String legacyTitle = "Lichess " + puzzleId;
                String title = "Imported Puzzle " + puzzleId;
                if (skipDupes && (puzzleRepository.existsByTitle(title) || puzzleRepository.existsByTitle(legacyTitle))) {
                    skipped++;
                    continue;
                }

                String[] moves = movesRaw.split("\\s+");
                if (moves.length == 0 || moves[0].isBlank()) {
                    skipped++;
                    continue;
                }

                Integer rating = parseIntSafe(ratingRaw);
                String side = moves.length > 1 ? oppositeSide(extractSide(fen)) : extractSide(fen);
                String studentFirstMove = moves.length > 1 ? moves[1] : moves[0];
                Puzzle puzzle = new Puzzle(title, themes, rating, fen, side, studentFirstMove);
                puzzle.setPublished(publish);

                List<PuzzleMove> moveList = new ArrayList<>();
                int order = 1;
                for (String mv : moves) {
                    if (mv == null || mv.isBlank()) {
                        continue;
                    }
                    PuzzleMove move = new PuzzleMove(puzzle, order, mv);
                    moveList.add(move);
                    order++;
                }
                puzzle.setMoves(moveList);

                batch.add(puzzle);
                imported++;

                if (batch.size() >= BATCH_SIZE) {
                    puzzleRepository.saveAll(batch);
                    batch.clear();
                }

                if (imported >= max) {
                    break;
                }
            }

            if (!batch.isEmpty()) {
                puzzleRepository.saveAll(batch);
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read CSV", ex);
        }

        return new ImportResult(imported, skipped, processed);
    }

    private Path resolvePath(String override) {
        String resolved = override != null && !override.isBlank() ? override : defaultPath;
        if (resolved == null || resolved.isBlank()) {
            throw new IllegalArgumentException("CSV path is required");
        }
        return Path.of(resolved);
    }

    private Integer parseIntSafe(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String extractSide(String fen) {
        String[] parts = fen.split("\\s+");
        if (parts.length >= 2 && ("w".equals(parts[1]) || "b".equals(parts[1]))) {
            return parts[1];
        }
        return "w";
    }

    private String oppositeSide(String side) {
        return "b".equals(side) ? "w" : "b";
    }

    private List<String> parseCsvLine(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    sb.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
                continue;
            }
            if (c == ',' && !inQuotes) {
                out.add(sb.toString());
                sb.setLength(0);
                continue;
            }
            sb.append(c);
        }
        out.add(sb.toString());
        return out;
    }

    public record ImportResult(int imported, int skipped, int processed) {
    }
}
