package com.fyp.fypsystem.service;

import com.fyp.fypsystem.dto.puzzle.PuzzleUploadOptions;
import com.fyp.fypsystem.dto.puzzle.PuzzleUploadResult;
import com.fyp.fypsystem.dto.vision.ChessVisionResult;
import com.fyp.fypsystem.model.Puzzle;
import com.fyp.fypsystem.service.vision.ChessVisionService;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PuzzleUploadProcessingService {

    private static final Pattern FEN_PATTERN = Pattern.compile(
            "([rnbqkpRNBQKP1-8]+/[rnbqkpRNBQKP1-8]+/[rnbqkpRNBQKP1-8]+/[rnbqkpRNBQKP1-8]+/[rnbqkpRNBQKP1-8]+/[rnbqkpRNBQKP1-8]+/[rnbqkpRNBQKP1-8]+/[rnbqkpRNBQKP1-8]+)(?:\\s+([wb])(?:\\s+(-|[KQkq]+)\\s+(-|[a-h][36])\\s+(\\d+)\\s+(\\d+))?)?",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern LABELLED_MOVE_PATTERN = Pattern.compile(
            "(?i)(?:solution|answer|best\\s*move|move|winning\\s*move)\\s*[:\\-]?\\s*([KQRBN]?[a-h]?[1-8]?x?[a-h][1-8](?:=[QRBN])?[+#]?|O-O(?:-O)?[+#]?|[a-h][1-8][a-h][1-8][qrbn]?)");
    private static final Pattern UCI_PATTERN = Pattern.compile("\\b[a-h][1-8][a-h][1-8][qrbn]?\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern TITLE_PATTERN = Pattern.compile("(?im)^(?:title|puzzle)\\s*[:\\-]\\s*(.+)$");
    private final ChessVisionService chessVisionService;

    public PuzzleUploadProcessingService(ChessVisionService chessVisionService) {
        this.chessVisionService = chessVisionService;
    }

    public PuzzleUploadResult process(MultipartFile file, String suppliedText, PuzzleUploadOptions options) throws IOException {
        String text = combineText(extractFileText(file), suppliedText);

        String side = normalizeSide(options.side());
        Optional<String> textFen = findFen(text, side);
        Optional<ChessVisionResult> visionResult = textFen.isEmpty()
                ? chessVisionService.detectFen(file, side)
                : Optional.empty();
        String fen = textFen.or(() -> visionResult.map(ChessVisionResult::fen))
                .orElseThrow(() -> new IllegalArgumentException("Could not find a FEN in text and vision detection did not return one."));
        side = fen.split("\\s+")[1];

        String solution = firstNonBlank(options.solutionMove(), findSolution(text).orElse(null));
        String theme = firstNonBlank(options.theme(), "uploaded");
        String title = firstNonBlank(options.title(), findTitle(text).orElse("Uploaded puzzle"));

        Puzzle puzzle = new Puzzle();
        puzzle.setTitle(title);
        puzzle.setTheme(theme);
        puzzle.setDifficulty(options.difficulty());
        puzzle.setFen(fen);
        puzzle.setSide(side);
        puzzle.setSolutionMove(solution);
        puzzle.setPublished(options.published() != null ? options.published() : Boolean.FALSE);
        String extracted = buildExtractedText(text, visionResult);
        return new PuzzleUploadResult(puzzle, extracted, solution != null && !solution.isBlank());
    }

    private String extractFileText(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return "";
        }
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        if (contentType.contains("pdf") || filename.endsWith(".pdf")) {
            try (PDDocument document = Loader.loadPDF(file.getBytes())) {
                return new PDFTextStripper().getText(document);
            }
        }
        return "";
    }

    private String combineText(String fileText, String suppliedText) {
        StringBuilder builder = new StringBuilder();
        if (fileText != null && !fileText.isBlank()) {
            builder.append(fileText).append('\n');
        }
        if (suppliedText != null && !suppliedText.isBlank()) {
            builder.append(suppliedText);
        }
        return builder.toString();
    }

    private Optional<String> findFen(String text, String fallbackSide) {
        Matcher matcher = FEN_PATTERN.matcher(text);
        while (matcher.find()) {
            String board = matcher.group(1);
            if (!isValidFenBoard(board)) {
                continue;
            }
            String side = matcher.group(2) != null ? matcher.group(2).toLowerCase(Locale.ROOT) : fallbackSide;
            String castling = matcher.group(3) != null ? matcher.group(3) : "-";
            String enPassant = matcher.group(4) != null ? matcher.group(4) : "-";
            String halfmove = matcher.group(5) != null ? matcher.group(5) : "0";
            String fullmove = matcher.group(6) != null ? matcher.group(6) : "1";
            return Optional.of(String.join(" ", board, side, castling, enPassant, halfmove, fullmove));
        }
        return Optional.empty();
    }

    private boolean isValidFenBoard(String board) {
        String[] ranks = board.split("/");
        if (ranks.length != 8) {
            return false;
        }
        for (String rank : ranks) {
            int count = 0;
            for (int i = 0; i < rank.length(); i++) {
                char c = rank.charAt(i);
                if (Character.isDigit(c)) {
                    count += Character.digit(c, 10);
                } else if ("rnbqkpRNBQKP".indexOf(c) >= 0) {
                    count++;
                } else {
                    return false;
                }
            }
            if (count != 8) {
                return false;
            }
        }
        return true;
    }

    private Optional<String> findSolution(String text) {
        Matcher labelled = LABELLED_MOVE_PATTERN.matcher(text);
        if (labelled.find()) {
            return Optional.of(labelled.group(1));
        }
        Matcher uci = UCI_PATTERN.matcher(text);
        if (uci.find()) {
            return Optional.of(uci.group().toLowerCase(Locale.ROOT));
        }
        return Optional.empty();
    }

    private Optional<String> findTitle(String text) {
        Matcher matcher = TITLE_PATTERN.matcher(text);
        if (matcher.find()) {
            return Optional.of(matcher.group(1).trim());
        }
        return Optional.empty();
    }

    private String normalizeSide(String side) {
        return "b".equalsIgnoreCase(side) ? "b" : "w";
    }

    private String firstNonBlank(String preferred, String fallback) {
        return preferred != null && !preferred.isBlank() ? preferred.trim() : fallback;
    }

    private String buildExtractedText(String text, Optional<ChessVisionResult> visionResult) {
        StringBuilder builder = new StringBuilder();
        if (text != null && !text.isBlank()) {
            builder.append(text.trim());
        }
        visionResult.ifPresent(result -> {
            if (!builder.isEmpty()) {
                builder.append("\n\n");
            }
            builder.append("Vision FEN: ").append(result.fen())
                    .append("\nDetected pieces: ").append(result.pieceCount())
                    .append("\nBoard detected: ").append(result.boardDetected());
        });
        return builder.toString();
    }
}
