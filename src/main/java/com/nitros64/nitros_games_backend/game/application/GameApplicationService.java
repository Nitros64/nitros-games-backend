package com.nitros64.nitros_games_backend.game.application;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nitros64.nitros_games_backend.catalog.application.GameGenreService;
import com.nitros64.nitros_games_backend.catalog.domain.GameGenre;
import com.nitros64.nitros_games_backend.game.domain.GameData;
import com.nitros64.nitros_games_backend.game.persistence.GameDataRepository;
import com.nitros64.nitros_games_backend.shared.application.ResourceNotFoundException;

@Service
public class GameApplicationService {

    private final GameDataRepository games;
    private final GameGenreService genres;

    public GameApplicationService(
            GameDataRepository games,
            GameGenreService genres) {
        this.games = games;
        this.genres = genres;
    }

    @Transactional(readOnly = true)
    public List<GameDetails> findAllGames() {
        return games.findAllDetailed().stream().map(this::toDetails).toList();
    }

    @Transactional(readOnly = true)
    public Page<GameDetails> findAllGames(Pageable pageable) {
        return hydrate(games.findAllIds(pageable), pageable);
    }

    @Transactional(readOnly = true)
    public Page<GameDetails> searchGames(GameSearchCriteria criteria, Pageable pageable) {
        return hydrate(games.searchIds(
                criteria.name(),
                criteria.genreId(),
                criteria.jam(),
                pageable), pageable);
    }

    @Transactional(readOnly = true)
    public GameDetails findGame(Long gameId) {
        return toDetails(games.findDetailedById(gameId)
                .orElseThrow(() -> new ResourceNotFoundException("Game not found")));
    }

    @Transactional
    public GameDetails createGame(SaveGameCommand command) {
        return toDetails(games.saveAndFlush(apply(new GameData(), command)));
    }

    @Transactional
    public GameDetails updateGame(Long gameId, SaveGameCommand command) {
        return toDetails(games.saveAndFlush(apply(requireGame(gameId), command)));
    }

    @Transactional
    public void deleteGame(Long gameId) {
        games.delete(requireGame(gameId));
        games.flush();
    }

    private GameData apply(GameData game, SaveGameCommand command) {
        var resolvedGenres = new LinkedHashSet<GameGenre>(genres.findAllById(command.genreIds()));
        game.updateDetails(
                command.name(),
                command.description(),
                command.jam(),
                command.developerCount(),
                resolvedGenres);
        return game;
    }

    private GameData requireGame(Long gameId) {
        return games.findById(gameId)
                .orElseThrow(() -> new ResourceNotFoundException("Game not found"));
    }

    private Page<GameDetails> hydrate(Page<Long> gameIds, Pageable pageable) {
        if (gameIds.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, gameIds.getTotalElements());
        }
        var gamesById = games.findDetailedByIdIn(gameIds.getContent()).stream()
                .collect(Collectors.toMap(GameData::getId, Function.identity()));
        var details = gameIds.getContent().stream()
                .map(gamesById::get)
                .map(this::toDetails)
                .toList();
        return new PageImpl<>(details, pageable, gameIds.getTotalElements());
    }

    private GameDetails toDetails(GameData game) {
        return new GameDetails(
                game.getId(),
                game.getName(),
                game.getDescription(),
                game.isJam(),
                game.getDeveloperCount(),
                game.getGenres().stream()
                        .map(GameGenre::getId)
                        .collect(Collectors.toCollection(LinkedHashSet::new)));
    }
}
