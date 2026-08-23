package com.nitros64.nitros_games_backend.game.persistence;

import org.springframework.stereotype.Repository;

import com.nitros64.nitros_games_backend.game.domain.DownloadLink;
import com.nitros64.nitros_games_backend.shared.persistence.BaseRepository;

@Repository
public interface DownloadLinkRepository extends BaseRepository<DownloadLink, Long> {
    java.util.List<DownloadLink> findAllByGameVersionIdOrderById(Long gameVersionId);
}
