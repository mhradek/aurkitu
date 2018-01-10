/**
 *
 */
package com.michaelhradek.aurkitu.core;

import java.util.Set;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.michaelhradek.aurkitu.annotations.FlatBufferTable;
import com.michaelhradek.aurkitu.test.BogusAnnotation;
import com.michaelhradek.aurkitu.test.SampleClassReferenced;
import com.michaelhradek.aurkitu.test.SampleClassTable;
import com.michaelhradek.aurkitu.test.SampleEnumByte;
import com.michaelhradek.aurkitu.test.SampleEnumNull;

/**
 * @author m.hradek
 */
public class AnnotationParserTest {

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

    /**
     * Test method for
     * {@link com.michaelhradek.aurkitu.core.AnnotationParser#findAnnotatedClasses(java.lang.Class)}.
     */
    @Test
    public void testFindAnnotatedClasses() {
        Set<Class<?>> annotated = AnnotationParser.findAnnotatedClasses(FlatBufferTable.class);
        Assert.assertEquals(false, annotated.isEmpty());
        Assert.assertEquals(4, annotated.size());

        Assert.assertEquals(true, annotated.contains(SampleClassReferenced.class));
        Assert.assertEquals(true, annotated.contains(SampleClassTable.class));
        Assert.assertEquals(false, annotated.contains(SampleEnumByte.class));
        Assert.assertEquals(false, annotated.contains(SampleEnumNull.class));
    }

    @Test
    public void testFindAnnotatedClassesWithPath() {
        Set<Class<?>> annotated = AnnotationParser.findAnnotatedClasses(".*", BogusAnnotation.class);
        Assert.assertEquals(true, annotated.isEmpty());
    }
}
