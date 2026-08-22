package com.nitros64.nitros_games_backend.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.nitros64.nitros_games_backend.game.domain.DownloadLink;
import com.nitros64.nitros_games_backend.storage.api.FileExceptionHandler;
import com.nitros64.nitros_games_backend.storage.api.ServerHostImageController;
import com.nitros64.nitros_games_backend.storage.application.FileHostImageHandler;
import com.nitros64.nitros_games_backend.storage.application.FilesStorageService;
import com.nitros64.nitros_games_backend.storage.application.ServerHostImageService;
import com.nitros64.nitros_games_backend.storage.application.UploadImageException;
import com.nitros64.nitros_games_backend.storage.domain.ServerHostImage;
import com.nitros64.nitros_games_backend.storage.infrastructure.FileHostImageStorage;
import com.nitros64.nitros_games_backend.storage.infrastructure.StorageProperties;
import com.nitros64.nitros_games_backend.storage.persistence.ServerHostImageRepository;

class StorageModuleStructureTests {

    private static final String STORAGE_PACKAGE =
            "com.nitros64.nitros_games_backend.storage";

    @Test
    void storageComponentsStayInsideTheirVerticalModule() {
        List<Class<?>> storageTypes = List.of(
                ServerHostImage.class,
                ServerHostImageService.class, FileHostImageHandler.class,
                FilesStorageService.class, UploadImageException.class,
                ServerHostImageRepository.class, FileHostImageStorage.class,
                StorageProperties.class,
                ServerHostImageController.class, FileExceptionHandler.class);

        assertThat(storageTypes)
                .allSatisfy(type -> assertThat(type.getPackageName())
                        .startsWith(STORAGE_PACKAGE + "."));
    }

    @Test
    void downloadLinksReferenceTheStorageDomainType() throws NoSuchFieldException {
        assertThat(DownloadLink.class.getDeclaredField("serverImage").getType())
                .isEqualTo(ServerHostImage.class);
    }
}
