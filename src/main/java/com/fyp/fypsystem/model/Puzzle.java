
package com.fyp.fypsystem.model;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "puzzles")
public class Puzzle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    @Column(columnDefinition = "TEXT")
    private String description;
    private String imagePath;
    private String theme;
    private Integer difficulty;
    @Column(columnDefinition = "TEXT")
    private String fen;
    private String side; // "w" or "b"
    private String solutionMove; // single move solution (uci)
    @Column(columnDefinition = "TEXT")
    private String solutionMoves;
    private String sideToMove;
    private Double recognitionConfidence;
    private String status;
    private String createdBy;
    private String createdAt;
    private Boolean published = Boolean.FALSE;

    @OneToMany(mappedBy = "puzzle", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("moveOrder ASC")
    private List<PuzzleMove> moves = new ArrayList<>();

    public Puzzle() {
    }

    public Puzzle(String title, String theme, Integer difficulty, String fen, String side) {
        this.title = title;
        this.theme = theme;
        this.difficulty = difficulty;
        this.fen = fen;
        this.side = side;
    }

    public Puzzle(String title, String theme, Integer difficulty, String fen, String side, String solutionMove) {
        this.title = title;
        this.theme = theme;
        this.difficulty = difficulty;
        this.fen = fen;
        this.side = side;
        this.solutionMove = solutionMove;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = java.time.LocalDateTime.now().toString();
        }
        if (sideToMove == null || sideToMove.isBlank()) {
            sideToMove = side;
        }
        if (side == null || side.isBlank()) {
            side = sideToMove;
        }
        if (solutionMoves == null || solutionMoves.isBlank()) {
            solutionMoves = solutionMove;
        }
        if (solutionMove == null || solutionMove.isBlank()) {
            solutionMove = firstSolutionMove(solutionMoves);
        }
        if (status == null || status.isBlank()) {
            status = Boolean.TRUE.equals(published) ? "PUBLISHED" : "PENDING_REVIEW";
        }
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public String getTheme() {
        return theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }

    public String getTopic() {
        return theme;
    }

    public void setTopic(String topic) {
        this.theme = topic;
    }

    public Integer getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(Integer difficulty) {
        this.difficulty = difficulty;
    }

    public Integer getLevel() {
        return difficulty;
    }

    public void setLevel(Integer level) {
        this.difficulty = level;
    }

    public String getFen() {
        return fen;
    }

    public void setFen(String fen) {
        this.fen = fen;
    }

    public String getSide() {
        return side;
    }

    public void setSide(String side) {
        this.side = side;
        this.sideToMove = side;
    }

    public String getSolutionMove() {
        return solutionMove;
    }

    public void setSolutionMove(String solutionMove) {
        this.solutionMove = solutionMove;
        if (this.solutionMoves == null || this.solutionMoves.isBlank()) {
            this.solutionMoves = solutionMove;
        }
    }

    public String getSolutionMoves() {
        return solutionMoves;
    }

    public void setSolutionMoves(String solutionMoves) {
        this.solutionMoves = solutionMoves;
        if (this.solutionMove == null || this.solutionMove.isBlank()) {
            this.solutionMove = firstSolutionMove(solutionMoves);
        }
    }

    public String getSideToMove() {
        return sideToMove;
    }

    public void setSideToMove(String sideToMove) {
        this.sideToMove = sideToMove;
        this.side = sideToMove;
    }

    public Double getRecognitionConfidence() {
        return recognitionConfidence;
    }

    public void setRecognitionConfidence(Double recognitionConfidence) {
        this.recognitionConfidence = recognitionConfidence;
    }

    public String getStatus() {
        if (status == null || status.isBlank()) {
            return Boolean.TRUE.equals(published) ? "PUBLISHED" : "PENDING_REVIEW";
        }
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
        if ("PUBLISHED".equalsIgnoreCase(status)) {
            this.published = Boolean.TRUE;
        } else if ("PENDING_REVIEW".equalsIgnoreCase(status)) {
            this.published = Boolean.FALSE;
        }
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public Boolean getPublished() {
        return published;
    }

    public void setPublished(Boolean published) {
        this.published = published;
        this.status = Boolean.TRUE.equals(published) ? "PUBLISHED" : "PENDING_REVIEW";
    }

    public List<PuzzleMove> getMoves() {
        return moves;
    }

    public void setMoves(List<PuzzleMove> moves) {
        this.moves = moves;
    }

    private String firstSolutionMove(String moves) {
        if (moves == null || moves.isBlank()) {
            return null;
        }
        String[] tokens = moves.trim().split("[,\\s]+");
        return tokens.length > 0 ? tokens[0] : null;
    }
}
