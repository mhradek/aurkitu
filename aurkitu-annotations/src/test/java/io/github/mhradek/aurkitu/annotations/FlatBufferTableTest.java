package io.github.mhradek.aurkitu.annotations;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;

public class FlatBufferTableTest {

    @Test
    public void testFlatbufferTableDefault() {
        TestTableDefault testTable = new TestTableDefault();
        Annotation[] listAnnotations = testTable.getClass().getAnnotations();
        assertNotNull(listAnnotations);
        assertEquals(1, listAnnotations.length);

        Annotation classAnnotation = testTable.getClass().getAnnotation(FlatBufferTable.class);
        assertNotNull(classAnnotation);

        assertEquals(false, ((FlatBufferTable) classAnnotation).rootType());
        assertEquals(FlatBufferTable.TableStructureType.TABLE, ((FlatBufferTable) classAnnotation).value());
    }

    @Test
    public void testFlatbufferTableStructRoot() {
        TestTableStructRoot testTable = new TestTableStructRoot();
        Annotation[] listAnnotations = testTable.getClass().getAnnotations();
        assertNotNull(listAnnotations);
        assertEquals(2, listAnnotations.length);

        Annotation classAnnotation = testTable.getClass().getAnnotation(FlatBufferTable.class);
        assertNotNull(classAnnotation);

        assertEquals(true, ((FlatBufferTable) classAnnotation).rootType());
        assertEquals(FlatBufferTable.TableStructureType.STRUCT, ((FlatBufferTable) classAnnotation).value());
    }

    @FlatBufferTable
    private class TestTableDefault {

    }

    @FlatBufferTable(rootType = true, value = FlatBufferTable.TableStructureType.STRUCT)
    @Deprecated
    private class TestTableStructRoot {

    }
}