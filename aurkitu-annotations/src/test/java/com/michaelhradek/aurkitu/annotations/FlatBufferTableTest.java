package com.michaelhradek.aurkitu.annotations;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.lang.annotation.Annotation;

public class FlatBufferTableTest {

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
    }


    @Test
    public void testFlatbufferTableDefault() {
        TestTableDefault testTable = new TestTableDefault();
        Annotation[] listAnnotations = testTable.getClass().getAnnotations();
        Assert.assertNotNull(listAnnotations);
        Assert.assertEquals(1, listAnnotations.length);

        Annotation classAnnotation = testTable.getClass().getAnnotation(FlatBufferTable.class);
        Assert.assertNotNull(classAnnotation);

        Assert.assertEquals(false, ((FlatBufferTable) classAnnotation).rootType());
        Assert.assertEquals(FlatBufferTable.TableStructureType.TABLE, ((FlatBufferTable) classAnnotation).value());
    }

    @Test
    public void testFlatbufferTableStructRoot() {
        TestTableStructRoot testTable = new TestTableStructRoot();
        Annotation[] listAnnotations = testTable.getClass().getAnnotations();
        Assert.assertNotNull(listAnnotations);
        Assert.assertEquals(2, listAnnotations.length);

        Annotation classAnnotation = testTable.getClass().getAnnotation(FlatBufferTable.class);
        Assert.assertNotNull(classAnnotation);

        Assert.assertEquals(true, ((FlatBufferTable) classAnnotation).rootType());
        Assert.assertEquals(FlatBufferTable.TableStructureType.STRUCT, ((FlatBufferTable) classAnnotation).value());
    }

    @FlatBufferTable
    private class TestTableDefault {

    }

    @FlatBufferTable(rootType = true, value = FlatBufferTable.TableStructureType.STRUCT)
    @Deprecated
    private class TestTableStructRoot {

    }
}