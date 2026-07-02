package com.fyp.fypsystem.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "saved_games")
public class SavedGame {

    public enum GameResult {
        WHITE_WIN,
        BLACK_WIN,
        DRAW,
        UNKNOWN
    }

    public enum GameSource {
        ANALYSIS_BOARD
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "student_id")
    private Long studentId;

    private String ownerEmail;

    @Column(name = "game_name")
    private String gameName;

    private String whitePlayer;
    private String blackPlayer;

    @Enumerated(EnumType.STRING)
    private GameResult result = GameResult.UNKNOWN;

    @Column(columnDefinition = "TEXT")
    private String fen;

    @Column(columnDefinition = "LONGTEXT")
    private String pgn;

    @Column(name = "moves_json", columnDefinition = "LONGTEXT")
    private String movesJson;

    @Column(name = "move_annotations_json", columnDefinition = "LONGTEXT")
    private String moveAnnotationsJson;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Enumerated(EnumType.STRING)
    private GameSource source = GameSource.ANALYSIS_BOARD;

    private String eventName;
    private String round;
    private String site;
    private LocalDate gameDate;

    @Column(columnDefinition = "LONGTEXT")
    private String moveHistoryJson;

    @Column(columnDefinition = "LONGTEXT")
    private String openingName;
    private Boolean analysisEnabled = Boolean.TRUE;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }

    public String getOwnerEmail() { return ownerEmail; }
    public void setOwnerEmail(String ownerEmail) { this.ownerEmail = ownerEmail; }

    public String getGameName() { return gameName; }
    public void setGameName(String gameName) { this.gameName = gameName; }

    public String getTitle() { return gameName; }
    public void setTitle(String title) { this.gameName = title; }

    public String getWhitePlayer() { return whitePlayer; }
    public void setWhitePlayer(String whitePlayer) { this.whitePlayer = whitePlayer; }

    public String getBlackPlayer() { return blackPlayer; }
    public void setBlackPlayer(String blackPlayer) { this.blackPlayer = blackPlayer; }

    public String getWinner() {
        if (result == GameResult.WHITE_WIN) return "WHITE";
        if (result == GameResult.BLACK_WIN) return "BLACK";
        if (result == GameResult.DRAW) return "DRAW";
        return "ONGOING";
    }

    public void setWinner(String winner) {
        if ("WHITE".equalsIgnoreCase(winner)) result = GameResult.WHITE_WIN;
        else if ("BLACK".equalsIgnoreCase(winner)) result = GameResult.BLACK_WIN;
        else if ("DRAW".equalsIgnoreCase(winner)) result = GameResult.DRAW;
        else result = GameResult.UNKNOWN;
    }

    public GameResult getResult() { return result == null ? GameResult.UNKNOWN : result; }
    public void setResult(GameResult result) { this.result = result == null ? GameResult.UNKNOWN : result; }

    public String getFen() { return fen; }
    public void setFen(String fen) { this.fen = fen; }

    public String getCurrentFen() { return fen; }
    public void setCurrentFen(String currentFen) { this.fen = currentFen; }

    public String getPgn() { return pgn; }
    public void setPgn(String pgn) { this.pgn = pgn; }

    public String getMovesJson() { return movesJson; }
    public void setMovesJson(String movesJson) {
        this.movesJson = movesJson;
        this.moveHistoryJson = movesJson;
    }

    public String getMoveAnnotationsJson() { return moveAnnotationsJson; }
    public void setMoveAnnotationsJson(String moveAnnotationsJson) { this.moveAnnotationsJson = moveAnnotationsJson; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public GameSource getSource() { return source == null ? GameSource.ANALYSIS_BOARD : source; }
    public void setSource(GameSource source) { this.source = source == null ? GameSource.ANALYSIS_BOARD : source; }

    public String getEventName() { return eventName; }
    public void setEventName(String eventName) { this.eventName = eventName; }

    public String getRound() { return round; }
    public void setRound(String round) { this.round = round; }

    public String getSite() { return site; }
    public void setSite(String site) { this.site = site; }

    public LocalDate getGameDate() { return gameDate; }
    public void setGameDate(LocalDate gameDate) { this.gameDate = gameDate; }

    public String getMoveHistoryJson() { return movesJson != null ? movesJson : moveHistoryJson; }
    public void setMoveHistoryJson(String moveHistoryJson) {
        this.moveHistoryJson = moveHistoryJson;
        this.movesJson = moveHistoryJson;
    }

    public String getOpeningName() { return openingName; }
    public void setOpeningName(String openingName) { this.openingName = openingName; }

    public Boolean getAnalysisEnabled() { return analysisEnabled; }
    public void setAnalysisEnabled(Boolean analysisEnabled) { this.analysisEnabled = analysisEnabled; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
