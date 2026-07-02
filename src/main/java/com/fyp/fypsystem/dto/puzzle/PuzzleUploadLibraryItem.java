package com.fyp.fypsystem.dto.puzzle;

import java.time.LocalDateTime;

public record PuzzleUploadLibraryItem(Long id, String originalFilename, String contentType, Long fileSize,
                                      String status, String uploadedBy, LocalDateTime uploadedAt, Long puzzleId) {
}
