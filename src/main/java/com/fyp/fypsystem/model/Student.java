package com.fyp.fypsystem.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "students")
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(unique = true, nullable = false)
    private String email;
    private Long coachId;          // optional link to User (coach)
    private Integer puzzlesSolved = 0; // simple progress metric

    public Student() {
    }

    public Student(String name, String email) {
        this.name = name;
        this.email = email;
    }

    public Student(String name, String email, Long coachId) {
        this.name = name;
        this.email = email;
        this.coachId = coachId;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Long getCoachId() {
        return coachId;
    }

    public void setCoachId(Long coachId) {
        this.coachId = coachId;
    }

    public Integer getPuzzlesSolved() {
        return puzzlesSolved;
    }

    public void setPuzzlesSolved(Integer puzzlesSolved) {
        this.puzzlesSolved = puzzlesSolved;
    }
}
