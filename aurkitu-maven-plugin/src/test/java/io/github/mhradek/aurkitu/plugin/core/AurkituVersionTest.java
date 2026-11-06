package io.github.mhradek.aurkitu.plugin.core;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class AurkituVersionTest {

    @Test
    void versionIsFilteredFromPom() {
        // Surefire runs against filtered target/classes, so the placeholder must be resolved.
        assertNotEquals("unknown", AurkituVersion.VERSION);
        assertTrue(AurkituVersion.VERSION.matches("\\d+\\.\\d+\\.\\d+.*"), AurkituVersion.VERSION);
        assertTrue(AurkituVersion.schemaHeader().contains("Aurkitu " + AurkituVersion.VERSION + " "));
    }

    @Test
    void missingOrUnfilteredResourceFallsBackToUnknown() {
        assertEquals("unknown", AurkituVersion.load(null));
        assertEquals("unknown", AurkituVersion.load(stream("version=${project.version}")));
        assertEquals("unknown", AurkituVersion.load(stream("other=1")));
        assertEquals("2.3.4", AurkituVersion.load(stream("version=2.3.4")));
    }

    private static InputStream stream(String s) {
        return new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8));
    }
}
