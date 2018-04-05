package com.michaelhradek.aurkitu.core;

import com.michaelhradek.aurkitu.Application;
import com.michaelhradek.aurkitu.Config;
import com.michaelhradek.aurkitu.annotations.FlatBufferEnum;
import com.michaelhradek.aurkitu.annotations.FlatBufferTable;
import com.michaelhradek.aurkitu.core.output.EnumDeclaration;
import com.michaelhradek.aurkitu.core.output.FieldType;
import com.michaelhradek.aurkitu.core.output.Schema;
import com.michaelhradek.aurkitu.core.output.TypeDeclaration;
import com.michaelhradek.aurkitu.core.output.TypeDeclaration.Property;
import com.michaelhradek.aurkitu.core.output.TypeDeclaration.Property.PropertyOptionKey;
import com.michaelhradek.aurkitu.stubs.AurkituTestMavenProjectStub;
import com.michaelhradek.aurkitu.stubs.AurkituTestSettingsStub;
import com.michaelhradek.aurkitu.test.SampleClassReferenced;
import com.michaelhradek.aurkitu.test.SampleClassReferenced.SampleClassTableInnerEnumInt;
import com.michaelhradek.aurkitu.test.SampleClassStruct;
import com.michaelhradek.aurkitu.test.SampleClassTable;
import com.michaelhradek.aurkitu.test.SampleClassTableWithUndefined;
import com.michaelhradek.aurkitu.test.SampleEnumByte;
import com.michaelhradek.aurkitu.test.SampleEnumNull;
import com.michaelhradek.aurkitu.test.other.SampleAnonymousEnum;
import com.michaelhradek.aurkitu.test.other.SampleClassNamespaceMap;
import java.io.File;
import java.lang.reflect.Field;
import java.net.URLClassLoader;
import java.util.HashMap;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.plugin.testing.MojoRule;
import org.junit.After;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author m.hradek
 */
