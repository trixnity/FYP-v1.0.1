package com.fyp.fypsystem.dto.puzzle;

public record PuzzleUploadOptions(String title, String theme, Integer difficulty, String side, String solutionMove,
                                  Boolean published) {
}
