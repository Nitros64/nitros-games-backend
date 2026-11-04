package com.nitros64.nitros_games_backend.media.application;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nitros64.nitros_games_backend.game.domain.GameData;
import com.nitros64.nitros_games_backend.game.domain.PublicationStatus;
import com.nitros64.nitros_games_backend.game.persistence.GameDataRepository;
import com.nitros64.nitros_games_backend.media.domain.GameMedia;
import com.nitros64.nitros_games_backend.media.domain.GameMediaType;
import com.nitros64.nitros_games_backend.media.persistence.GameMediaRepository;
import com.nitros64.nitros_games_backend.shared.application.ResourceNotFoundException;
import com.nitros64.nitros_games_backend.storage.application.UploadImageException;

@Service
public class GameMediaService {
    private final GameDataRepository games;
    private final GameMediaRepository media;
    private final GameImageProcessor processor;
    private final GameMediaFiles files;
    private final GameMediaStorage storage;

    public GameMediaService(GameDataRepository games, GameMediaRepository media,
            GameImageProcessor processor, GameMediaFiles files, GameMediaStorage storage) {
        this.games = games;
        this.media = media;
        this.processor = processor;
        this.files = files;
        this.storage = storage;
    }

    @Transactional(readOnly = true)
    public List<GameMediaDetails> list(Long gameId, boolean administrative) {
        requireVisibleGame(gameId, administrative);
        return media.findAllByGameIdOrderByTypeAscPositionAsc(gameId).stream().map(GameMediaDetails::from).toList();
    }

    @Transactional(readOnly = true)
    public Content read(Long gameId, Long mediaId, boolean administrative) {
        requireVisibleGame(gameId, administrative);
        GameMedia image = requireMedia(gameId, mediaId);
        return new Content(storage.load(image.getStorageKey()), image.getContentType());
    }

    @Transactional
    public GameMediaDetails upload(Long gameId, GameMediaType type, boolean replace,
            String filename, String contentType, byte[] content, String altText, String caption) {
        GameData game = lockGame(gameId);
        GameMedia image;
        if (type == GameMediaType.COVER) {
            var current = media.findByGameIdAndType(gameId, type);
            if (replace) image = current.orElseThrow(() -> new ResourceNotFoundException("Cover not found"));
            else {
                if (current.isPresent()) throw new DataIntegrityViolationException("Game already has a cover");
                image = new GameMedia(game, type, 0);
            }
        } else {
            var screenshots = screenshots(gameId);
            image = new GameMedia(game, type, screenshots.size() + 1);
        }
        var validated = processor.process(filename, contentType, content);
        String oldKey = image.getStorageKey();
        String key = files.store(validated);
        image.replaceImage(key, validated.contentType(), validated.content().length, validated.width(), validated.height());
        image.updateMetadata(altText, caption);
        media.saveAndFlush(image);
        if (oldKey != null) files.deleteAfterCommit(oldKey);
        return GameMediaDetails.from(image);
    }

    @Transactional
    public GameMediaDetails updateMetadata(Long gameId, Long mediaId, String altText, String caption) {
        lockGame(gameId);
        GameMedia image = requireMedia(gameId, mediaId);
        image.updateMetadata(altText, caption);
        return GameMediaDetails.from(image);
    }

    @Transactional
    public List<GameMediaDetails> reorder(Long gameId, List<Long> ids) {
        lockGame(gameId);
        List<GameMedia> screenshots = screenshots(gameId);
        Map<Long, GameMedia> byId = screenshots.stream().collect(Collectors.toMap(GameMedia::getId, Function.identity()));
        if (ids.size() != byId.size() || !new HashSet<>(ids).equals(byId.keySet())) {
            throw new UploadImageException("Order must contain every screenshot ID exactly once", null, HttpStatus.BAD_REQUEST);
        }
        assignPositions(screenshots, ids.stream().map(byId::get).toList());
        return ids.stream().map(byId::get).map(GameMediaDetails::from).toList();
    }

    @Transactional
    public void deleteCover(Long gameId) {
        lockGame(gameId);
        deleteImage(media.findByGameIdAndType(gameId, GameMediaType.COVER)
                .orElseThrow(() -> new ResourceNotFoundException("Cover not found")));
    }

    @Transactional
    public void deleteScreenshot(Long gameId, Long id) {
        lockGame(gameId);
        GameMedia image = requireMedia(gameId, id);
        if (image.getType() != GameMediaType.SCREENSHOT) throw new ResourceNotFoundException("Screenshot not found");
        deleteImage(image);
        List<GameMedia> remaining = screenshots(gameId);
        assignPositions(remaining, remaining);
    }

    /** Called in the game's deletion transaction, before its row and cascading children are removed. */
    @Transactional
    public void deleteAllForGame(Long gameId) {
        lockGame(gameId);
        var images = media.findAllByGameIdOrderByTypeAscPositionAsc(gameId);
        images.forEach(image -> files.deleteAfterCommit(image.getStorageKey()));
        media.deleteAll(images);
        media.flush();
    }

    private void deleteImage(GameMedia image) {
        media.delete(image);
        media.flush();
        files.deleteAfterCommit(image.getStorageKey());
    }

    private List<GameMedia> screenshots(Long gameId) {
        return media.findAllByGameIdAndTypeOrderByPositionAsc(gameId, GameMediaType.SCREENSHOT);
    }

    private void assignPositions(List<GameMedia> current, List<GameMedia> ordered) {
        // Vacate all occupied positions before a swap, so the unique constraint holds at every flush.
        int offset = current.stream().mapToInt(GameMedia::getPosition).max().orElse(0);
        for (int i = 0; i < current.size(); i++) current.get(i).moveTo(offset + i + 1);
        media.flush();
        for (int i = 0; i < ordered.size(); i++) ordered.get(i).moveTo(i + 1);
        media.flush();
    }

    private GameData lockGame(Long gameId) {
        return games.findLockedById(gameId).orElseThrow(() -> new ResourceNotFoundException("Game not found"));
    }

    private void requireVisibleGame(Long gameId, boolean administrative) {
        GameData game = games.findById(gameId).orElseThrow(() -> new ResourceNotFoundException("Game not found"));
        if (!administrative && game.getPublicationStatus() != PublicationStatus.PUBLISHED) {
            throw new ResourceNotFoundException("Game not found");
        }
    }

    private GameMedia requireMedia(Long gameId, Long id) {
        return media.findByIdAndGameId(id, gameId).orElseThrow(() -> new ResourceNotFoundException("Game media not found"));
    }

    public record Content(byte[] bytes, String contentType) {}
}
