package com.fyp.fypsystem.dto.analysis;

import com.fyp.fypsystem.service.StockfishService;

import java.util.List;

public record AnalysisResponse(String bestMove, String evaluation, String pv,
                               List<StockfishService.AnalysisLine> lines) {
}
