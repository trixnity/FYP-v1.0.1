package com.fyp.fypsystem.dto.puzzle;

import com.fyp.fypsystem.model.Puzzle;

public record ProcessPuzzleUploadResponse(Puzzle puzzle, String extractedText, boolean solutionDetected) {
}
