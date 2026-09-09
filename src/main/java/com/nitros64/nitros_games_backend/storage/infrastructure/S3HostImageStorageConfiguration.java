package com.nitros64.nitros_games_backend.storage.infrastructure;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
        prefix = "app.storage.host-images",
        name = "backend",
        havingValue = "s3")
public class S3HostImageStorageConfiguration {

    @Bean
    S3Client hostImageS3Client(StorageProperties properties) {
        return S3Client.builder()
                .region(Region.of(properties.getS3().getRegion()))
                .build();
    }
}
