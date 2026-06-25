package com.fyp.fypsystem.dto.puzzle;

public record PuzzleImportRequest(String path, Integer limit, Integer offset, Boolean published, Boolean skipExisting) {
}
