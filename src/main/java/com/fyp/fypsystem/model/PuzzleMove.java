package com.fyp.fypsystem.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "puzzle_moves")
public class PuzzleMove {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "puzzle_id")
    private Puzzle puzzle;

    private Integer moveOrder;
    private String moveUci;

    public PuzzleMove() {
    }

    public PuzzleMove(Puzzle puzzle, Integer moveOrder, String moveUci) {
        this.puzzle = puzzle;
        this.moveOrder = moveOrder;
        this.moveUci = moveUci;
    }

    public Long getId() {
        return id;
    }

    public Puzzle getPuzzle() {
        return puzzle;
    }

    public void setPuzzle(Puzzle puzzle) {
        this.puzzle = puzzle;
    }

    public Integer getMoveOrder() {
        return moveOrder;
    }

    public void setMoveOrder(Integer moveOrder) {
        this.moveOrder = moveOrder;
    }

    public String getMoveUci() {
        return moveUci;
    }

    public void setMoveUci(String moveUci) {
        this.moveUci = moveUci;
    }
}
