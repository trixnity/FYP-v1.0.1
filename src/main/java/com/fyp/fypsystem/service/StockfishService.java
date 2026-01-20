package com.fyp.fypsystem.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class StockfishService {
    private static final Pattern SCORE_PATTERN = Pattern.compile("score (cp|mate) (-?\\d+)");
    private static final Pattern MULTIPV_PATTERN = Pattern.compile("\\bmultipv (\\d+)\\b");
    private static final Pattern DEPTH_PATTERN = Pattern.compile("\\bdepth (\\d+)\\b");
    private static final Pattern PV_PATTERN = Pattern.compile(" pv (.+)$");

    @Value("${stockfish.path:}")
    private String stockfishPath;

    public AnalysisResult analyze(String fen, Integer depth, Integer movetimeMs, Integer multiPv) {
        if (fen == null || fen.isBlank()) {
            throw new IllegalArgumentException("FEN is required");
        }
        if (stockfishPath == null || stockfishPath.isBlank()) {
            throw new IllegalStateException("Stockfish path not configured");
        }
        Path enginePath = Path.of(stockfishPath);
        if (!Files.exists(enginePath)) {
            throw new IllegalStateException("Stockfish path not found: " + stockfishPath);
        }

        ProcessBuilder builder = new ProcessBuilder(enginePath.toString());
        builder.redirectErrorStream(true);
        Process process = null;

        try {
            process = builder.start();
            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));
                 BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {

                send(writer, "uci");
                waitForToken(reader, "uciok", 2000);

                int linesRequested = multiPv != null && multiPv > 0 ? multiPv : 3;
                send(writer, "setoption name MultiPV value " + linesRequested);

                send(writer, "isready");
                waitForToken(reader, "readyok", 2000);

                send(writer, "ucinewgame");
                send(writer, "position fen " + fen);

                int movetime = movetimeMs != null && movetimeMs > 0 ? movetimeMs : 800;
                if (depth != null && depth > 0) {
                    send(writer, "go depth " + depth);
                } else {
                    send(writer, "go movetime " + movetime);
                }

                String bestMove = null;
                String evaluation = null;
                String pv = null;
                Map<Integer, LineCandidate> lineCandidates = new HashMap<>();
                long deadline = System.currentTimeMillis() + Math.max(2000, movetime + 1500);

                while (System.currentTimeMillis() < deadline) {
                    String line = readLineWithTimeout(reader, 250);
                    if (line == null) {
                        continue;
                    }
                    if (line.startsWith("info")) {
                        int multipv = 1;
                        Matcher multipvMatch = MULTIPV_PATTERN.matcher(line);
                        if (multipvMatch.find()) {
                            multipv = Integer.parseInt(multipvMatch.group(1));
                        }
                        int lineDepth = -1;
                        Matcher depthMatch = DEPTH_PATTERN.matcher(line);
                        if (depthMatch.find()) {
                            lineDepth = Integer.parseInt(depthMatch.group(1));
                        }

                        Matcher scoreMatch = SCORE_PATTERN.matcher(line);
                        String lineEval = null;
                        if (scoreMatch.find()) {
                            lineEval = scoreMatch.group(1) + " " + scoreMatch.group(2);
                            evaluation = lineEval;
                        }
                        Matcher pvMatch = PV_PATTERN.matcher(line);
                        String linePv = null;
                        if (pvMatch.find()) {
                            linePv = pvMatch.group(1);
                            pv = linePv;
                        }

                        if (multipv <= linesRequested) {
                            LineCandidate candidate = lineCandidates.getOrDefault(multipv, new LineCandidate());
                            if (lineDepth > candidate.depth) {
                                candidate.depth = lineDepth;
                            }
                            if (lineEval != null) {
                                candidate.evaluation = lineEval;
                            }
                            if (linePv != null) {
                                candidate.pv = linePv;
                                String[] parts = linePv.trim().split("\\s+");
                                if (parts.length > 0) {
                                    candidate.move = parts[0];
                                }
                            }
                            lineCandidates.put(multipv, candidate);
                        }
                    } else if (line.startsWith("bestmove")) {
                        String[] parts = line.split("\\s+");
                        if (parts.length >= 2) {
                            bestMove = parts[1];
                        }
                        break;
                    }
                }

                if (bestMove == null || bestMove.isBlank()) {
                    throw new IllegalStateException("Stockfish did not return a bestmove");
                }

                LineCandidate topLine = lineCandidates.get(1);
                if (topLine != null) {
                    if (topLine.evaluation != null) {
                        evaluation = topLine.evaluation;
                    }
                    if (topLine.pv != null) {
                        pv = topLine.pv;
                    }
                }

                List<AnalysisLine> lines = new ArrayList<>();
                for (int i = 1; i <= linesRequested; i += 1) {
                    LineCandidate candidate = lineCandidates.get(i);
                    if (candidate == null) {
                        continue;
                    }
                    String move = candidate.move;
                    if (move == null && i == 1) {
                        move = bestMove;
                    }
                    if (move == null) {
                        continue;
                    }
                    lines.add(new AnalysisLine(i, move, candidate.evaluation, candidate.pv));
                }

                return new AnalysisResult(bestMove, evaluation, pv, lines);
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Stockfish failed to start", ex);
        } finally {
            if (process != null) {
                process.destroy();
                try {
                    if (!process.waitFor(200, TimeUnit.MILLISECONDS)) {
                        process.destroyForcibly();
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    process.destroyForcibly();
                }
            }
        }
    }

    private void send(BufferedWriter writer, String command) throws IOException {
        writer.write(command);
        writer.newLine();
        writer.flush();
    }

    private void waitForToken(BufferedReader reader, String token, long timeoutMs) throws IOException {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < timeoutMs) {
            String line = readLineWithTimeout(reader, 200);
            if (line == null) {
                continue;
            }
            if (line.contains(token)) {
                return;
            }
        }
        throw new IllegalStateException("Stockfish did not respond with " + token);
    }

    private String readLineWithTimeout(BufferedReader reader, long timeoutMs) throws IOException {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < timeoutMs) {
            if (reader.ready()) {
                return reader.readLine();
            }
            try {
                Thread.sleep(10);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return null;
            }
        }
        return null;
    }

    private static class LineCandidate {
        int depth = -1;
        String move;
        String evaluation;
        String pv;
    }

    public record AnalysisLine(int rank, String move, String evaluation, String pv) {
    }

    public record AnalysisResult(String bestMove, String evaluation, String pv, List<AnalysisLine> lines) {
    }
}
