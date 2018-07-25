package com.michaelhradek.aurkitu.core;

import com.michaelhradek.aurkitu.Application;
import com.michaelhradek.aurkitu.annotations.FlatBufferEnum;
import com.michaelhradek.aurkitu.annotations.FlatBufferTable;
import com.michaelhradek.aurkitu.core.output.Schema;
import com.michaelhradek.aurkitu.test.SampleClassTable;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.plugin.testing.MojoRule;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.net.URLClassLoader;

public class UtilitiesTest extends AbstractMojoTestCase {

    private static String OUTPUT_DIRECTORY = "target/aurkito/utilities/test";

    /**
     * @see junit.framework.TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {

        // required for mojo lookups to work
        super.setUp();
    }

    @Rule
    public MojoRule rule = new MojoRule() {

        @Override
        protected void before() throws Throwable {
        }

        @Override
        protected void after() {
        }
    };

    @Test
    public void testIsLowerCaseType() {

        Double primDouble = 123D;
        Assert.assertTrue(Utilities.isLowerCaseType(primDouble.getClass()));

        Float primFloat = 123F;
        Assert.assertTrue(Utilities.isLowerCaseType(primFloat.getClass()));

        Long primLong = 123L;
        Assert.assertTrue(Utilities.isLowerCaseType(primLong.getClass()));

        Integer primInteger = 123;
        Assert.assertTrue(Utilities.isLowerCaseType(primInteger.getClass()));

        Short primShort = 12;
        Assert.assertTrue(Utilities.isLowerCaseType(primShort.getClass()));

        Character primCharacter = 'd';
        Assert.assertTrue(Utilities.isLowerCaseType(primCharacter.getClass()));

        Byte primByte = 8;
        Assert.assertTrue(Utilities.isLowerCaseType(primByte.getClass()));

        Boolean primBoolean = true;
        Assert.assertTrue(Utilities.isLowerCaseType(primBoolean.getClass()));

        String primString = "Test string";
        Assert.assertTrue(Utilities.isLowerCaseType(primString.getClass()));

        Schema schema = new Schema();
        Assert.assertFalse(Utilities.isLowerCaseType(schema.getClass()));

        Assert.assertFalse(Utilities.isLowerCaseType(Application.class));
    }

    @Test
    public void testExecuteActionOnSpecifiedClassLoader() {
        Class<?> result = Utilities.executeActionOnSpecifiedClassLoader(URLClassLoader.getSystemClassLoader(),
            new Utilities.ExecutableAction<Class<?>>() {

                public Class<?> run() {
                    try {
                        return Class.forName(SampleClassTable.class.getName());
                    } catch (ClassNotFoundException e) {
                        return null;
                    }
                }
            });

        Assert.assertNotNull(result);
        Assert.assertEquals(SampleClassTable.class, result);
    }

    @Test
    public void testBuildProjectClasspathList() throws Exception {

    }

    @Test
    public void testBuildReflections() {

    }

    @Test
    public void testIsSchemaPresent() throws Exception {

        // File will not exist
        Assert.assertFalse(Utilities.isSchemaPresent(new Schema(), new File("/")));

        Processor processor = new Processor().withSourceAnnotation(FlatBufferTable.class)
                .withSourceAnnotation(FlatBufferEnum.class);

        Schema schema = processor.buildSchema();
        FileGeneration gen = new FileGeneration(new File(OUTPUT_DIRECTORY));
        gen.writeSchema(schema);

        // No name set so even with caching it will return false because the filename keeps changing
        Assert.assertFalse(Utilities.isSchemaPresent(schema, gen.getOutputDirectory()));

        schema.setName("some-test-name");
        gen.writeSchema(schema);

        // File name is consistent and thus we can check cached file
        Assert.assertTrue(Utilities.isSchemaPresent(schema, gen.getOutputDirectory()));
    }
}

