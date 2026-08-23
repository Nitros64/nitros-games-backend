package com.nitros64.nitros_games_backend.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import com.nitros64.nitros_games_backend.game.domain.DownloadLink;
import com.nitros64.nitros_games_backend.storage.api.FileExceptionHandler;
import com.nitros64.nitros_games_backend.storage.api.ServerHostImageController;
import com.nitros64.nitros_games_backend.storage.api.dto.ServerHostImageNameRequest;
import com.nitros64.nitros_games_backend.storage.api.dto.ServerHostImageResponse;
import com.nitros64.nitros_games_backend.storage.api.dto.ServerHostImageUploadRequest;
import com.nitros64.nitros_games_backend.storage.api.mapper.ServerHostImageApiMapper;
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
                ServerHostImageController.class, FileExceptionHandler.class,
                ServerHostImageNameRequest.class, ServerHostImageUploadRequest.class,
                ServerHostImageResponse.class, ServerHostImageApiMapper.class);

        assertThat(storageTypes)
                .allSatisfy(type -> assertThat(type.getPackageName())
                        .startsWith(STORAGE_PACKAGE + "."));
    }

    @Test
    void storageControllerDoesNotExposeJpaEntities() {
        Stream.of(ServerHostImageController.class.getDeclaredMethods())
                .map(method -> method.toGenericString())
                .forEach(signature -> assertThat(signature)
                        .doesNotContain(STORAGE_PACKAGE + ".domain."));
    }

    @Test
    void downloadLinksReferenceTheStorageDomainType() throws NoSuchFieldException {
        assertThat(DownloadLink.class.getDeclaredField("serverImage").getType())
                .isEqualTo(ServerHostImage.class);
    }

    @Test
    void storageServiceUsesAnExplicitPersistenceContract() {
        assertThat(ServerHostImageService.class.getSuperclass()).isEqualTo(Object.class);
    }
}
