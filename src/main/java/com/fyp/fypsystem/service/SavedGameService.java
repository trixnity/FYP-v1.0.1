package com.fyp.fypsystem.service;

import com.fyp.fypsystem.model.SavedGame;
import com.fyp.fypsystem.model.User;
import com.fyp.fypsystem.repository.SavedGameRepository;
import com.fyp.fypsystem.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class SavedGameService {

    private static final DateTimeFormatter DEFAULT_NAME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final SavedGameRepository savedGameRepository;
    private final UserRepository userRepository;

    public SavedGameService(SavedGameRepository savedGameRepository, UserRepository userRepository) {
        this.savedGameRepository = savedGameRepository;
        this.userRepository = userRepository;
    }

    public List<SavedGame> myGames(String email) {
        User user = requireUser(email);
        Map<Long, SavedGame> gamesById = new LinkedHashMap<>();
        savedGameRepository.findByUserIdOrderByUpdatedAtDesc(user.getId())
                .forEach(game -> gamesById.put(game.getId(), game));
        savedGameRepository.findByOwnerEmailOrderByUpdatedAtDesc(user.getEmail())
                .forEach(game -> gamesById.putIfAbsent(game.getId(), game));
        return new ArrayList<>(gamesById.values());
    }

    public Optional<SavedGame> getOwned(Long id, String email) {
        User user = requireUser(email);
        return savedGameRepository.findByIdAndUserId(id, user.getId())
                .or(() -> savedGameRepository.findByIdAndOwnerEmail(id, user.getEmail()));
    }

    public SavedGame create(SavedGame payload, String email) {
        User user = requireUser(email);
        SavedGame game = new SavedGame();
        game.setUserId(user.getId());
        game.setStudentId(user.getId());
        game.setOwnerEmail(user.getEmail());
        copyEditableFields(payload, game, true);
        return savedGameRepository.save(game);
    }

    public Optional<SavedGame> update(Long id, SavedGame payload, String email) {
        User user = requireUser(email);
        return getOwned(id, email).map(existing -> {
            existing.setUserId(user.getId());
            existing.setStudentId(user.getId());
            existing.setOwnerEmail(user.getEmail());
            copyEditableFields(payload, existing, false);
            return savedGameRepository.save(existing);
        });
    }

    public boolean delete(Long id, String email) {
        User user = requireUser(email);
        return getOwned(id, email).map(existing -> {
            savedGameRepository.delete(existing);
            return true;
        }).orElse(false);
    }

    private User requireUser(String email) {
        if (email == null || email.isBlank()) {
            throw new AccessDeniedException("Unauthorized");
        }
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new AccessDeniedException("Unauthorized"));
    }

    private void copyEditableFields(SavedGame source, SavedGame target, boolean creating) {
        if (source == null) {
            source = new SavedGame();
        }

        String gameName = clean(source.getGameName());
        if (gameName == null) {
            gameName = "Untitled Game - " + LocalDateTime.now().format(DEFAULT_NAME_FORMAT);
        }

        target.setGameName(gameName);
        target.setWhitePlayer(clean(source.getWhitePlayer()));
        target.setBlackPlayer(clean(source.getBlackPlayer()));
        target.setResult(source.getResult());
        target.setPgn(source.getPgn());
        target.setFen(source.getFen() != null ? source.getFen() : source.getCurrentFen());
        target.setMovesJson(source.getMovesJson() != null ? source.getMovesJson() : source.getMoveHistoryJson());
        target.setMoveAnnotationsJson(source.getMoveAnnotationsJson());
        target.setNotes(source.getNotes());
        target.setSource(SavedGame.GameSource.ANALYSIS_BOARD);
        target.setEventName(clean(source.getEventName()));
        target.setRound(clean(source.getRound()));
        target.setSite(clean(source.getSite()));
        target.setGameDate(source.getGameDate());
        target.setOpeningName(clean(source.getOpeningName()));
        target.setAnalysisEnabled(Boolean.FALSE.equals(source.getAnalysisEnabled()) ? Boolean.FALSE : Boolean.TRUE);
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
