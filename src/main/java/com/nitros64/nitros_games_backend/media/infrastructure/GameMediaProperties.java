package com.nitros64.nitros_games_backend.media.infrastructure;

import java.nio.file.Path;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Validated
@ConfigurationProperties("app.storage.game-media")
public class GameMediaProperties {
    @NotNull
    private Path directory = Path.of("game-media");
    @Min(1)
    @jakarta.validation.constraints.Max(Integer.MAX_VALUE - 1)
    private long maxBytes = 10 * 1024 * 1024;
    @Min(1)
    private long maxPixels = 20_000_000;
}
