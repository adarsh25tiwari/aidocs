package com.aidocs.exception;

public class DocumentProcessingException extends RuntimeException{

    public DocumentProcessingException(String message) {
        super(message);
    }
    public DocumentProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
    public DocumentProcessingException() {
        super("Document processing failed!");
    }
}
