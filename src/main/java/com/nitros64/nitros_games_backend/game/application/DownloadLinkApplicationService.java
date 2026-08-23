package com.nitros64.nitros_games_backend.game.application;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nitros64.nitros_games_backend.game.domain.DownloadLink;
import com.nitros64.nitros_games_backend.game.domain.GameVersion;
import com.nitros64.nitros_games_backend.game.persistence.DownloadLinkRepository;
import com.nitros64.nitros_games_backend.game.persistence.GameVersionRepository;
import com.nitros64.nitros_games_backend.shared.application.ResourceNotFoundException;
import com.nitros64.nitros_games_backend.storage.application.ServerHostImageService;

@Service
public class DownloadLinkApplicationService {

    private final GameVersionRepository versions;
    private final DownloadLinkRepository downloadLinks;
    private final ServerHostImageService hostImages;

    public DownloadLinkApplicationService(
            GameVersionRepository versions,
            DownloadLinkRepository downloadLinks,
            ServerHostImageService hostImages) {
        this.versions = versions;
        this.downloadLinks = downloadLinks;
        this.hostImages = hostImages;
    }

    @Transactional(readOnly = true)
    public List<DownloadLinkDetails> findDownloadLinks(Long gameId, Long versionId) {
        var foundLinks = downloadLinks.findAllDetailedByHierarchy(versionId, gameId);
        if (foundLinks.isEmpty() && !versions.existsByIdAndGameId(versionId, gameId)) {
            throw new ResourceNotFoundException("Game version not found");
        }
        return foundLinks.stream()
                .map(this::toDetails)
                .toList();
    }

    @Transactional(readOnly = true)
    public DownloadLinkDetails findDownloadLink(Long gameId, Long versionId, Long linkId) {
        return toDetails(requireDownloadLink(gameId, versionId, linkId));
    }

    @Transactional
    public DownloadLinkDetails createDownloadLink(
            Long gameId,
            Long versionId,
            SaveDownloadLinkCommand command) {
        var link = new DownloadLink();
        link.attachToVersion(requireVersion(gameId, versionId));
        return toDetails(downloadLinks.saveAndFlush(apply(link, command)));
    }

    @Transactional
    public DownloadLinkDetails updateDownloadLink(
            Long gameId,
            Long versionId,
            Long linkId,
            SaveDownloadLinkCommand command) {
        var link = requireDownloadLink(gameId, versionId, linkId);
        return toDetails(downloadLinks.saveAndFlush(apply(link, command)));
    }

    @Transactional
    public void deleteDownloadLink(Long gameId, Long versionId, Long linkId) {
        downloadLinks.delete(requireDownloadLink(gameId, versionId, linkId));
        downloadLinks.flush();
    }

    private DownloadLink apply(DownloadLink link, SaveDownloadLinkCommand command) {
        link.updateDetails(command.link(), hostImages.findById(command.serverHostImageId()));
        return link;
    }

    private GameVersion requireVersion(Long gameId, Long versionId) {
        return versions.findOwnedByIdAndGameId(versionId, gameId)
                .orElseThrow(() -> new ResourceNotFoundException("Game version not found"));
    }

    private DownloadLink requireDownloadLink(Long gameId, Long versionId, Long linkId) {
        return downloadLinks.findDetailedByIdAndHierarchy(linkId, versionId, gameId)
                .orElseThrow(() -> new ResourceNotFoundException("Download link not found"));
    }

    private DownloadLinkDetails toDetails(DownloadLink link) {
        return new DownloadLinkDetails(
                link.getId(),
                link.getGameVersion().getId(),
                link.getLink(),
                link.getServerImage().getId());
    }
}
