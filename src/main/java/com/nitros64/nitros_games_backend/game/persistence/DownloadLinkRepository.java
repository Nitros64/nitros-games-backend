package com.nitros64.nitros_games_backend.game.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.nitros64.nitros_games_backend.game.domain.DownloadLink;

public interface DownloadLinkRepository extends JpaRepository<DownloadLink, Long> {

    @Query("""
            select link
            from DownloadLink link
            join fetch link.gameVersion version
            join fetch link.serverImage
            where version.id = :gameVersionId
              and version.game.id = :gameId
            order by link.id
            """)
    List<DownloadLink> findAllDetailedByHierarchy(
            @Param("gameVersionId") Long gameVersionId,
            @Param("gameId") Long gameId);

    @Query("""
            select link
            from DownloadLink link
            join fetch link.gameVersion version
            join fetch link.serverImage
            where link.id = :id
              and version.id = :gameVersionId
              and version.game.id = :gameId
            """)
    Optional<DownloadLink> findDetailedByIdAndHierarchy(
            @Param("id") Long id,
            @Param("gameVersionId") Long gameVersionId,
            @Param("gameId") Long gameId);
}
