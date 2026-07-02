package com.fyp.fypsystem.controller;

import com.fyp.fypsystem.dto.puzzle.PuzzleUploadLibraryItem;
import com.fyp.fypsystem.service.PuzzleUploadLibraryService;
import com.fyp.fypsystem.service.PuzzleUploadLibraryService.PuzzleUploadFile;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/puzzle-uploads")
@CrossOrigin(origins = "*")
public class PuzzleUploadController {

    private final PuzzleUploadLibraryService puzzleUploadLibraryService;

    public PuzzleUploadController(PuzzleUploadLibraryService puzzleUploadLibraryService) {
        this.puzzleUploadLibraryService = puzzleUploadLibraryService;
    }

    @GetMapping
    public List<PuzzleUploadLibraryItem> list() {
        return puzzleUploadLibraryService.list();
    }

    @GetMapping("/{id}/file")
    public ResponseEntity<Resource> file(@PathVariable Long id) {
        try {
            PuzzleUploadFile file = puzzleUploadLibraryService.loadFile(id);
            MediaType mediaType = parseMediaType(file.contentType());
            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                            .filename(file.originalFilename())
                            .build()
                            .toString())
                    .body(new PathResource(file.path()));
        } catch (NoSuchElementException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        }
    }

    @PostMapping
    public List<PuzzleUploadLibraryItem> upload(@RequestParam("files") List<MultipartFile> files,
                                                @RequestHeader(value = "X-User-Email", required = false) String uploadedBy) {
        try {
            return puzzleUploadLibraryService.saveAll(files, uploadedBy);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        } catch (IllegalStateException | IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
        }
    }

    private MediaType parseMediaType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        try {
            return MediaType.parseMediaType(contentType);
        } catch (IllegalArgumentException ex) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}
