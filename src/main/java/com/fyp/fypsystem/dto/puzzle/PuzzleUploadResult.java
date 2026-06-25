package com.fyp.fypsystem.dto.puzzle;

import com.fyp.fypsystem.model.Puzzle;

public record PuzzleUploadResult(Puzzle puzzle, String extractedText, boolean solutionDetected) {
}
