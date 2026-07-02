package com.fyp.fypsystem.config;

import com.fyp.fypsystem.model.Opening;
import com.fyp.fypsystem.repository.OpeningRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashMap;
import java.util.Map;

@Configuration
public class OpeningBookSeeder {

    @Bean
    public CommandLineRunner seedOpenings(OpeningRepository repo) {
        return args -> {
            if (repo.count() > 0) return;

            Map<String, String> book = new LinkedHashMap<>();

            // 1. e4
            book.put("e2e4", "King's Pawn Opening");
            // Open game
            book.put("e2e4 e7e5", "Open Game");
            book.put("e2e4 e7e5 g1f3", "King's Knight Opening");
            book.put("e2e4 e7e5 g1f3 b8c6", "Three Knights Game");
            // Ruy Lopez
            book.put("e2e4 e7e5 g1f3 b8c6 f1b5", "Ruy Lopez");
            book.put("e2e4 e7e5 g1f3 b8c6 f1b5 a7a6", "Ruy Lopez: Morphy Defense");
            book.put("e2e4 e7e5 g1f3 b8c6 f1b5 a7a6 b5a4", "Ruy Lopez: Morphy Defense");
            book.put("e2e4 e7e5 g1f3 b8c6 f1b5 a7a6 b5a4 g8f6", "Ruy Lopez: Morphy Defense");
            book.put("e2e4 e7e5 g1f3 b8c6 f1b5 a7a6 b5a4 g8f6 e1g1", "Ruy Lopez: Closed");
            book.put("e2e4 e7e5 g1f3 b8c6 f1b5 a7a6 b5a4 g8f6 e1g1 f8e7", "Ruy Lopez: Closed");
            book.put("e2e4 e7e5 g1f3 b8c6 f1b5 a7a6 b5a4 g8f6 e1g1 f8e7 f1e1", "Ruy Lopez: Closed");
            book.put("e2e4 e7e5 g1f3 b8c6 f1b5 a7a6 b5a4 g8f6 e1g1 f8e7 f1e1 b7b5", "Ruy Lopez: Closed Main Line");
            book.put("e2e4 e7e5 g1f3 b8c6 f1b5 a7a6 b5a4 g8f6 e1g1 f8e7 f1e1 b7b5 a4b3 d7d6", "Ruy Lopez: Chigorin");
            book.put("e2e4 e7e5 g1f3 b8c6 f1b5 a7a6 b5a4 b7b5", "Ruy Lopez: Arkhangelsk");
            book.put("e2e4 e7e5 g1f3 b8c6 f1b5 f8c5", "Ruy Lopez: Classical");
            book.put("e2e4 e7e5 g1f3 b8c6 f1b5 g8f6", "Ruy Lopez: Berlin Defense");
            book.put("e2e4 e7e5 g1f3 b8c6 f1b5 g8f6 e1g1", "Ruy Lopez: Berlin Defense");
            book.put("e2e4 e7e5 g1f3 b8c6 f1b5 g8f6 e1g1 f6e4", "Ruy Lopez: Berlin Wall");
            book.put("e2e4 e7e5 g1f3 b8c6 f1b5 d7d6", "Ruy Lopez: Steinitz Defense");
            book.put("e2e4 e7e5 g1f3 b8c6 f1b5 b8d4", "Ruy Lopez: Bird's Defense");
            // Italian
            book.put("e2e4 e7e5 g1f3 b8c6 f1c4", "Italian Game");
            book.put("e2e4 e7e5 g1f3 b8c6 f1c4 f8c5", "Italian: Giuoco Piano");
            book.put("e2e4 e7e5 g1f3 b8c6 f1c4 f8c5 c2c3", "Italian: Giuoco Pianissimo");
            book.put("e2e4 e7e5 g1f3 b8c6 f1c4 f8c5 c2c3 g8f6", "Italian: Giuoco Pianissimo");
            book.put("e2e4 e7e5 g1f3 b8c6 f1c4 f8c5 c2c3 g8f6 d2d4", "Italian: Giuoco Piano");
            book.put("e2e4 e7e5 g1f3 b8c6 f1c4 f8c5 b2b4", "Italian: Evans Gambit");
            book.put("e2e4 e7e5 g1f3 b8c6 f1c4 f8c5 b2b4 c5b4", "Italian: Evans Gambit Accepted");
            book.put("e2e4 e7e5 g1f3 b8c6 f1c4 f8c5 b2b4 c5b4 c2c3", "Italian: Evans Gambit");
            book.put("e2e4 e7e5 g1f3 b8c6 f1c4 g8f6", "Italian: Two Knights Defense");
            book.put("e2e4 e7e5 g1f3 b8c6 f1c4 g8f6 d2d3", "Italian: Two Knights, Anti-Fried Liver");
            book.put("e2e4 e7e5 g1f3 b8c6 f1c4 g8f6 b1c3", "Italian: Three Knights");
            book.put("e2e4 e7e5 g1f3 b8c6 f1c4 g8f6 f3g5", "Italian: Two Knights, Fried Liver Attack");
            book.put("e2e4 e7e5 g1f3 b8c6 f1c4 g8f6 d2d4", "Italian: Two Knights, Scotch Attack");
            // Scotch
            book.put("e2e4 e7e5 g1f3 b8c6 d2d4", "Scotch Game");
            book.put("e2e4 e7e5 g1f3 b8c6 d2d4 e5d4", "Scotch Game");
            book.put("e2e4 e7e5 g1f3 b8c6 d2d4 e5d4 f3d4", "Scotch Game");
            book.put("e2e4 e7e5 g1f3 b8c6 d2d4 e5d4 f3d4 f8c5", "Scotch: Classical");
            book.put("e2e4 e7e5 g1f3 b8c6 d2d4 e5d4 f3d4 g8f6", "Scotch: Schmidt Variation");
            book.put("e2e4 e7e5 g1f3 b8c6 d2d4 e5d4 f3d4 d8h4", "Scotch: Steinitz Variation");
            book.put("e2e4 e7e5 g1f3 b8c6 d2d4 e5d4 f3d4 b8d4", "Scotch: Mieses-Kotroc");
            // Four Knights
            book.put("e2e4 e7e5 g1f3 b8c6 b1c3", "Three Knights Game");
            book.put("e2e4 e7e5 g1f3 b8c6 b1c3 g8f6", "Four Knights Game");
            book.put("e2e4 e7e5 g1f3 b8c6 b1c3 g8f6 f1b5", "Four Knights: Spanish");
            book.put("e2e4 e7e5 g1f3 b8c6 b1c3 g8f6 d2d4", "Scotch Four Knights");
            book.put("e2e4 e7e5 g1f3 b8c6 b1c3 g8f6 f1c4", "Four Knights: Italian");
            // Petrov
            book.put("e2e4 e7e5 g1f3 g8f6", "Petrov's Defense");
            book.put("e2e4 e7e5 g1f3 g8f6 f3e5", "Petrov's Defense");
            book.put("e2e4 e7e5 g1f3 g8f6 f3e5 d7d6", "Petrov's Defense: Classical");
            book.put("e2e4 e7e5 g1f3 g8f6 f3e5 d7d6 e5f3", "Petrov's Defense: Classical");
            book.put("e2e4 e7e5 g1f3 g8f6 f3e5 d7d6 e5f3 f6e4", "Petrov's Defense: Classical");
            book.put("e2e4 e7e5 g1f3 g8f6 d2d4", "Petrov's Defense: Steinitz Attack");
            book.put("e2e4 e7e5 g1f3 g8f6 b1c3", "Petrov's Defense: Three Knights");
            // King's Gambit
            book.put("e2e4 e7e5 f2f4", "King's Gambit");
            book.put("e2e4 e7e5 f2f4 e5f4", "King's Gambit Accepted");
            book.put("e2e4 e7e5 f2f4 e5f4 g1f3", "King's Gambit Accepted: Kieseritzky");
            book.put("e2e4 e7e5 f2f4 e5f4 g1f3 g7g5", "King's Gambit: Kieseritzky");
            book.put("e2e4 e7e5 f2f4 e5f4 f1c4", "King's Gambit: Bishop's Gambit");
            book.put("e2e4 e7e5 f2f4 e5f4 h2h4", "King's Gambit Accepted: McDonnell");
            book.put("e2e4 e7e5 f2f4 f8c5", "King's Gambit Declined: Classical");
            book.put("e2e4 e7e5 f2f4 d7d5", "King's Gambit Declined: Falkbeer");
            book.put("e2e4 e7e5 f2f4 d7d5 e4d5", "King's Gambit Declined: Falkbeer Counter Gambit");
            // Vienna
            book.put("e2e4 e7e5 b1c3", "Vienna Game");
            book.put("e2e4 e7e5 b1c3 g8f6", "Vienna Game: Falkbeer");
            book.put("e2e4 e7e5 b1c3 f8c5", "Vienna Game: Meitner-Mieses Gambit");
            book.put("e2e4 e7e5 b1c3 b8c6", "Vienna Game");
            book.put("e2e4 e7e5 b1c3 b8c6 f2f4", "Vienna Gambit");
            book.put("e2e4 e7e5 b1c3 b8c6 g2g3", "Vienna Game: Anderssen Defense");
            // Latvian / Elephant
            book.put("e2e4 e7e5 g1f3 f7f5", "Latvian Gambit");
            book.put("e2e4 e7e5 g1f3 d7d5", "Elephant Gambit");
            // Sicilian
            book.put("e2e4 c7c5", "Sicilian Defense");
            book.put("e2e4 c7c5 g1f3", "Sicilian Defense");
            book.put("e2e4 c7c5 g1f3 d7d6", "Sicilian Defense");
            book.put("e2e4 c7c5 g1f3 b8c6", "Sicilian Defense");
            book.put("e2e4 c7c5 g1f3 e7e6", "Sicilian Defense");
            book.put("e2e4 c7c5 g1f3 d7d6 d2d4", "Sicilian: Open");
            book.put("e2e4 c7c5 g1f3 d7d6 d2d4 c5d4", "Sicilian: Open");
            book.put("e2e4 c7c5 g1f3 d7d6 d2d4 c5d4 f3d4", "Sicilian: Open");
            book.put("e2e4 c7c5 g1f3 d7d6 d2d4 c5d4 f3d4 g8f6", "Sicilian: Open");
            book.put("e2e4 c7c5 g1f3 d7d6 d2d4 c5d4 f3d4 g8f6 b1c3", "Sicilian: Open");
            book.put("e2e4 c7c5 g1f3 d7d6 d2d4 c5d4 f3d4 g8f6 b1c3 a7a6", "Sicilian: Najdorf");
            book.put("e2e4 c7c5 g1f3 d7d6 d2d4 c5d4 f3d4 g8f6 b1c3 a7a6 f1e2", "Sicilian: Najdorf, Classical");
            book.put("e2e4 c7c5 g1f3 d7d6 d2d4 c5d4 f3d4 g8f6 b1c3 a7a6 c1g5", "Sicilian: Najdorf, English Attack");
            book.put("e2e4 c7c5 g1f3 d7d6 d2d4 c5d4 f3d4 g8f6 b1c3 a7a6 f2f3", "Sicilian: Najdorf, English Attack");
            book.put("e2e4 c7c5 g1f3 d7d6 d2d4 c5d4 f3d4 g8f6 b1c3 e7e6", "Sicilian: Scheveningen");
            book.put("e2e4 c7c5 g1f3 d7d6 d2d4 c5d4 f3d4 g8f6 b1c3 g7g6", "Sicilian: Dragon");
            book.put("e2e4 c7c5 g1f3 d7d6 d2d4 c5d4 f3d4 g8f6 b1c3 g7g6 f1e2", "Sicilian: Dragon, Classical");
            book.put("e2e4 c7c5 g1f3 d7d6 d2d4 c5d4 f3d4 g8f6 b1c3 g7g6 c1e3", "Sicilian: Dragon, Yugoslav Attack");
            book.put("e2e4 c7c5 g1f3 d7d6 d2d4 c5d4 f3d4 g8f6 b1c3 b8c6", "Sicilian: Classical");
            book.put("e2e4 c7c5 g1f3 b8c6 d2d4", "Sicilian: Open");
            book.put("e2e4 c7c5 g1f3 b8c6 d2d4 c5d4 f3d4", "Sicilian: Open");
            book.put("e2e4 c7c5 g1f3 b8c6 d2d4 c5d4 f3d4 g7g6", "Sicilian: Accelerated Dragon");
            book.put("e2e4 c7c5 g1f3 b8c6 d2d4 c5d4 f3d4 e7e6 b1c3 d8c7", "Sicilian: Taimanov");
            book.put("e2e4 c7c5 g1f3 b8c6 d2d4 c5d4 f3d4 e7e6 b1c3 a7a6", "Sicilian: Paulsen");
            book.put("e2e4 c7c5 g1f3 e7e6 d2d4 c5d4 f3d4 a7a6", "Sicilian: Kan");
            book.put("e2e4 c7c5 g1f3 e7e6 d2d4 c5d4 f3d4 b8c6", "Sicilian: Paulsen");
            book.put("e2e4 c7c5 c2c3", "Sicilian: Alapin");
            book.put("e2e4 c7c5 c2c3 d7d5", "Sicilian: Alapin, ...d5");
            book.put("e2e4 c7c5 c2c3 g8f6", "Sicilian: Alapin, ...Nf6");
            book.put("e2e4 c7c5 c2c3 g8f6 e4e5", "Sicilian: Alapin, ...Nf6");
            book.put("e2e4 c7c5 b1c3", "Sicilian: Closed");
            book.put("e2e4 c7c5 b1c3 b8c6", "Sicilian: Closed");
            book.put("e2e4 c7c5 b1c3 b8c6 g2g3", "Sicilian: Closed");
            book.put("e2e4 c7c5 d2d4 c5d4 c2c3", "Sicilian: Morra Gambit");
            book.put("e2e4 c7c5 d2d4 c5d4 c2c3 d4c3", "Sicilian: Morra Gambit Accepted");
            // French
            book.put("e2e4 e7e6", "French Defense");
            book.put("e2e4 e7e6 d2d4", "French Defense");
            book.put("e2e4 e7e6 d2d4 d7d5", "French Defense");
            book.put("e2e4 e7e6 d2d4 d7d5 b1c3", "French: Classical");
            book.put("e2e4 e7e6 d2d4 d7d5 b1c3 g8f6", "French: Classical");
            book.put("e2e4 e7e6 d2d4 d7d5 b1c3 f8b4", "French: Winawer");
            book.put("e2e4 e7e6 d2d4 d7d5 b1c3 f8b4 e4e5", "French: Winawer, Advance");
            book.put("e2e4 e7e6 d2d4 d7d5 b1c3 f8b4 e4e5 c7c5", "French: Winawer, Advance");
            book.put("e2e4 e7e6 d2d4 d7d5 b1c3 f8b4 a2a3", "French: Winawer, Poisoned Pawn");
            book.put("e2e4 e7e6 d2d4 d7d5 b1d2", "French: Tarrasch");
            book.put("e2e4 e7e6 d2d4 d7d5 b1d2 g8f6", "French: Tarrasch, Open");
            book.put("e2e4 e7e6 d2d4 d7d5 b1d2 c7c5", "French: Tarrasch, Open");
            book.put("e2e4 e7e6 d2d4 d7d5 b1d2 b8c6", "French: Tarrasch, Guimard");
            book.put("e2e4 e7e6 d2d4 d7d5 e4e5", "French: Advance");
            book.put("e2e4 e7e6 d2d4 d7d5 e4e5 c7c5", "French: Advance");
            book.put("e2e4 e7e6 d2d4 d7d5 e4e5 c7c5 c2c3", "French: Advance, Main Line");
            book.put("e2e4 e7e6 d2d4 d7d5 e4d5", "French: Exchange");
            book.put("e2e4 e7e6 d2d4 d7d5 e4d5 e6d5", "French: Exchange");
            book.put("e2e4 e7e6 d2d4 d7d5 g1f3", "French: Two Knights");
            // Caro-Kann
            book.put("e2e4 c7c6", "Caro-Kann Defense");
            book.put("e2e4 c7c6 d2d4", "Caro-Kann Defense");
            book.put("e2e4 c7c6 d2d4 d7d5", "Caro-Kann Defense");
            book.put("e2e4 c7c6 d2d4 d7d5 b1c3", "Caro-Kann: Classical");
            book.put("e2e4 c7c6 d2d4 d7d5 b1c3 d5e4", "Caro-Kann: Classical");
            book.put("e2e4 c7c6 d2d4 d7d5 b1c3 d5e4 c3e4", "Caro-Kann: Classical");
            book.put("e2e4 c7c6 d2d4 d7d5 b1c3 d5e4 c3e4 g8f6", "Caro-Kann: Classical");
            book.put("e2e4 c7c6 d2d4 d7d5 b1c3 d5e4 c3e4 c8f5", "Caro-Kann: Classical, Capablanca");
            book.put("e2e4 c7c6 d2d4 d7d5 b1c3 d5e4 c3e4 c8f5 e4g3", "Caro-Kann: Classical, Capablanca");
            book.put("e2e4 c7c6 d2d4 d7d5 b1d2", "Caro-Kann: Modern");
            book.put("e2e4 c7c6 d2d4 d7d5 b1d2 d5e4", "Caro-Kann: Modern");
            book.put("e2e4 c7c6 d2d4 d7d5 b1d2 d5e4 d2e4", "Caro-Kann: Modern");
            book.put("e2e4 c7c6 d2d4 d7d5 e4e5", "Caro-Kann: Advance");
            book.put("e2e4 c7c6 d2d4 d7d5 e4e5 c8f5", "Caro-Kann: Advance");
            book.put("e2e4 c7c6 d2d4 d7d5 e4e5 c8f5 g1f3", "Caro-Kann: Advance");
            book.put("e2e4 c7c6 d2d4 d7d5 e4d5", "Caro-Kann: Exchange");
            book.put("e2e4 c7c6 d2d4 d7d5 e4d5 c6d5", "Caro-Kann: Exchange");
            // Scandinavian
            book.put("e2e4 d7d5", "Scandinavian Defense");
            book.put("e2e4 d7d5 e4d5", "Scandinavian Defense");
            book.put("e2e4 d7d5 e4d5 d8d5", "Scandinavian: Main Line");
            book.put("e2e4 d7d5 e4d5 d8d5 b1c3", "Scandinavian: Mieses-Kotroc");
            book.put("e2e4 d7d5 e4d5 d8d5 b1c3 d5a5", "Scandinavian: Main Line");
            book.put("e2e4 d7d5 e4d5 d8d5 b1c3 d5d6", "Scandinavian: Gubinsky-Melts");
            book.put("e2e4 d7d5 e4d5 g8f6", "Scandinavian: Modern");
            book.put("e2e4 d7d5 e4d5 g8f6 d2d4", "Scandinavian: Modern");
            // Pirc / Modern / Alekhine
            book.put("e2e4 d7d6", "Pirc Defense");
            book.put("e2e4 d7d6 d2d4", "Pirc Defense");
            book.put("e2e4 d7d6 d2d4 g8f6", "Pirc Defense");
            book.put("e2e4 d7d6 d2d4 g8f6 b1c3", "Pirc Defense");
            book.put("e2e4 d7d6 d2d4 g8f6 b1c3 g7g6", "Pirc Defense: Main Line");
            book.put("e2e4 d7d6 d2d4 g8f6 b1c3 g7g6 f2f4", "Pirc Defense: Austrian Attack");
            book.put("e2e4 d7d6 d2d4 g8f6 b1c3 g7g6 g1f3", "Pirc Defense: Classical");
            book.put("e2e4 g7g6", "Modern Defense");
            book.put("e2e4 g7g6 d2d4 f8g7", "Modern Defense");
            book.put("e2e4 g7g6 d2d4 f8g7 b1c3", "Modern Defense");
            book.put("e2e4 g8f6", "Alekhine's Defense");
            book.put("e2e4 g8f6 e4e5", "Alekhine's Defense");
            book.put("e2e4 g8f6 e4e5 f6d5", "Alekhine's Defense");
            book.put("e2e4 g8f6 e4e5 f6d5 d2d4", "Alekhine's Defense: Modern");
            book.put("e2e4 g8f6 e4e5 f6d5 d2d4 d7d6", "Alekhine's Defense: Modern");
            book.put("e2e4 g8f6 e4e5 f6d5 c2c4", "Alekhine's Defense: Four Pawns Attack");
            book.put("e2e4 g8f6 e4e5 f6d5 c2c4 d5b6 d2d4", "Alekhine's Defense: Four Pawns");
            // Misc 1.e4
            book.put("e2e4 b7b6", "Owen's Defense");
            book.put("e2e4 f7f5", "Fred Defense");
            book.put("e2e4 a7a6", "St. George Defense");
            book.put("e2e4 b7b5", "Polish Defense");

            // 1. d4
            book.put("d2d4", "Queen's Pawn");
            book.put("d2d4 d7d5", "Queen's Pawn Game");
            // Queen's Gambit
            book.put("d2d4 d7d5 c2c4", "Queen's Gambit");
            book.put("d2d4 d7d5 c2c4 e7e6", "Queen's Gambit Declined");
            book.put("d2d4 d7d5 c2c4 e7e6 b1c3", "Queen's Gambit Declined");
            book.put("d2d4 d7d5 c2c4 e7e6 b1c3 g8f6", "Queen's Gambit Declined");
            book.put("d2d4 d7d5 c2c4 e7e6 b1c3 g8f6 c1g5", "Queen's Gambit Declined: Classical");
            book.put("d2d4 d7d5 c2c4 e7e6 b1c3 g8f6 g1f3", "Queen's Gambit Declined: Orthodox");
            book.put("d2d4 d7d5 c2c4 e7e6 b1c3 g8f6 g1f3 f8e7", "Queen's Gambit Declined: Orthodox");
            book.put("d2d4 d7d5 c2c4 e7e6 b1c3 g8f6 g1f3 f8e7 c1f4", "Queen's Gambit Declined: Modern");
            book.put("d2d4 d7d5 c2c4 e7e6 b1c3 g8f6 g1f3 f8e7 c1g5", "Queen's Gambit Declined: Tartakower");
            book.put("d2d4 d7d5 c2c4 e7e6 b1c3 g8f6 g1f3 c7c6", "Queen's Gambit Declined: Semi-Slav");
            book.put("d2d4 d7d5 c2c4 e7e6 g1f3 g8f6 b1c3", "Queen's Gambit Declined");
            // Slav Defense
            book.put("d2d4 d7d5 c2c4 c7c6", "Slav Defense");
            book.put("d2d4 d7d5 c2c4 c7c6 g1f3", "Slav Defense");
            book.put("d2d4 d7d5 c2c4 c7c6 g1f3 g8f6", "Slav Defense");
            book.put("d2d4 d7d5 c2c4 c7c6 g1f3 g8f6 b1c3", "Slav Defense");
            book.put("d2d4 d7d5 c2c4 c7c6 g1f3 g8f6 b1c3 d5c4", "Slav Defense: Accepted");
            book.put("d2d4 d7d5 c2c4 c7c6 g1f3 g8f6 b1c3 e7e6", "Semi-Slav Defense");
            book.put("d2d4 d7d5 c2c4 c7c6 g1f3 g8f6 b1c3 e7e6 c1g5", "Semi-Slav: Anti-Moscow");
            book.put("d2d4 d7d5 c2c4 c7c6 g1f3 g8f6 b1c3 e7e6 e2e3", "Semi-Slav: Meran");
            book.put("d2d4 d7d5 c2c4 c7c6 b1c3", "Slav Defense");
            book.put("d2d4 d7d5 c2c4 c7c6 b1c3 g8f6 g1f3", "Slav Defense");
            // QGA
            book.put("d2d4 d7d5 c2c4 d5c4", "Queen's Gambit Accepted");
            book.put("d2d4 d7d5 c2c4 d5c4 g1f3", "Queen's Gambit Accepted");
            book.put("d2d4 d7d5 c2c4 d5c4 g1f3 g8f6", "Queen's Gambit Accepted");
            book.put("d2d4 d7d5 c2c4 d5c4 e2e4", "Queen's Gambit Accepted: Central Variation");
            book.put("d2d4 d7d5 c2c4 d5c4 e2e3", "Queen's Gambit Accepted: Classical");
            // Tarrasch
            book.put("d2d4 d7d5 c2c4 c7c5", "Tarrasch Defense");
            book.put("d2d4 d7d5 c2c4 c7c5 c4d5", "Tarrasch Defense: Exchange");
            // London System
            book.put("d2d4 d7d5 c1f4", "London System");
            book.put("d2d4 d7d5 c1f4 g8f6", "London System");
            book.put("d2d4 d7d5 c1f4 g8f6 g1f3", "London System");
            book.put("d2d4 d7d5 c1f4 g8f6 g1f3 e7e6", "London System");
            book.put("d2d4 d7d5 c1f4 g8f6 g1f3 c7c5", "London System");
            book.put("d2d4 d7d5 g1f3 g8f6 c1f4", "London System");
            book.put("d2d4 d7d5 g1f3 g8f6 c1f4 e7e6", "London System");
            book.put("d2d4 d7d5 g1f3 g8f6 c1f4 c7c5", "London System");
            book.put("d2d4 d7d5 g1f3 g8f6 c1f4 c7c5 e2e3", "London System");
            book.put("d2d4 g8f6 g1f3 d7d5 c1f4", "London System");
            book.put("d2d4 g8f6 g1f3 d7d5 c1f4 e7e6", "London System");
            book.put("d2d4 g8f6 g1f3 d7d5 c1f4 c7c5", "London System");
            // Colle
            book.put("d2d4 d7d5 g1f3 g8f6 e2e3", "Colle System");
            book.put("d2d4 d7d5 g1f3 g8f6 e2e3 e7e6", "Colle System");
            book.put("d2d4 d7d5 g1f3 g8f6 e2e3 c7c5", "Colle System");
            // Catalan
            book.put("d2d4 g8f6 c2c4 e7e6 g1f3 d7d5 g2g3", "Catalan Opening");
            book.put("d2d4 g8f6 c2c4 e7e6 g1f3 d7d5 g2g3 f8e7 f1g2", "Catalan: Closed");
            book.put("d2d4 g8f6 c2c4 e7e6 g1f3 d7d5 g2g3 d5c4", "Catalan: Open");
            book.put("d2d4 g8f6 c2c4 e7e6 g1f3 d7d5 g2g3 d5c4 f1g2", "Catalan: Open");
            // Veresov / Trompowsky
            book.put("d2d4 d7d5 b1c3", "Veresov Attack");
            book.put("d2d4 d7d5 b1c3 g8f6 c1g5", "Veresov Attack");
            book.put("d2d4 g8f6 c1g5", "Trompowsky Attack");
            book.put("d2d4 g8f6 c1g5 e7e5", "Trompowsky Attack");
            book.put("d2d4 g8f6 c1g5 f6e4", "Trompowsky Attack: Raptor");
            book.put("d2d4 g8f6 g1f3 e7e6 c1g5", "Torre Attack");
            book.put("d2d4 g8f6 g1f3 e7e6 c1g5 h7h6", "Torre Attack");
            // King's Indian Defense
            book.put("d2d4 g8f6", "Indian Defense");
            book.put("d2d4 g8f6 c2c4", "Indian Defense");
            book.put("d2d4 g8f6 c2c4 g7g6", "King's Indian Defense");
            book.put("d2d4 g8f6 c2c4 g7g6 b1c3", "King's Indian Defense");
            book.put("d2d4 g8f6 c2c4 g7g6 b1c3 f8g7", "King's Indian Defense");
            book.put("d2d4 g8f6 c2c4 g7g6 b1c3 f8g7 e2e4", "King's Indian Defense");
            book.put("d2d4 g8f6 c2c4 g7g6 b1c3 f8g7 e2e4 d7d6", "King's Indian Defense");
            book.put("d2d4 g8f6 c2c4 g7g6 b1c3 f8g7 e2e4 d7d6 g1f3", "King's Indian: Main Line");
            book.put("d2d4 g8f6 c2c4 g7g6 b1c3 f8g7 e2e4 d7d6 g1f3 e8g8", "King's Indian: Main Line");
            book.put("d2d4 g8f6 c2c4 g7g6 b1c3 f8g7 e2e4 d7d6 g1f3 e8g8 f1e2", "King's Indian: Classical");
            book.put("d2d4 g8f6 c2c4 g7g6 b1c3 f8g7 e2e4 d7d6 g1f3 e8g8 f1e2 e7e5", "King's Indian: Classical");
            book.put("d2d4 g8f6 c2c4 g7g6 b1c3 f8g7 e2e4 d7d6 g1f3 e8g8 f1e2 e7e5 e1g1", "King's Indian: Classical Main");
            book.put("d2d4 g8f6 c2c4 g7g6 b1c3 f8g7 e2e4 d7d6 f2f3", "King's Indian: Samisch");
            book.put("d2d4 g8f6 c2c4 g7g6 b1c3 f8g7 e2e4 d7d6 g1e2", "King's Indian: Averbakh");
            book.put("d2d4 g8f6 c2c4 g7g6 b1c3 f8g7 e2e4 d7d6 c1e3", "King's Indian: Petrosian");
            book.put("d2d4 g8f6 c2c4 g7g6 b1c3 f8g7 g2g3", "King's Indian: Fianchetto");
            book.put("d2d4 g8f6 c2c4 g7g6 b1c3 f8g7 g2g3 e8g8 f1g2", "King's Indian: Fianchetto");
            // Grunfeld Defense
            book.put("d2d4 g8f6 c2c4 g7g6 b1c3 d7d5", "Grunfeld Defense");
            book.put("d2d4 g8f6 c2c4 g7g6 b1c3 d7d5 c4d5", "Grunfeld Defense: Exchange");
            book.put("d2d4 g8f6 c2c4 g7g6 b1c3 d7d5 g1f3", "Grunfeld Defense");
            book.put("d2d4 g8f6 c2c4 g7g6 b1c3 d7d5 c4d5 f6d5 e2e4", "Grunfeld Defense: Exchange, Classical");
            book.put("d2d4 g8f6 c2c4 g7g6 b1c3 d7d5 c4d5 f6d5 e2e4 d5c3", "Grunfeld Defense: Exchange, Main Line");
            book.put("d2d4 g8f6 c2c4 g7g6 b1c3 d7d5 c4d5 f6d5 e2e4 d5c3 b2c3 f8g7", "Grunfeld Defense: Exchange");
            book.put("d2d4 g8f6 c2c4 g7g6 b1c3 d7d5 g1f3 f8g7 d1b3", "Grunfeld Defense: Taimanov");
            // Nimzo-Indian
            book.put("d2d4 g8f6 c2c4 e7e6", "Indian Defense");
            book.put("d2d4 g8f6 c2c4 e7e6 b1c3", "Nimzo-Indian / Queen's Indian");
            book.put("d2d4 g8f6 c2c4 e7e6 b1c3 f8b4", "Nimzo-Indian Defense");
            book.put("d2d4 g8f6 c2c4 e7e6 b1c3 f8b4 e2e3", "Nimzo-Indian: Rubinstein");
            book.put("d2d4 g8f6 c2c4 e7e6 b1c3 f8b4 e2e3 e8g8", "Nimzo-Indian: Rubinstein");
            book.put("d2d4 g8f6 c2c4 e7e6 b1c3 f8b4 d1c2", "Nimzo-Indian: Classical");
            book.put("d2d4 g8f6 c2c4 e7e6 b1c3 f8b4 d1c2 d7d5", "Nimzo-Indian: Classical");
            book.put("d2d4 g8f6 c2c4 e7e6 b1c3 f8b4 a2a3", "Nimzo-Indian: Samisch");
            book.put("d2d4 g8f6 c2c4 e7e6 b1c3 f8b4 g1f3", "Nimzo-Indian: Three Knights");
            book.put("d2d4 g8f6 c2c4 e7e6 b1c3 f8b4 f2f3", "Nimzo-Indian: Leningrad");
            book.put("d2d4 g8f6 c2c4 e7e6 b1c3 f8b4 b2b4", "Nimzo-Indian: Samisch Gambit");
            // Queen's Indian
            book.put("d2d4 g8f6 c2c4 e7e6 g1f3", "Queen's Indian / Bogo-Indian");
            book.put("d2d4 g8f6 c2c4 e7e6 g1f3 b7b6", "Queen's Indian Defense");
            book.put("d2d4 g8f6 c2c4 e7e6 g1f3 b7b6 g2g3", "Queen's Indian: Fianchetto");
            book.put("d2d4 g8f6 c2c4 e7e6 g1f3 b7b6 g2g3 c8b7", "Queen's Indian: Fianchetto");
            book.put("d2d4 g8f6 c2c4 e7e6 g1f3 b7b6 b1c3", "Queen's Indian: Classical");
            book.put("d2d4 g8f6 c2c4 e7e6 g1f3 f8b4", "Bogo-Indian Defense");
            book.put("d2d4 g8f6 c2c4 e7e6 g1f3 d7d5", "Queen's Indian / QGD");
            // Dutch Defense
            book.put("d2d4 f7f5", "Dutch Defense");
            book.put("d2d4 f7f5 c2c4", "Dutch Defense");
            book.put("d2d4 f7f5 c2c4 g8f6", "Dutch Defense");
            book.put("d2d4 f7f5 c2c4 g8f6 g2g3", "Dutch: Leningrad");
            book.put("d2d4 f7f5 c2c4 g8f6 g2g3 g7g6", "Dutch: Leningrad");
            book.put("d2d4 f7f5 c2c4 e7e6", "Dutch: Classical");
            book.put("d2d4 f7f5 g1f3", "Dutch Defense");
            book.put("d2d4 f7f5 g2g3", "Dutch Defense");
            // Benoni
            book.put("d2d4 g8f6 c2c4 c7c5", "Benoni Defense");
            book.put("d2d4 g8f6 c2c4 c7c5 d4d5", "Benoni Defense");
            book.put("d2d4 g8f6 c2c4 c7c5 d4d5 e7e6", "Benoni Defense: Modern");
            book.put("d2d4 g8f6 c2c4 c7c5 d4d5 e7e6 b1c3", "Benoni Defense: Modern");
            book.put("d2d4 g8f6 c2c4 c7c5 d4d5 e7e6 b1c3 e6d5 c4d5 d7d6", "Modern Benoni");
            book.put("d2d4 g8f6 c2c4 c7c5 d4d5 e7e6 b1c3 e6d5 c4d5 d7d6 e2e4", "Modern Benoni: Main Line");
            // Old Indian
            book.put("d2d4 g8f6 c2c4 d7d6", "Old Indian Defense");
            book.put("d2d4 g8f6 c2c4 d7d6 b1c3 e7e5", "Old Indian Defense");

            // 1. c4  (English Opening)
            book.put("c2c4", "English Opening");
            book.put("c2c4 e7e5", "English: Reversed Sicilian");
            book.put("c2c4 e7e5 b1c3", "English: Reversed Sicilian");
            book.put("c2c4 e7e5 b1c3 g8f6", "English Opening");
            book.put("c2c4 e7e5 b1c3 b8c6", "English: Four Knights");
            book.put("c2c4 e7e5 b1c3 b8c6 g1f3", "English: Four Knights");
            book.put("c2c4 e7e5 g2g3", "English: King's English");
            book.put("c2c4 e7e5 g2g3 g8f6 f1g2", "English: King's English");
            book.put("c2c4 e7e5 g2g3 b8c6 f1g2", "English: King's English");
            book.put("c2c4 c7c5", "English: Symmetrical");
            book.put("c2c4 c7c5 g1f3", "English: Symmetrical");
            book.put("c2c4 c7c5 g1f3 g8f6", "English: Symmetrical");
            book.put("c2c4 c7c5 b1c3", "English: Symmetrical");
            book.put("c2c4 g8f6", "English Opening");
            book.put("c2c4 g8f6 b1c3", "English Opening");
            book.put("c2c4 g8f6 b1c3 e7e5", "English: King's English");
            book.put("c2c4 g8f6 b1c3 d7d5", "English: Agincourt Defense");
            book.put("c2c4 g8f6 b1c3 c7c5", "English: Symmetrical");
            book.put("c2c4 e7e6", "English: Agincourt Defense");
            book.put("c2c4 e7e6 b1c3", "English Opening");
            book.put("c2c4 e7e6 b1c3 d7d5", "English: Agincourt");
            book.put("c2c4 d7d5", "English: Anglo-Scandinavian");
            book.put("c2c4 d7d5 c4d5", "English: Anglo-Scandinavian");

            // 1. Nf3  (Reti Opening)
            book.put("g1f3", "Reti Opening");
            book.put("g1f3 d7d5", "Reti Opening");
            book.put("g1f3 d7d5 c2c4", "Reti Opening");
            book.put("g1f3 d7d5 g2g3", "King's Indian Attack");
            book.put("g1f3 d7d5 g2g3 g8f6", "King's Indian Attack");
            book.put("g1f3 g8f6", "Reti Opening");
            book.put("g1f3 g8f6 c2c4", "Reti Opening");
            book.put("g1f3 g8f6 g2g3", "King's Indian Attack");
            book.put("g1f3 d7d5 c2c4 c7c6 b1c3", "Reti: Slav Formation");
            book.put("g1f3 d7d5 c2c4 e7e6", "Reti: English/Catalan");
            book.put("g1f3 c7c5", "Reti Opening");
            book.put("g1f3 e7e6", "Reti Opening");

            // Other first moves
            book.put("g2g3", "King's Fianchetto Opening");
            book.put("b2b3", "Nimzowitsch-Larsen Attack");
            book.put("b2b3 e7e5", "Nimzowitsch-Larsen Attack");
            book.put("b2b3 d7d5", "Nimzowitsch-Larsen Attack");
            book.put("b2b4", "Polish Opening");
            book.put("b2b4 e7e5", "Polish Opening: King's Gambit Hybrid");
            book.put("f2f4", "Bird's Opening");
            book.put("f2f4 e7e5", "Bird's Opening: From's Gambit");
            book.put("f2f4 d7d5 g1f3 g8f6", "Bird's Opening");
            book.put("d2d4 e7e5", "Englund Gambit");
            book.put("d2d4 e7e5 d4e5", "Englund Gambit Accepted");

            book.forEach((seq, name) ->
                repo.save(new Opening(seq, name))
            );
        };
    }
}
