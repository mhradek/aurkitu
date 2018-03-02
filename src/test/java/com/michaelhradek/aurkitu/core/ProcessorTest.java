/**
 *
 */
package com.michaelhradek.aurkitu.core;

import java.lang.reflect.Field;

import org.apache.maven.plugin.MojoExecutionException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.michaelhradek.aurkitu.Config;
import com.michaelhradek.aurkitu.annotations.FlatBufferEnum;
import com.michaelhradek.aurkitu.annotations.FlatBufferTable;
import com.michaelhradek.aurkitu.core.output.EnumDeclaration;
import com.michaelhradek.aurkitu.core.output.FieldType;
import com.michaelhradek.aurkitu.core.output.Schema;
import com.michaelhradek.aurkitu.core.output.TypeDeclaration;
import com.michaelhradek.aurkitu.core.output.TypeDeclaration.Property;
import com.michaelhradek.aurkitu.test.SampleClassReferenced;
import com.michaelhradek.aurkitu.test.SampleClassStruct;
import com.michaelhradek.aurkitu.test.SampleClassTable;
import com.michaelhradek.aurkitu.test.SampleClassTableWithUndefined;
import com.michaelhradek.aurkitu.test.SampleEnumByte;
import com.michaelhradek.aurkitu.test.SampleEnumNull;

/**
 * @author m.hradek
 */
