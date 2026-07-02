package com.fyp.fypsystem.service.vision;

import com.fyp.fypsystem.dto.vision.ChessVisionResult;
import com.fyp.fypsystem.dto.vision.VisionStatusResponse;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ChessVisionService {

    private static final Pattern JSON_STRING_VALUE = Pattern.compile("\"%s\"\\s*:\\s*\"((?:\\\\.|[^\"])*)\"");
    private static final Pattern JSON_INT_VALUE = Pattern.compile("\"%s\"\\s*:\\s*(\\d+)");
    private static final Pattern JSON_BOOLEAN_VALUE = Pattern.compile("\"%s\"\\s*:\\s*(true|false)");
    private final String pythonCommand;
    private final String scriptPath;
    private final String modelPath;

    public ChessVisionService(@Value("${puzzle.vision.python:python}") String pythonCommand,
                              @Value("${puzzle.vision.script:tools/vision/chess_vision_to_fen.py}") String scriptPath,
                              @Value("${puzzle.vision.model:models/chess-yolo11/best.pt}") String modelPath) {
        this.pythonCommand = pythonCommand;
        this.scriptPath = scriptPath;
        this.modelPath = modelPath;
    }

    public Optional<ChessVisionResult> detectFen(MultipartFile file, String side) throws IOException {
        if (file == null || file.isEmpty()) {
            return Optional.empty();
        }

        byte[] imageBytes = toImageBytes(file);
        if (imageBytes.length == 0) {
            return Optional.empty();
        }

        Path tempImage = Files.createTempFile("chess-upload-", ".png");
        try {
            Files.write(tempImage, imageBytes);
            ProcessBuilder builder = new ProcessBuilder(
                    pythonCommand,
                    scriptPath,
                    "--image", tempImage.toAbsolutePath().toString(),
                    "--model", modelPath,
                    "--side", "b".equalsIgnoreCase(side) ? "b" : "w"
            );
            builder.redirectErrorStream(true);
            Process process = builder.start();
            boolean finished;
            try {
                finished = process.waitFor(Duration.ofSeconds(45).toMillis(), TimeUnit.MILLISECONDS);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new IOException("Vision processing interrupted", ex);
            }
            if (!finished) {
                process.destroyForcibly();
                throw new IOException("Vision processing timed out");
            }

            String output;
            try (ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
                process.getInputStream().transferTo(buffer);
                output = buffer.toString(StandardCharsets.UTF_8);
            }
            String json = readLastJsonLine(output);
            Optional<String> error = readString(json, "error");
            if (error.isPresent()) {
                throw new IOException(error.get());
            }
            Optional<String> fen = readString(json, "fen");
            if (fen.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(new ChessVisionResult(
                    fen.get(),
                    readInt(json, "pieceCount").orElse(0),
                    readBoolean(json, "boardDetected").orElse(false),
                    json
            ));
        } finally {
            Files.deleteIfExists(tempImage);
        }
    }

    public VisionStatusResponse status() {
        boolean pythonAvailable = isPythonAvailable();
        boolean scriptExists = Files.exists(Path.of(scriptPath));
        boolean modelExists = Files.exists(Path.of(modelPath));
        boolean ready = pythonAvailable && scriptExists && modelExists;
        String message;
        if (ready) {
            message = "Vision setup is ready.";
        } else if (!pythonAvailable) {
            message = "Python command is not available. Check puzzle.vision.python.";
        } else if (!scriptExists) {
            message = "Vision script is missing. Check puzzle.vision.script.";
        } else {
            message = "YOLO model is missing. Train/export best.pt and place it at puzzle.vision.model.";
        }
        return new VisionStatusResponse(ready, pythonAvailable, scriptExists, modelExists,
                pythonCommand, scriptPath, modelPath, message);
    }

    private byte[] toImageBytes(MultipartFile file) throws IOException {
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        if (contentType.contains("pdf") || filename.endsWith(".pdf")) {
            try (PDDocument document = Loader.loadPDF(file.getBytes())) {
                if (document.getNumberOfPages() == 0) {
                    return new byte[0];
                }
                PDFRenderer renderer = new PDFRenderer(document);
                BufferedImage image = renderer.renderImageWithDPI(0, 220, ImageType.RGB);
                try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                    ImageIO.write(image, "png", output);
                    return output.toByteArray();
                }
            }
        }
        if (contentType.startsWith("image/") || filename.endsWith(".png") || filename.endsWith(".jpg")
                || filename.endsWith(".jpeg") || filename.endsWith(".webp")) {
            return file.getBytes();
        }
        return new byte[0];
    }

    private String readLastJsonLine(String output) throws IOException {
        String[] lines = output.split("\\R");
        for (int i = lines.length - 1; i >= 0; i--) {
            String line = lines[i].trim();
            if (line.startsWith("{") && line.endsWith("}")) {
                return line;
            }
        }
        throw new IOException("Vision processor did not return JSON");
    }

    private boolean isPythonAvailable() {
        ProcessBuilder builder = new ProcessBuilder(pythonCommand, "--version");
        builder.redirectErrorStream(true);
        try {
            Process process = builder.start();
            return process.waitFor(Duration.ofSeconds(8).toMillis(), TimeUnit.MILLISECONDS) && process.exitValue() == 0;
        } catch (IOException ex) {
            return false;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private Optional<String> readString(String json, String key) {
        Matcher matcher = Pattern.compile(JSON_STRING_VALUE.pattern().formatted(Pattern.quote(key))).matcher(json);
        if (!matcher.find()) {
            return Optional.empty();
        }
        return Optional.of(matcher.group(1).replace("\\\"", "\"").replace("\\\\", "\\"));
    }

    private Optional<Integer> readInt(String json, String key) {
        Matcher matcher = Pattern.compile(JSON_INT_VALUE.pattern().formatted(Pattern.quote(key))).matcher(json);
        if (!matcher.find()) {
            return Optional.empty();
        }
        return Optional.of(Integer.parseInt(matcher.group(1)));
    }

    private Optional<Boolean> readBoolean(String json, String key) {
        Matcher matcher = Pattern.compile(JSON_BOOLEAN_VALUE.pattern().formatted(Pattern.quote(key))).matcher(json);
        if (!matcher.find()) {
            return Optional.empty();
        }
        return Optional.of(Boolean.parseBoolean(matcher.group(1)));
    }
}
