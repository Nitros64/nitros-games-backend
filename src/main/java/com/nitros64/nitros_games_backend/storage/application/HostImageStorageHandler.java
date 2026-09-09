package com.nitros64.nitros_games_backend.storage.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

@Service
public class HostImageStorageHandler {

    private static final Logger log = LoggerFactory.getLogger(HostImageStorageHandler.class);

    private final HostImageStorage storage;

    public HostImageStorageHandler(HostImageStorage storage) {
        this.storage = storage;
    }

    public String store(MultipartFile file) {
        return storage.store(file);
    }

    public boolean delete(String storageKey) {
        return storage.delete(storageKey);
    }

    public void deleteOnRollback(String storageKey) {
        requireTransactionSynchronization();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != TransactionSynchronization.STATUS_COMMITTED) {
                    safelyDelete(storageKey, "rollback cleanup");
                }
            }
        });
    }

    public void deleteAfterCommit(String storageKey) {
        requireTransactionSynchronization();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                safelyDelete(storageKey, "post-commit cleanup");
            }
        });
    }

    private void requireTransactionSynchronization() {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalStateException("Image cleanup must be registered inside a transaction");
        }
    }

    private void safelyDelete(String storageKey, String operation) {
        try {
            storage.delete(storageKey);
        } catch (RuntimeException exception) {
            log.error("Image {} failed for storage key {}", operation, storageKey, exception);
        }
    }
}