public class ProcessorTest {

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
     * Test method for {@link com.michaelhradek.aurkitu.core.Processor#buildSchema()}.
     */
    @Test
    public void testBuildSchema() throws MojoExecutionException {
        Processor processor = new Processor().withSourceAnnotation(FlatBufferTable.class)
                .withSourceAnnotation(FlatBufferEnum.class);
        Assert.assertEquals(2, processor.getSourceAnnotations().size());

        Schema schema = processor.buildSchema();
        schema.setNamespace(
                processor.getClass().getPackage().getName().toString().replace("core", "flatbuffers"));
        schema.addAttribute("Priority");
        schema.addAttribute("ConsiderThis");
        schema.addInclude("AnotherFile.fbs");

        Assert.assertEquals(6, processor.getTargetClasses().size());
        Assert.assertEquals(6, schema.getTypes().size());
        Assert.assertEquals(3, schema.getEnums().size());

        Assert.assertEquals("SampleClassTable", schema.getRootType());

        // TODO Test multiple root types
        if (Config.DEBUG) {
            System.out.println(schema.toString());
        }
    }

    /**
     * Test method for
     * {@link com.michaelhradek.aurkitu.core.Processor#buildEnumDeclaration(java.lang.Class)}.
     */
    @Test
    public void testBuildEnumDeclaration() throws MojoExecutionException {
        Processor processor = new Processor().withSourceAnnotation(FlatBufferEnum.class);
        Assert.assertEquals(1, processor.getSourceAnnotations().size());
        Schema schema = processor.buildSchema();

        Assert.assertEquals(2, processor.getTargetClasses().size());
        Assert.assertEquals(0, schema.getTypes().size());
        Assert.assertEquals(2, schema.getEnums().size());

        Assert.assertEquals(null, schema.getRootType());

        for (EnumDeclaration enumD : schema.getEnums()) {
            if (enumD.getName().equals(SampleEnumByte.class.getSimpleName())) {
                Assert.assertEquals(FieldType.BYTE, enumD.getType());

                if (Config.DEBUG) {
                    System.out.println(enumD.toString());
                }

                continue;
            }

            if (enumD.getName().equals(SampleEnumNull.class.getSimpleName())) {
                Assert.assertEquals(null, enumD.getType());

                if (Config.DEBUG) {
                    System.out.println(enumD.toString());
                }

                continue;
            }
        }
    }

    /**
     * Test method for
     * {@link com.michaelhradek.aurkitu.core.Processor#buildTypeDeclaration(java.lang.Class)}.
     */
    @Test
    public void testBuildTypeDeclaration() throws MojoExecutionException {
        Processor processor = new Processor().withSourceAnnotation(FlatBufferTable.class);
        Assert.assertEquals(1, processor.getSourceAnnotations().size());
        Schema schema = processor.buildSchema();

        Assert.assertEquals(4, processor.getTargetClasses().size());
        Assert.assertEquals(6, schema.getTypes().size());
        Assert.assertEquals(1, schema.getEnums().size());

        Assert.assertEquals("SampleClassTable", schema.getRootType());

        for (TypeDeclaration type : schema.getTypes()) {
            if (type.getName().equals(SampleClassTable.class.getSimpleName())) {
                Assert.assertEquals(9, type.properties.size());

                if (Config.DEBUG) {
                    System.out.println(type.toString());
                }

                continue;
            }

            if (type.getName().equals(SampleClassReferenced.class.getSimpleName())) {
                Assert.assertEquals(3, type.properties.size());
                // TODO More tests here

                if (Config.DEBUG) {
                    System.out.println(type.toString());
                }

                continue;
            }

            if (type.getName().equals(SampleClassStruct.class.getSimpleName())) {
                Assert.assertEquals(3, type.properties.size());
                // TODO More tests here

                if (Config.DEBUG) {
                    System.out.println(type.toString());
                }

                continue;
            }

            if (type.getName().equals(SampleClassReferenced.InnerClassStatic.class.getSimpleName())) {
                Assert.assertEquals(1, type.properties.size());
                // TODO More tests here

                if (Config.DEBUG) {
                    System.out.println(type.toString());
                }

                continue;
            }

            if (type.getName().equals(SampleClassReferenced.InnerClass.class.getSimpleName())) {
                Assert.assertEquals(2, type.properties.size());
                // TODO More tests here

                if (Config.DEBUG) {
                    System.out.println(type.toString());
                }

                continue;
            }

            if (type.getName().equals(SampleClassTableWithUndefined.class.getSimpleName())) {
                Assert.assertEquals(3, type.properties.size());
                // TODO More tests here

                if (Config.DEBUG) {
                    System.out.println(type.toString());
                }

                continue;
            }

            Assert.fail("Unaccounted class: " + type.getName());
        }
    }

    /**
     * Test method for
     * {@link com.michaelhradek.aurkitu.core.Processor#getPropertyForField(java.lang.reflect.Field)}.
     *
     * @throws SecurityException
     * @throws NoSuchFieldException
     */
    @Test
    public void testGetPropertyForField() throws NoSuchFieldException, SecurityException {
        Processor processor = new Processor();

        Field field = SampleClassTable.class.getDeclaredField("id");
        Property prop = processor.getPropertyForField(field);
        Assert.assertEquals("id", prop.name);
        Assert.assertEquals(FieldType.LONG, prop.type);
        Assert.assertEquals(true, prop.options.isEmpty());

        field = SampleClassTable.class.getDeclaredField("name");
        prop = processor.getPropertyForField(field);
        Assert.assertEquals("name", prop.name);
        Assert.assertEquals(FieldType.STRING, prop.type);
        Assert.assertEquals(true, prop.options.isEmpty());

        field = SampleClassTable.class.getDeclaredField("level");
        prop = processor.getPropertyForField(field);
        Assert.assertEquals("level", prop.name);
        Assert.assertEquals(FieldType.SHORT, prop.type);
        Assert.assertEquals(true, prop.options.isEmpty());

        field = SampleClassTable.class.getDeclaredField("currency");
        prop = processor.getPropertyForField(field);
        Assert.assertEquals("currency", prop.name);
        Assert.assertEquals(FieldType.INT, prop.type);
        Assert.assertEquals(true, prop.options.isEmpty());

        field = SampleClassTable.class.getDeclaredField("tokens");
        prop = processor.getPropertyForField(field);
        Assert.assertEquals("tokens", prop.name);
        Assert.assertEquals(FieldType.ARRAY, prop.type);
        Assert.assertEquals(false, prop.options.isEmpty());
        Assert.assertEquals(true, prop.options.containsKey(FieldType.ARRAY.toString()));
        Assert.assertEquals("string", prop.options.get(FieldType.ARRAY.toString()));

        field = SampleClassTable.class.getDeclaredField("deleted");
        prop = processor.getPropertyForField(field);
        Assert.assertEquals("deleted", prop.name);
        Assert.assertEquals(FieldType.BOOL, prop.type);

        // FIXME This should be false; boolean has a defualt value assigned to it
        Assert.assertEquals(true, prop.options.isEmpty());

        field = SampleClassTable.class.getDeclaredField("energy");
        prop = processor.getPropertyForField(field);
        Assert.assertEquals("energy", prop.name);
        Assert.assertEquals(FieldType.BYTE, prop.type);
        Assert.assertEquals(true, prop.options.isEmpty());
    }

}
