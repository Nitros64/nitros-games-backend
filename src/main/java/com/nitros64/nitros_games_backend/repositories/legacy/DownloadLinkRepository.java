package com.nitros64.nitros_games_backend.repositories.legacy;

import org.springframework.stereotype.Repository;

import com.nitros64.nitros_games_backend.model.entity.DownloadLink;
import com.nitros64.nitros_games_backend.shared.persistence.BaseRepository;

@Repository
public interface DownloadLinkRepository extends BaseRepository<DownloadLink, Long> {
    
}
