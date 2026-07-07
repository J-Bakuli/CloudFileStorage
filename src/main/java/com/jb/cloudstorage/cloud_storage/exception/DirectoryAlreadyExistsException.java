package com.jb.cloudstorage.cloud_storage.exception;

public class DirectoryAlreadyExistsException extends RuntimeException {
    public DirectoryAlreadyExistsException(String message) {
        super(message);
    }

    public DirectoryAlreadyExistsException(String message, Throwable cause) {
        super(message, cause);
    }
}
