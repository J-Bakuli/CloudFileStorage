package com.jb.cloudstorage.cloud_storage.exception;

public class StorageException extends RuntimeException {
    public StorageException(String message, Exception e) {
        super(message, e);
    }
}