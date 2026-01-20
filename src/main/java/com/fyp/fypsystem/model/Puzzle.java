
package com.fyp.fypsystem.model;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

@Entity
@Table(name = "puzzles")
public class Puzzle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String theme;
    private Integer difficulty;
    private String fen;
    private String side; // "w" or "b"
    private String solutionMove; // single move solution (uci)
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

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
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
    }

    public String getSolutionMove() {
        return solutionMove;
    }

    public void setSolutionMove(String solutionMove) {
        this.solutionMove = solutionMove;
    }

    public Boolean getPublished() {
        return published;
    }

    public void setPublished(Boolean published) {
        this.published = published;
    }

    public List<PuzzleMove> getMoves() {
        return moves;
    }

    public void setMoves(List<PuzzleMove> moves) {
        this.moves = moves;
    }
}