public class ProcessorTest extends AbstractMojoTestCase {

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

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
        super.tearDown();
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
            processor.getClass().getPackage().getName().replace("core", "flatbuffers"));
        schema.addAttribute("Priority");
        schema.addAttribute("ConsiderThis");
        schema.addInclude("AnotherFile.fbs");

        Assert.assertEquals(13, processor.getTargetClasses().size());
        Assert.assertEquals(8, schema.getTypes().size());
        Assert.assertEquals(9, schema.getEnums().size());

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

        Assert.assertEquals(7, processor.getTargetClasses().size());
        Assert.assertEquals(0, schema.getTypes().size());
        Assert.assertEquals(7, schema.getEnums().size());

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

        Assert.assertEquals(6, processor.getTargetClasses().size());
        Assert.assertEquals(8, schema.getTypes().size());
        Assert.assertEquals(7, schema.getEnums().size());

        Assert.assertEquals("SampleClassTable", schema.getRootType());

        for (TypeDeclaration type : schema.getTypes()) {

            if (type.getName().equals(SampleClassTable.class.getSimpleName())) {
                Assert.assertEquals(14, type.properties.size());

                for (Property property : type.getProperties()) {
                    if (property.name.equals("innerEnum")) {
                        Assert.assertEquals("SHORT_SWORD", property.options.get(PropertyOptionKey.DEFAULT_VALUE));
                    }

                    if (property.name.equals("definedInnerEnumArray")) {
                        Assert.assertEquals(SampleClassTableInnerEnumInt.class.getName(),
                            property.options.get(PropertyOptionKey.ARRAY));
                    }

                    if (property.name.equals("fullnameClass")) {
                        Assert.assertEquals(SampleClassReferenced.class.getName(),
                            property.options.get(PropertyOptionKey.IDENT));
                    }
                }

                if (Config.DEBUG) {
                    System.out.println(type.toString());
                }

                continue;
            }

            if (type.getName().equals(SampleClassReferenced.class.getSimpleName())) {
                Assert.assertEquals(4, type.properties.size());
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

            if (type.getName().equals(SampleClassNamespaceMap.class.getSimpleName())) {
                Assert.assertEquals(1, type.properties.size());
                // TODO More tests here

                if (Config.DEBUG) {
                    System.out.println(type.toString());
                }

                continue;
            }

            if (type.getName().equals(SampleAnonymousEnum.class.getSimpleName())) {
                Assert.assertEquals(6, type.properties.size());
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
        Assert.assertEquals(true, prop.options.containsKey(Property.PropertyOptionKey.ARRAY));
        Assert.assertEquals("string", prop.options.get(Property.PropertyOptionKey.ARRAY));

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

    @Test
    public void testParseFieldSignatureForParametrizedTypeString() throws NoSuchFieldException, IllegalAccessException, ClassNotFoundException {
        Schema schema = new Schema();
        Field field = schema.getClass().getDeclaredField("enums");

        Processor processor = new Processor();
        Assert.assertEquals(EnumDeclaration.class.getName(), processor.parseFieldSignatureForParametrizedTypeString(field));

        Class<?> testClass = Class.forName(SampleClassTable.class.getName());
        field = testClass.getDeclaredField("tokens");
        Assert.assertEquals(String.class.getName(), processor.parseFieldSignatureForParametrizedTypeString(field));

        testClass = Class.forName(SampleClassReferenced.class.getName());
        field = testClass.getDeclaredField("baggage");
        Assert.assertEquals(SampleClassTable.class.getName(), processor.parseFieldSignatureForParametrizedTypeString(field));
    }

    @Test
    public void testExecuteActionOnSpecifiedClassLoader() {
        Class<?> result;

        result = Processor.executeActionOnSpecifiedClassLoader(URLClassLoader.getSystemClassLoader(), new Processor.ExecutableAction<Class<?>>() {

            public Class<?> run() {
                try {
                    return Class.forName(SampleClassTable.class.getName());
                } catch (ClassNotFoundException e) {
                    return null;
                }
            }
        });
    }

    @Test
    public void testProcessNamespaceOverride() throws MojoExecutionException, NoSuchFieldException {
        Processor processor = new Processor()
            .withSourceAnnotation(FlatBufferTable.class)
            .withNamespaceOverrideMap(new HashMap<String, String>() {
                {
                    put("com.michaelhradek.aurkitu.test.other", "com.michaelhradek.aurkitu.test.flatbuffer");
                }
            });

        Schema schema = processor.buildSchema();
        for (TypeDeclaration type : schema.getTypes()) {
            if (type.getName().equals(SampleClassReferenced.class.getSimpleName())) {

                Field field = SampleClassReferenced.class.getDeclaredField("samples");
                Property prop = processor.getPropertyForField(field);

                Assert.assertEquals(
                    "com.michaelhradek.aurkitu.test.flatbuffer." + SampleClassNamespaceMap.class.getSimpleName(),
                    prop.options.get(PropertyOptionKey.ARRAY)
                );
            }
        }
    }

    @Test
    public void testGetClassForClassName() throws Exception {
        File testPom = new File(getBasedir(), "src/test/resources/plugin-basic-with-project/pom.xml");

        Application mojo = new Application();
        mojo = (Application) this.configureMojo(
            mojo, extractPluginConfiguration(Application.MOJO_NAME, testPom)
        );

        Field projectField = mojo.getClass().getDeclaredField("project");
        projectField.setAccessible(true);
        AurkituTestMavenProjectStub mavenProject = (AurkituTestMavenProjectStub) projectField.get(mojo);

        Assert.assertNotNull(mavenProject);

        Class<?> clazz = Processor.getClassForClassName(mavenProject, AurkituTestSettingsStub.class.getName());
        Assert.assertNotNull(clazz);
        Assert.assertEquals(AurkituTestSettingsStub.class.getName(), clazz.getName());
    }
}
