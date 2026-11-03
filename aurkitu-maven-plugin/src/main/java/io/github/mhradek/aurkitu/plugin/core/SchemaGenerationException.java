package io.github.mhradek.aurkitu.plugin.core;

/**
 * Thrown when schema generation fails due to validation issues or I/O errors.
 */
public class SchemaGenerationException extends RuntimeException {
    public SchemaGenerationException(String message) {
        super(message);
    }

    public SchemaGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
