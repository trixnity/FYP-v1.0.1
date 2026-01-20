package com.fyp.fypsystem;

import com.fyp.fypsystem.model.Puzzle;
import com.fyp.fypsystem.model.PuzzleMove;
import com.fyp.fypsystem.model.Attempt;
import com.fyp.fypsystem.repository.PuzzleRepository;
import com.fyp.fypsystem.repository.AttemptRepository;
import java.time.LocalDateTime;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class FypApplication {

	public static void main(String[] args) {
		SpringApplication.run(FypApplication.class, args);
	}

	@Bean
	CommandLineRunner seedPuzzles(PuzzleRepository puzzleRepository, AttemptRepository attemptRepository) {
		return args -> {
			if (puzzleRepository.count() > 0) return;

			Puzzle sample = new Puzzle(
					"Mate in 1",
					"checkmate",
					900,
					"6k1/5ppp/8/8/8/8/5PPP/6K1 w - - 0 1",
					"w"
			);
			PuzzleMove move = new PuzzleMove(sample, 1, "g2g3");
			sample.getMoves().add(move);
			sample.setSolutionMove("g2g3");
			sample.setPublished(true);
			puzzleRepository.save(sample);

			// Additional mate-in-one: queen mating
			Puzzle queenMate = new Puzzle(
					"Queen mating (mate in 1)",
					"checkmate",
					1000,
					"1k6/8/QK6/8/8/8/8/8 w - - 0 1",
					"w"
			);
			PuzzleMove queenMove = new PuzzleMove(queenMate, 1, "a6b7");
			queenMate.getMoves().add(queenMove);
			queenMate.setSolutionMove("a6b7");
			queenMate.setPublished(true);
			puzzleRepository.save(queenMate);

			// Seed a few attempts for demo stats
			attemptRepository.save(new Attempt(sample.getId(), 1L, "demo@user.com", true, 3, 1, sample.getTheme(), LocalDateTime.now()));
			attemptRepository.save(new Attempt(queenMate.getId(), 1L, "demo@user.com", false, 0, 1, queenMate.getTheme(), null));
		};
	}

}
