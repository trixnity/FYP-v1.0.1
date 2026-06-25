package com.fyp.fypsystem.dto.vision;

public record ChessVisionResult(String fen, int pieceCount, boolean boardDetected, String rawJson) {
}
