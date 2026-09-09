package com.nitros64.nitros_games_backend.storage.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.nitros64.nitros_games_backend.storage.application.HostImageStorage;

import software.amazon.awssdk.services.s3.S3Client;

class StorageBackendSelectionTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(StorageTestConfiguration.class)
            .withPropertyValues(
                    "app.storage.host-images.directory=target/test-storage/backend-selection");

    @Test
    void filesystemIsTheDefaultAndOnlyStorageAdapter() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(HostImageStorage.class);
            assertThat(context.getBean(HostImageStorage.class))
                    .isInstanceOf(FileHostImageStorage.class);
            assertThat(context).doesNotHaveBean(S3HostImageStorage.class);
        });
    }

    @Test
    void s3SelectionActivatesOnlyTheS3Adapter() {
        contextRunner
                .withBean(S3Client.class, () -> mock(S3Client.class))
                .withPropertyValues(
                        "app.storage.host-images.backend=s3",
                        "app.storage.host-images.s3.bucket=nitros-games-production-images",
                        "app.storage.host-images.s3.region=eu-west-1",
                        "app.storage.host-images.s3.prefix=host-images/")
                .run(context -> {
                    assertThat(context).hasSingleBean(HostImageStorage.class);
                    assertThat(context.getBean(HostImageStorage.class))
                            .isInstanceOf(S3HostImageStorage.class);
                    assertThat(context).doesNotHaveBean(FileHostImageStorage.class);
                });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(StorageProperties.class)
    @Import({
        FileHostImageStorage.class,
        S3HostImageStorage.class,
        HostImageUploadValidator.class
    })
    static class StorageTestConfiguration {
    }
}
