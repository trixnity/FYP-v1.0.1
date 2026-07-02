package com.fyp.fypsystem.dto.vision;

public record VisionStatusResponse(boolean ready, boolean pythonAvailable, boolean scriptExists, boolean modelExists,
                                   String pythonCommand, String scriptPath, String modelPath, String message) {
}
