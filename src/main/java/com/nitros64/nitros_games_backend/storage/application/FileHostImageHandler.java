package com.nitros64.nitros_games_backend.storage.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileHostImageHandler {

    private static final Logger log = LoggerFactory.getLogger(FileHostImageHandler.class);

    private final FilesStorageService storage;

    public FileHostImageHandler(@Qualifier("FileHostImageStorage") FilesStorageService storage) {
        this.storage = storage;
    }

    public String store(MultipartFile file) {
        return storage.write(file);
    }

    public boolean delete(String filename) {
        return storage.delete(filename);
    }

    public void deleteOnRollback(String filename) {
        requireTransactionSynchronization();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != TransactionSynchronization.STATUS_COMMITTED) {
                    safelyDelete(filename, "rollback cleanup");
                }
            }
        });
    }

    public void deleteAfterCommit(String filename) {
        requireTransactionSynchronization();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                safelyDelete(filename, "post-commit cleanup");
            }
        });
    }

    private void requireTransactionSynchronization() {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalStateException("File cleanup must be registered inside a transaction");
        }
    }

    private void safelyDelete(String filename, String operation) {
        try {
            storage.delete(filename);
        } catch (RuntimeException exception) {
            log.error("Image {} failed for file {}", operation, filename, exception);
        }
    }
}
