package com.nitros64.nitros_games_backend.media.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class GameMediaFiles {
    private static final Logger log = LoggerFactory.getLogger(GameMediaFiles.class);
    private final GameMediaStorage storage;

    public GameMediaFiles(GameMediaStorage storage) {
        this.storage = storage;
    }

    public String store(GameImageProcessor.ProcessedImage image) {
        requireTransaction();
        String key = storage.store(image.content(), image.extension());
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) safelyDelete(key);
            }
        });
        return key;
    }

    public void deleteAfterCommit(String key) {
        requireTransaction();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                safelyDelete(key);
            }
        });
    }

    private void requireTransaction() {
        if (!TransactionSynchronizationManager.isActualTransactionActive()
                || !TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalStateException("Media file changes require an active transaction");
        }
    }

    private void safelyDelete(String key) {
        try {
            storage.delete(key);
        } catch (RuntimeException exception) {
            log.error("Game media cleanup failed for key {}; retry deletion after checking database references", key, exception);
        }
    }
}
