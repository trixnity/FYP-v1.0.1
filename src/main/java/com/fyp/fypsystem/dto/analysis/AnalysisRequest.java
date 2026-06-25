package com.fyp.fypsystem.dto.analysis;

public record AnalysisRequest(String fen, Integer depth, Integer movetime, Integer multipv) {
}
