package com.fyp.fypsystem.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.fyp.fypsystem.model.Student;
import com.fyp.fypsystem.repository.StudentRepository;

@RestController
@RequestMapping("/api/students")
@CrossOrigin(origins = "*")
public class StudentController {

    private final StudentRepository repo;

    public StudentController(StudentRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public List<Student> getAll() {
        return repo.findAll();
    }

    @PostMapping
    public Student create(@RequestBody Student student) {
        repo.findByEmail(student.getEmail()).ifPresent(existing -> {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        });
        return repo.save(student);
    }

    @GetMapping("/{id}")
    public Student getOne(@PathVariable Long id) {
        return repo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));
    }

    @GetMapping("/coach/{coachId}")
    public List<Student> getByCoach(@PathVariable Long coachId) {
        return repo.findByCoachId(coachId);
    }

    @PutMapping("/{id}")
    public Student update(@PathVariable Long id, @RequestBody Student updated) {
        Student existing = repo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));
        Optional.ofNullable(updated.getName()).ifPresent(existing::setName);
        Optional.ofNullable(updated.getEmail()).ifPresent(email -> {
            repo.findByEmail(email).filter(s -> !s.getId().equals(id)).ifPresent(conflict -> {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
            });
            existing.setEmail(email);
        });
        Optional.ofNullable(updated.getCoachId()).ifPresent(existing::setCoachId);
        if (updated.getPuzzlesSolved() != null) {
            existing.setPuzzlesSolved(updated.getPuzzlesSolved());
        }
        return repo.save(existing);
    }

    @PostMapping("/{id}/puzzles/solved")
    public Student incrementPuzzles(@PathVariable Long id, @RequestParam(name = "inc", defaultValue = "1") int inc) {
        Student existing = repo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));
        int current = existing.getPuzzlesSolved() != null ? existing.getPuzzlesSolved() : 0;
        existing.setPuzzlesSolved(Math.max(0, current + inc));
        return repo.save(existing);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        if (!repo.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found");
        }
        repo.deleteById(id);
    }
}
