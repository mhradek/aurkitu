package io.github.mhradek.aurkitu.plugin.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SchemaGenerationExceptionTest {

    @Test
    void messageOnlyConstructor() {
        SchemaGenerationException ex = new SchemaGenerationException("boom");
        assertEquals("boom", ex.getMessage());
        assertNull(ex.getCause());
    }

    @Test
    void messageAndCauseConstructor() {
        Throwable cause = new IllegalStateException("root");
        SchemaGenerationException ex = new SchemaGenerationException("wrap", cause);
        assertEquals("wrap", ex.getMessage());
        assertSame(cause, ex.getCause());
    }
}
