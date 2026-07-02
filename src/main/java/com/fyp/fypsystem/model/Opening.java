package com.fyp.fypsystem.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "openings", indexes = {
    @Index(name = "idx_move_sequence", columnList = "move_sequence", unique = true)
})
public class Opening {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "move_sequence", nullable = false, unique = true, length = 500)
    private String moveSequence;

    @Column(name = "opening_name", nullable = false, length = 255)
    private String openingName;

    public Opening() {}

    public Opening(String moveSequence, String openingName) {
        this.moveSequence = moveSequence;
        this.openingName = openingName;
    }

    public Long getId() { return id; }
    public String getMoveSequence() { return moveSequence; }
    public void setMoveSequence(String moveSequence) { this.moveSequence = moveSequence; }
    public String getOpeningName() { return openingName; }
    public void setOpeningName(String openingName) { this.openingName = openingName; }
}
