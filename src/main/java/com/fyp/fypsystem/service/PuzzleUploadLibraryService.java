package com.fyp.fypsystem.service;

import com.fyp.fypsystem.dto.puzzle.PuzzleUploadLibraryItem;
import com.fyp.fypsystem.model.PuzzleUpload;
import com.fyp.fypsystem.repository.PuzzleUploadRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class PuzzleUploadLibraryService {

    private final PuzzleUploadRepository puzzleUploadRepository;
    private final Path storageDir;

    public PuzzleUploadLibraryService(PuzzleUploadRepository puzzleUploadRepository,
                                      @Value("${puzzle.upload.storage-dir:uploads/puzzle-library}") String storageDir) {
        this.puzzleUploadRepository = puzzleUploadRepository;
        this.storageDir = Path.of(storageDir);
    }

    public List<PuzzleUploadLibraryItem> list() {
        return puzzleUploadRepository.findAllByOrderByUploadedAtDesc().stream()
                .map(this::toItem)
                .toList();
    }

    public List<PuzzleUploadLibraryItem> saveAll(List<MultipartFile> files, String uploadedBy) throws IOException {
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("At least one file is required");
        }
        Files.createDirectories(storageDir);
        return files.stream()
                .filter(file -> file != null && !file.isEmpty())
                .map(file -> saveOne(file, uploadedBy))
                .map(this::toItem)
                .toList();
    }

    public PuzzleUploadFile loadFile(Long id) {
        PuzzleUpload upload = puzzleUploadRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Puzzle upload not found"));
        Path root = storageDir.toAbsolutePath().normalize();
        Path file = root.resolve(upload.getStoredFilename()).normalize();
        if (!file.startsWith(root) || !Files.exists(file) || !Files.isRegularFile(file)) {
            throw new NoSuchElementException("Puzzle upload file not found");
        }
        return new PuzzleUploadFile(file, upload.getOriginalFilename(), upload.getContentType());
    }

    private PuzzleUpload saveOne(MultipartFile file, String uploadedBy) {
        validateFile(file);
        String original = cleanFilename(file.getOriginalFilename());
        String extension = extensionOf(original);
        String stored = UUID.randomUUID() + extension;
        Path target = storageDir.resolve(stored).normalize();
        try {
            file.transferTo(target);
        } catch (IOException ex) {
            throw new IllegalStateException("Could not store " + original, ex);
        }

        PuzzleUpload upload = new PuzzleUpload();
        upload.setOriginalFilename(original);
        upload.setStoredFilename(stored);
        upload.setContentType(file.getContentType());
        upload.setFileSize(file.getSize());
        upload.setStatus("UPLOADED");
        upload.setUploadedBy(uploadedBy == null || uploadedBy.isBlank() ? "unknown" : uploadedBy.trim());
        upload.setUploadedAt(LocalDateTime.now());
        return puzzleUploadRepository.save(upload);
    }

    private void validateFile(MultipartFile file) {
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        boolean accepted = contentType.startsWith("image/")
                || contentType.contains("pdf")
                || filename.endsWith(".png")
                || filename.endsWith(".jpg")
                || filename.endsWith(".jpeg")
                || filename.endsWith(".webp")
                || filename.endsWith(".pdf");
        if (!accepted) {
            throw new IllegalArgumentException("Only image and PDF puzzle files are allowed");
        }
    }

    private PuzzleUploadLibraryItem toItem(PuzzleUpload upload) {
        return new PuzzleUploadLibraryItem(
                upload.getId(),
                upload.getOriginalFilename(),
                upload.getContentType(),
                upload.getFileSize(),
                upload.getStatus(),
                upload.getUploadedBy(),
                upload.getUploadedAt(),
                upload.getPuzzleId()
        );
    }

    private String cleanFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "puzzle-upload";
        }
        return Path.of(filename).getFileName().toString().replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String extensionOf(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return "";
        }
        return filename.substring(dot).toLowerCase(Locale.ROOT);
    }

    public record PuzzleUploadFile(Path path, String originalFilename, String contentType) {
    }
}
