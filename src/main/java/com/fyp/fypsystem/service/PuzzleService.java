package com.fyp.fypsystem.service;
import com.fyp.fypsystem.model.Puzzle;
import com.fyp.fypsystem.repository.PuzzleRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PuzzleService {

    private final PuzzleRepository puzzleRepository;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final String aiBaseUrl;
    private final Path storageDir;

    public PuzzleService(PuzzleRepository puzzleRepository,
                         @Value("${puzzle.ai.base-url:}") String aiBaseUrl,
                         @Value("${puzzle.recognition.storage-dir:uploads/puzzles}") String storageDir) {
        this.puzzleRepository = puzzleRepository;
        this.aiBaseUrl = aiBaseUrl;
        this.storageDir = Path.of(storageDir);
    }

    public Puzzle uploadAndRecognize(MultipartFile file,
                                     String title,
                                     String description,
                                     String topic,
                                     Integer difficulty,
                                     String sideToMove,
                                     String solutionMoves,
                                     String createdBy) throws IOException, InterruptedException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Puzzle image is required.");
        }

        Files.createDirectories(storageDir);
        String original = file.getOriginalFilename() == null ? "puzzle.png" : file.getOriginalFilename();
        String extension = extensionOf(original);
        String filename = UUID.randomUUID() + extension;
        Path imagePath = storageDir.resolve(filename).normalize();
        Files.copy(file.getInputStream(), imagePath);

        Map<String, Object> recognition = callRecognitionService(imagePath, normalizeSide(sideToMove));
        boolean success = Boolean.TRUE.equals(recognition.get("success"));
        String message = stringValue(recognition.get("message"));
        String fen = stringValue(recognition.get("fen"));
        if (!success || fen == null || fen.isBlank()) {
            throw new IllegalArgumentException(message != null ? message : "Puzzle recognition failed.");
        }

        Puzzle puzzle = new Puzzle();
        puzzle.setTitle(firstNonBlank(title, "Uploaded puzzle " + LocalDateTime.now().toLocalDate()));
        puzzle.setDescription(firstNonBlank(description, message));
        puzzle.setTheme(firstNonBlank(topic, "uploaded"));
        puzzle.setDifficulty(difficulty);
        puzzle.setImagePath(imagePath.toString());
        puzzle.setFen(fen);
        puzzle.setSideToMove(normalizeSide(sideToMove));
        puzzle.setSolutionMoves(solutionMoves);
        puzzle.setSolutionMove(firstSolutionMove(solutionMoves));
        puzzle.setRecognitionConfidence(doubleValue(recognition.get("confidence")));
        puzzle.setStatus("PENDING_REVIEW");
        puzzle.setCreatedBy(createdBy);
        return puzzleRepository.save(puzzle);
    }

    public Puzzle review(Long id, Puzzle payload) {
        Puzzle puzzle = puzzleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Puzzle not found"));
        if (payload.getTitle() != null) puzzle.setTitle(payload.getTitle());
        if (payload.getDescription() != null) puzzle.setDescription(payload.getDescription());
        if (payload.getTheme() != null) puzzle.setTheme(payload.getTheme());
        if (payload.getTopic() != null) puzzle.setTopic(payload.getTopic());
        if (payload.getDifficulty() != null) puzzle.setDifficulty(payload.getDifficulty());
        if (payload.getFen() != null) puzzle.setFen(payload.getFen());
        if (payload.getSideToMove() != null) puzzle.setSideToMove(payload.getSideToMove());
        if (payload.getSide() != null) puzzle.setSide(payload.getSide());
        if (payload.getSolutionMoves() != null) puzzle.setSolutionMoves(payload.getSolutionMoves());
        if (payload.getSolutionMove() != null) puzzle.setSolutionMove(payload.getSolutionMove());
        if (payload.getStatus() != null) puzzle.setStatus(payload.getStatus());
        return puzzleRepository.save(puzzle);
    }

    private Map<String, Object> callRecognitionService(Path imagePath, String sideToMove) throws IOException, InterruptedException {
        String boundary = "EduChessBoundary" + UUID.randomUUID();
        byte[] fileBytes = Files.readAllBytes(imagePath);
        String filename = imagePath.getFileName().toString();

        byte[] body = multipartBody(boundary, filename, fileBytes, sideToMove);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(aiBaseUrl.replaceAll("/+$", "") + "/recognize-puzzle"))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new IllegalArgumentException("AI recognition service returned HTTP " + response.statusCode());
        }
        return parseRecognitionJson(response.body());
    }

    private byte[] multipartBody(String boundary, String filename, byte[] fileBytes, String sideToMove) throws IOException {
        String line = "\r\n";
        StringBuilder head = new StringBuilder();
        head.append("--").append(boundary).append(line);
        head.append("Content-Disposition: form-data; name=\"sideToMove\"").append(line).append(line);
        head.append(sideToMove).append(line);
        head.append("--").append(boundary).append(line);
        head.append("Content-Disposition: form-data; name=\"file\"; filename=\"").append(filename).append("\"").append(line);
        head.append("Content-Type: image/png").append(line).append(line);
        String tail = line + "--" + boundary + "--" + line;

        byte[] headBytes = head.toString().getBytes(StandardCharsets.UTF_8);
        byte[] tailBytes = tail.getBytes(StandardCharsets.UTF_8);
        byte[] body = new byte[headBytes.length + fileBytes.length + tailBytes.length];
        System.arraycopy(headBytes, 0, body, 0, headBytes.length);
        System.arraycopy(fileBytes, 0, body, headBytes.length, fileBytes.length);
        System.arraycopy(tailBytes, 0, body, headBytes.length + fileBytes.length, tailBytes.length);
        return body;
    }

    private String extensionOf(String filename) {
        int idx = filename.lastIndexOf('.');
        return idx >= 0 ? filename.substring(idx) : ".png";
    }

    private String normalizeSide(String side) {
        return "b".equalsIgnoreCase(side) ? "b" : "w";
    }

    private String firstNonBlank(String preferred, String fallback) {
        return preferred != null && !preferred.isBlank() ? preferred.trim() : fallback;
    }

    private String firstSolutionMove(String moves) {
        if (moves == null || moves.isBlank()) return null;
        String[] tokens = moves.trim().split("[,\\s]+");
        return tokens.length > 0 ? tokens[0] : null;
    }

    private String stringValue(Object value) {
        return value == null ? null : value.toString();
    }

    private Double doubleValue(Object value) {
        if (value == null) return null;
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Map<String, Object> parseRecognitionJson(String json) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("success", Boolean.parseBoolean(extractJsonScalar(json, "success", "false")));
        map.put("fen", extractJsonString(json, "fen"));
        map.put("message", extractJsonString(json, "message"));
        map.put("confidence", extractJsonScalar(json, "confidence", "0"));
        return map;
    }

    private String extractJsonString(String json, String key) {
        String pattern = "\"" + Pattern.quote(key) + "\"\\s*:\\s*\"((?:\\\\.|[^\"])*)\"";
        Matcher matcher = Pattern.compile(pattern).matcher(json);
        if (!matcher.find()) return null;
        return matcher.group(1)
                .replace("\\\"", "\"")
                .replace("\\\\", "\\")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t");
    }

    private String extractJsonScalar(String json, String key, String fallback) {
        String pattern = "\"" + Pattern.quote(key) + "\"\\s*:\\s*([^,}\\s]+)";
        Matcher matcher = Pattern.compile(pattern).matcher(json);
        return matcher.find() ? matcher.group(1) : fallback;
    }
}
