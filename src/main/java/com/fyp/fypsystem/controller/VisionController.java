package com.fyp.fypsystem.controller;

import com.fyp.fypsystem.dto.vision.VisionStatusResponse;
import com.fyp.fypsystem.service.vision.ChessVisionService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/vision")
@CrossOrigin(origins = "*")
public class VisionController {

    private final ChessVisionService chessVisionService;

    public VisionController(ChessVisionService chessVisionService) {
        this.chessVisionService = chessVisionService;
    }

    @GetMapping("/status")
    public VisionStatusResponse status() {
        return chessVisionService.status();
    }
}
