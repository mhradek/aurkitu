package com.michaelhradek.aurkitu.plugin.core;

import com.michaelhradek.aurkitu.annotations.FlatBufferComment;
import com.michaelhradek.aurkitu.annotations.FlatBufferEnum;
import com.michaelhradek.aurkitu.annotations.FlatBufferEnumTypeField;
import com.michaelhradek.aurkitu.annotations.FlatBufferTable;
import com.michaelhradek.aurkitu.annotations.types.EnumType;
import com.michaelhradek.aurkitu.annotations.types.FieldType;
import com.michaelhradek.aurkitu.plugin.Application;
import com.michaelhradek.aurkitu.plugin.Config;
import com.michaelhradek.aurkitu.plugin.core.output.EnumDeclaration;
import com.michaelhradek.aurkitu.plugin.core.output.Schema;
import com.michaelhradek.aurkitu.plugin.core.output.TypeDeclaration;
import com.michaelhradek.aurkitu.plugin.core.output.TypeDeclaration.Property;
import com.michaelhradek.aurkitu.plugin.core.output.TypeDeclaration.Property.PropertyOptionKey;
import com.michaelhradek.aurkitu.plugin.core.parsing.ArtifactReference;
import com.michaelhradek.aurkitu.plugin.stubs.AurkituTestMavenProjectStub;
import com.michaelhradek.aurkitu.plugin.stubs.AurkituTestSettingsStub;
import com.michaelhradek.aurkitu.plugin.test.*;
import com.michaelhradek.aurkitu.plugin.test.SampleClassReferenced.SampleClassTableInnerEnumInt;
import com.michaelhradek.aurkitu.plugin.test.other.SampleAnonymousEnum;
import com.michaelhradek.aurkitu.plugin.test.other.SampleClassNamespaceMap;
import javassist.*;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.ClassFile;
import javassist.bytecode.ConstPool;
import javassist.bytecode.annotation.Annotation;
import javassist.bytecode.annotation.BooleanMemberValue;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.project.MavenProject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * @author m.hradek
 */
public class ProcessorTest extends AbstractMojoTestCase {

    private static final String TEST_NAMESPACE_OVERRIDE_KEY = "com.test.company.key";
    private static final String TEST_NAMESPACE_OVERRIDE_VALUE = "com.test.company.value";
    private static final String TEST_SPECIFIED_DEPENDENCY = TEST_NAMESPACE_OVERRIDE_KEY;
    private static final Map<String, String> TEST_NAMESPACE_OVERRIDE_MAP = new HashMap<String, String>() {
        {
            put("com.michaelhradek.aurkitu.plugin.test.other", "com.michaelhradek.aurkitu.plugin.test" +
                    ".flatbuffer");
        }
    };

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    private static Class<?> createTestClassRootType(String name) throws CannotCompileException {
        CtClass targetClass = ClassPool.getDefault().makeClass(name);
        ClassFile ccFile = targetClass.getClassFile();
        ConstPool constpool = ccFile.getConstPool();

        AnnotationsAttribute attribute = new AnnotationsAttribute(constpool, AnnotationsAttribute.visibleTag);
        Annotation annotation = new Annotation(FlatBufferTable.class.getName(), constpool);
        annotation.addMemberValue("rootType", new BooleanMemberValue(true, constpool));
        attribute.addAnnotation(annotation);

        ccFile.addAttribute(attribute);

        return targetClass.toClass();
    }


    @Test
    public void testProcessClass() throws NoSuchFieldException, IllegalAccessException {
        MavenProject mockMavenProject = Mockito.mock(MavenProject.class);
        ArtifactReference reference = new ArtifactReference(mockMavenProject, null, null, null, null);

        Field tableField = SampleClassTable.class.getDeclaredField("fullnameClass");

        Schema schema = new Schema();
        schema.setClasspathReferenceList(new ArrayList<>());

        Processor processor = new Processor().withArtifactReference(reference).withConsolidatedSchemas(false).withSchema(schema);

        Field currentSchemaField = processor.getClass().getDeclaredField("currentSchema");
        currentSchemaField.setAccessible(true);
        currentSchemaField.set(processor, schema);

        Property property = processor.processClass(new Property(), tableField, false);
    }

    /**
     * TestEnumMultipleType
     *
     * @param name
     * @param setEnumType
     * @return
     * @throws CannotCompileException
     */
    private static Class<?> createTestEnum(String name, boolean setEnumType, boolean setMultipleEnumTypeFields) throws CannotCompileException {
        CtClass enumMultipleType = ClassPool.getDefault().makeClass(name);

        ClassFile ccFile = enumMultipleType.getClassFile();
        ConstPool constpool = ccFile.getConstPool();

        AnnotationsAttribute attrField = new AnnotationsAttribute(constpool, AnnotationsAttribute.visibleTag);
        Annotation annotField = new Annotation(FlatBufferEnumTypeField.class.getName(), constpool);
        attrField.addAnnotation(annotField);

        CtField field = new CtField(CtClass.intType, "key", enumMultipleType);
        field.getFieldInfo().addAttribute(attrField);
        enumMultipleType.addField(field);

        field = new CtField(CtClass.booleanType, "value", enumMultipleType);

        // Unnecessary. Leaving in for now.
        if (setMultipleEnumTypeFields) {
            field.getFieldInfo().addAttribute(attrField);
        }

        enumMultipleType.addField(field);

        CtMethod method = CtNewMethod.make("public java.lang.Object[] getEnumConstants() { return new java.lang.Object[0]; }", enumMultipleType);
        method.setModifiers(enumMultipleType.getModifiers() & -Modifier.PUBLIC);
        enumMultipleType.addMethod(method);

        if (setEnumType) {
            AnnotationsAttribute attrClass = new AnnotationsAttribute(constpool, AnnotationsAttribute.visibleTag);
            Annotation annotClass = new Annotation(FlatBufferEnum.class.getName(), constpool);
            attrClass.addAnnotation(annotClass);
            ccFile.addAttribute(attrClass);
        }

        return enumMultipleType.toClass();
    }

    /**
     * @see junit.framework.TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {

        // required for mojo lookups to work
        super.setUp();
    }

    /**
     * @throws java.lang.Exception via super
     */
    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test method for {@link com.michaelhradek.aurkitu.plugin.core.Processor#execute()}.
     */
    @Test
    public void testExecute() throws MojoExecutionException {
        Processor processor = new Processor().withSourceAnnotation(FlatBufferTable.class)
                .withSourceAnnotation(FlatBufferEnum.class).withSchema(new Schema()).withValidateSchemas(true);
        Assert.assertEquals(2, processor.getSourceAnnotations().size());

        processor.execute();
        Schema schema = processor.getProcessedSchemas().get(0);
        schema.setNamespace(
                processor.getClass().getPackage().getName().replace("core", "flatbuffers"));
        schema.addAttribute("Priority");
        schema.addAttribute("ConsiderThis");
        schema.addInclude("AnotherFile.fbs");

        Assert.assertEquals(12, processor.getTargetClasses().size());
        Assert.assertEquals(9, schema.getTypes().size());
        Assert.assertEquals(8, schema.getEnums().size());

        Assert.assertEquals("SampleClassTable", schema.getRootType());

        if (Config.DEBUG) {
            System.out.println(schema.toString());
        }
    }

    @Test
    public void testBuildEnumDeclarationPass() throws MojoExecutionException {
        Processor processor = new Processor().withSourceAnnotation(FlatBufferEnum.class).withSchema(new Schema());
        Assert.assertEquals(1, processor.getSourceAnnotations().size());

        processor.execute();
        Schema schema = processor.getProcessedSchemas().get(0);

        Assert.assertEquals(6, processor.getTargetClasses().size());
        Assert.assertEquals(0, schema.getTypes().size());
        Assert.assertEquals(6, schema.getEnums().size());

        Assert.assertEquals(null, schema.getRootType());

        for (EnumDeclaration enumD : schema.getEnums()) {
            if (enumD.getName().equals(SampleEnumByte.class.getSimpleName())) {
                Assert.assertEquals(EnumType.BYTE, enumD.getType());
                Assert.assertNull(enumD.getComment());

                if (Config.DEBUG) {
                    System.out.println(enumD.toString());
                }

                continue;
            }

            // Undefined defaults to EnumType.BYTE
            if (enumD.getName().equals(SampleEnumNull.class.getSimpleName())) {
                Assert.assertEquals(EnumType.BYTE, enumD.getType());
                Assert.assertNull(enumD.getComment());

                if (Config.DEBUG) {
                    System.out.println(enumD.toString());
                }

                continue;
            }

            if (enumD.getName().equals(SampleAnonymousEnum.class.getSimpleName())) {
                Assert.assertEquals(EnumType.SHORT, enumD.getType());
                Assert.assertNotNull(enumD.getComment());

                if (Config.DEBUG) {
                    System.out.println(enumD.toString());
                }
            }
        }
    }

    @Test
    public void testBuildTypeDeclarationPass() throws MojoExecutionException {
        Processor processor = new Processor().withSourceAnnotation(FlatBufferTable.class).withSchema(new Schema());
        Assert.assertEquals(1, processor.getSourceAnnotations().size());

        processor.execute();
        Schema schema = processor.getProcessedSchemas().get(0);

        Assert.assertEquals(6, processor.getTargetClasses().size());
        Assert.assertEquals(9, schema.getTypes().size());
        Assert.assertEquals(5, schema.getEnums().size());

        Assert.assertEquals("SampleClassTable", schema.getRootType());

        for (TypeDeclaration type : schema.getTypes()) {

            if (type.getName().equals(SampleClassTable.class.getSimpleName())) {
                Assert.assertEquals(16, type.getProperties().size());

                Assert.assertNotNull(type.getComment());

                for (Property property : type.getProperties()) {
                    if ("innerEnum".equals(property.name)) {
                        Assert.assertEquals("SHORT_SWORD",
                                property.options.get(PropertyOptionKey.DEFAULT_VALUE));
                        Assert.assertNull(property.options.get(PropertyOptionKey.COMMENT));
                    }

                    if ("definedInnerEnumArray".equals(property.name)) {
                        Assert.assertEquals(SampleClassTableInnerEnumInt.class.getName(),
                                property.options.get(PropertyOptionKey.ARRAY));
                        Assert.assertNull(property.options.get(PropertyOptionKey.COMMENT));
                    }

                    if ("fullnameClass".equals(property.name)) {
                        Assert.assertEquals(SampleClassReferenced.class.getName(),
                                property.options.get(PropertyOptionKey.IDENT));
                        Assert.assertNull(property.options.get(PropertyOptionKey.COMMENT));
                    }

                    if ("level".equals(property.name)) {
                        Assert.assertNotNull(property.options.get(PropertyOptionKey.COMMENT));
                    }

                    if ("dataMap".equals(property.name)) {
                        Assert.assertEquals(FieldType.MAP, property.type);
                        Assert.assertTrue(property.options.containsKey(PropertyOptionKey.MAP));
                        Assert.assertEquals(TypeDeclaration.MapValueSet.class.getSimpleName() + "_" +
                                SampleClassTable.class.getSimpleName() + "_" + property.name, property.options.get(PropertyOptionKey.MAP));
                    }
                }

                if (Config.DEBUG) {
                    System.out.println(type.toString());
                }

                continue;
            }

            if (type.getName().equals(SampleClassReferenced.class.getSimpleName())) {
                Assert.assertEquals(4, type.getProperties().size());
                // TODO More tests here

                if (Config.DEBUG) {
                    System.out.println(type.toString());
                }

                continue;
            }

            if (type.getName().equals(SampleClassStruct.class.getSimpleName())) {
                Assert.assertEquals(3, type.getProperties().size());
                // TODO More tests here

                if (Config.DEBUG) {
                    System.out.println(type.toString());
                }

                continue;
            }

            if (type.getName().equals(SampleClassReferenced.InnerClassStatic.class.getSimpleName())) {
                Assert.assertEquals(1, type.getProperties().size());
                // TODO More tests here

                if (Config.DEBUG) {
                    System.out.println(type.toString());
                }

                continue;
            }

            if (type.getName().equals(SampleClassReferenced.InnerClass.class.getSimpleName())) {
                Assert.assertEquals(2, type.getProperties().size());
                // TODO More tests here

                if (Config.DEBUG) {
                    System.out.println(type.toString());
                }

                continue;
            }

            if (type.getName().equals(SampleClassTableWithUndefined.class.getSimpleName())) {
                Assert.assertEquals(3, type.getProperties().size());
                // TODO More tests here

                if (Config.DEBUG) {
                    System.out.println(type.toString());
                }

                continue;
            }

            if (type.getName().equals(SampleClassNamespaceMap.class.getSimpleName())) {
                Assert.assertEquals(1, type.getProperties().size());
                // TODO More tests here

                if (Config.DEBUG) {
                    System.out.println(type.toString());
                }

                continue;
            }

            if (type.getName().equals(SampleAnonymousEnum.class.getSimpleName())) {
                Assert.assertEquals(4, type.getProperties().size());
                // TODO More tests here

                if (Config.DEBUG) {
                    System.out.println(type.toString());
                }

                continue;
            }

            if (type.getName().startsWith(TypeDeclaration.MapValueSet.class.getSimpleName() + "_")) {
                Assert.assertEquals(2, type.getProperties().size());
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
     * {@link com.michaelhradek.aurkitu.plugin.core.Processor#getPropertyForField(com.michaelhradek.aurkitu.plugin.core.output.Schema, java.lang.reflect.Field)}.
     *
     * @throws SecurityException    if unable to access field via getDeclaredField()
     * @throws NoSuchFieldException if unable to locate field via getDeclaredField()
     */
    @Test
    public void testGetPropertyForField() throws NoSuchFieldException, SecurityException {
        Processor processor = new Processor();
        Schema schema = new Schema();

        // Test "Long"
        Field field = SampleClassTable.class.getDeclaredField("id");
        Property prop = processor.getPropertyForField(schema, field);
        Assert.assertEquals("id", prop.name);
        Assert.assertEquals(FieldType.LONG, prop.type);
        Assert.assertEquals(true, prop.options.isEmpty());

        // Test "String"
        field = SampleClassTable.class.getDeclaredField("name");
        prop = processor.getPropertyForField(schema, field);
        Assert.assertEquals("name", prop.name);
        Assert.assertEquals(FieldType.STRING, prop.type);
        Assert.assertEquals(true, prop.options.isEmpty());

        // Test "short"
        field = SampleClassTable.class.getDeclaredField("level");
        prop = processor.getPropertyForField(schema, field);
        Assert.assertEquals("level", prop.name);
        Assert.assertEquals(FieldType.SHORT, prop.type);
        Assert.assertEquals(false, prop.options.isEmpty());

        // Test "int"
        field = SampleClassTable.class.getDeclaredField("currency");
        prop = processor.getPropertyForField(schema, field);
        Assert.assertEquals("currency", prop.name);
        Assert.assertEquals(FieldType.INT, prop.type);
        Assert.assertEquals(true, prop.options.isEmpty());

        // Test "List<T>" array
        field = SampleClassTable.class.getDeclaredField("tokens");
        prop = processor.getPropertyForField(schema, field);
        Assert.assertEquals("tokens", prop.name);
        Assert.assertEquals(FieldType.ARRAY, prop.type);
        Assert.assertEquals(false, prop.options.isEmpty());
        Assert.assertEquals(true, prop.options.containsKey(Property.PropertyOptionKey.ARRAY));
        Assert.assertEquals("string", prop.options.get(Property.PropertyOptionKey.ARRAY));

        // Test "int[]" array
        field = SampleClassTable.class.getDeclaredField("options");
        prop = processor.getPropertyForField(schema, field);
        Assert.assertEquals("options", prop.name);
        Assert.assertEquals(FieldType.ARRAY, prop.type);
        Assert.assertEquals(false, prop.options.isEmpty());
        Assert.assertEquals(true, prop.options.containsKey(Property.PropertyOptionKey.ARRAY));
        Assert.assertEquals("int", prop.options.get(Property.PropertyOptionKey.ARRAY));

        // Test "T[]" array
        field = SampleClassTable.class.getDeclaredField("anomalousSamples");
        prop = processor.getPropertyForField(schema, field);
        Assert.assertEquals("anomalousSamples", prop.name);
        Assert.assertEquals(FieldType.ARRAY, prop.type);
        Assert.assertEquals(false, prop.options.isEmpty());
        Assert.assertEquals(true, prop.options.containsKey(Property.PropertyOptionKey.ARRAY));
        Assert.assertEquals("SimpleUndefinedClass", prop.options.get(Property.PropertyOptionKey.ARRAY));

        // Test "Set<E>"
        field = SampleClassTable.class.getDeclaredField("regionLocations");
        prop = processor.getPropertyForField(schema, field);
        Assert.assertEquals("regionLocations", prop.name);
        Assert.assertEquals(FieldType.ARRAY, prop.type);
        Assert.assertEquals(false, prop.options.isEmpty());
        Assert.assertEquals(true, prop.options.containsKey(Property.PropertyOptionKey.ARRAY));

        // FIXME This needs to be corrected - how do we want to serialize things in Set
        Assert.assertEquals("URL", prop.options.get(Property.PropertyOptionKey.ARRAY));

        // Test "long"
        field = SampleClassTable.class.getDeclaredField("createTime");
        prop = processor.getPropertyForField(schema, field);
        Assert.assertEquals("createTime", prop.name);
        Assert.assertEquals(FieldType.LONG, prop.type);
        Assert.assertEquals(true, prop.options.isEmpty());

        // Test "Double"
        field = SampleClassTable.class.getDeclaredField("weight");
        prop = processor.getPropertyForField(schema, field);
        Assert.assertEquals("weight", prop.name);
        Assert.assertEquals(FieldType.DOUBLE, prop.type);
        Assert.assertEquals(true, prop.options.isEmpty());

        // Test "Map<?, ?>"
        field = SampleClassTable.class.getDeclaredField("dataMap");
        prop = processor.getPropertyForField(schema, field);
        Assert.assertEquals("dataMap", prop.name);
        Assert.assertEquals(FieldType.MAP, prop.type);
        Assert.assertEquals(false, prop.options.isEmpty());
        Assert.assertEquals(true, prop.options.containsKey(Property.PropertyOptionKey.MAP));

        // Test "boolean"
        field = SampleClassTable.class.getDeclaredField("deleted");
        prop = processor.getPropertyForField(schema, field);
        Assert.assertEquals("deleted", prop.name);
        Assert.assertEquals(FieldType.BOOL, prop.type);

        // FIXME This should be false; boolean has a default value assigned to it
        Assert.assertEquals(true, prop.options.isEmpty());

        // Test ""byte"
        field = SampleClassTable.class.getDeclaredField("energy");
        prop = processor.getPropertyForField(schema, field);
        Assert.assertEquals("energy", prop.name);
        Assert.assertEquals(FieldType.BYTE, prop.type);
        Assert.assertEquals(true, prop.options.isEmpty());
    }

    @Test
    public void testParseFieldSignatureForParametrizedTypeStringoOnList()
            throws NoSuchFieldException, IllegalAccessException, ClassNotFoundException {
        Schema schema = new Schema();
        Field field = schema.getClass().getDeclaredField("enums");

        Assert.assertEquals(EnumDeclaration.class.getName(),
                Processor.parseFieldSignatureForParametrizedTypeStringOnList(field));

        Class<?> testClass = Class.forName(SampleClassTable.class.getName());
        field = testClass.getDeclaredField("tokens");
        Assert
                .assertEquals(String.class.getName(), Processor.parseFieldSignatureForParametrizedTypeStringOnList(field));

        testClass = Class.forName(SampleClassReferenced.class.getName());
        field = testClass.getDeclaredField("baggage");
        Assert.assertEquals(SampleClassTable.class.getName(),
                Processor.parseFieldSignatureForParametrizedTypeStringOnList(field));
    }

    @Test
    public void testParseFieldSignatureForParametrizedTypeStringsOnMap()
            throws NoSuchFieldException, IllegalAccessException, ClassNotFoundException {

        Class<?> testClass = Class.forName(SampleClassTable.class.getName());
        Field field = testClass.getDeclaredField("dataMap");

        String[] expected = new String[2];
        expected[0] = "java.lang.String";
        expected[1] = "java.lang.Object";

        Assert.assertArrayEquals(expected, Processor.parseFieldSignatureForParametrizedTypeStringsOnMap(field));
    }

    @Test
    public void testProcessNamespaceOverride() throws MojoExecutionException, NoSuchFieldException {
        Processor processor = new Processor()
                .withSourceAnnotation(FlatBufferTable.class)
                .withNamespaceOverrideMap(TEST_NAMESPACE_OVERRIDE_MAP)
                .withSchema(new Schema());

        processor.execute();
        Schema schema = processor.getProcessedSchemas().get(0);
        for (TypeDeclaration type : schema.getTypes()) {
            if (type.getName().equals(SampleClassReferenced.class.getSimpleName())) {

                Field field = SampleClassReferenced.class.getDeclaredField("samples");
                Property prop = processor.getPropertyForField(schema, field);

                Assert.assertEquals(
                        "com.michaelhradek.aurkitu.plugin.test.flatbuffer." + SampleClassNamespaceMap.class.getSimpleName(),
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

        Class<?> clazz = Processor.getClassForClassName(mavenProject, new Schema(), AurkituTestSettingsStub.class.getName());
        Assert.assertNotNull(clazz);
        Assert.assertEquals(AurkituTestSettingsStub.class.getName(), clazz.getName());
    }

    @Test
    public void testWithNamespaceOverrideMap() {
        Processor processor = new Processor();
        Assert.assertNotNull(processor.getNamespaceOverrideMap());
        Assert.assertEquals(0, processor.getNamespaceOverrideMap().size());
        Assert.assertNotNull(processor.withNamespaceOverrideMap(null));

        Map<String, String> testMap = new HashMap<>();
        testMap.put(TEST_NAMESPACE_OVERRIDE_KEY, TEST_NAMESPACE_OVERRIDE_VALUE);
        processor.withNamespaceOverrideMap(testMap);
        Assert.assertEquals(1, processor.getNamespaceOverrideMap().size());

        // Updated with trailing period (.)
        Assert.assertNull(processor.getNamespaceOverrideMap().get(TEST_NAMESPACE_OVERRIDE_KEY));

        String testValue = processor.getNamespaceOverrideMap().get(TEST_NAMESPACE_OVERRIDE_KEY + ".");
        Assert.assertNotNull(testValue);
        Assert.assertEquals(TEST_NAMESPACE_OVERRIDE_VALUE + ".", testValue);

        testMap.put(TEST_NAMESPACE_OVERRIDE_KEY + ".", TEST_NAMESPACE_OVERRIDE_VALUE + ".");
        processor.withNamespaceOverrideMap(testMap);
        Assert.assertEquals(1, processor.getNamespaceOverrideMap().size());

        testValue = processor.getNamespaceOverrideMap().get(TEST_NAMESPACE_OVERRIDE_KEY + ".");
        Assert.assertNotNull(testValue);
        Assert.assertEquals(TEST_NAMESPACE_OVERRIDE_VALUE + ".", testValue);
    }

    @Test
    public void testWithSpecifiedDependencies() {
        Processor processor = new Processor();
        Assert.assertNull(processor.getSpecifiedDependencies());
        Assert.assertNotNull(processor.withSpecifiedDependencies(null));

        List<String> testList = new ArrayList<>();
        testList.add(TEST_SPECIFIED_DEPENDENCY);
        Assert.assertNotNull(processor.withSpecifiedDependencies(testList));
        Assert.assertEquals(1, processor.getSpecifiedDependencies().size());
        Assert.assertFalse(processor.getSpecifiedDependencies().isEmpty());
        Assert.assertEquals(TEST_SPECIFIED_DEPENDENCY, processor.getSpecifiedDependencies().get(0));
    }

    @Test
    public void testIsEnumWorkAround() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        Processor processor = new Processor();
        Method privateIsEnumWorkAround = Processor.class.getDeclaredMethod("isEnumWorkaround", Class.class);
        privateIsEnumWorkAround.setAccessible(true);

        Boolean result = (Boolean) privateIsEnumWorkAround.invoke(processor, SampleClassReferenced.class);
        Assert.assertFalse(result);

        result = (Boolean) privateIsEnumWorkAround.invoke(processor, SampleClassTableInnerEnumInt.class);
        Assert.assertTrue(result);

        result = (Boolean) privateIsEnumWorkAround.invoke(processor, TestAnonymousClass.class);
        Assert.assertFalse(result);

        result = (Boolean) privateIsEnumWorkAround.invoke(processor, TestInterface.class);
        Assert.assertFalse(result);
    }

    @Test
    public void testWithArtifactReference() {
        Processor processor = new Processor();
        Assert.assertNull(processor.getArtifactReference());

        Processor processorCopy = processor.withArtifactReference(new ArtifactReference(null, null, null, null, null));
        Assert.assertNotNull(processor.getArtifactReference());
        Assert.assertNotNull(processorCopy);
        Assert.assertNotNull(processorCopy.getArtifactReference());
    }

    @Test
    public void testWithConsolidatedSchemas() {
        Processor processor = new Processor();
        Assert.assertTrue(processor.isConsolidatedSchemas());

        Processor processorCopy = processor.withConsolidatedSchemas(null);
        Assert.assertTrue(processor.isConsolidatedSchemas());
        Assert.assertTrue(processorCopy.isConsolidatedSchemas());

        processorCopy = processor.withConsolidatedSchemas(false);
        Assert.assertFalse(processor.isConsolidatedSchemas());
        Assert.assertFalse(processorCopy.isConsolidatedSchemas());
    }

    @Test
    public void testBuildEnumDelarationComment() {
        Processor processor = new Processor();
        EnumDeclaration declaration = processor.buildEnumDeclaration(TestEnumCommentEmpty.class);
        Assert.assertNotNull(declaration);
        Assert.assertNull(declaration.getComment());
    }

    @Test
    public void testWithValidateSchemas() {
        Processor processor = new Processor();
        Assert.assertFalse(processor.isValidateSchemas());

        Processor processorCopy = processor.withValidateSchemas(null);
        Assert.assertFalse(processor.isValidateSchemas());
        Assert.assertFalse(processorCopy.isValidateSchemas());

        processorCopy = processor.withValidateSchemas(true);
        Assert.assertTrue(processor.isValidateSchemas());
        Assert.assertTrue(processorCopy.isValidateSchemas());
    }

    @Test
    public void testWithSchemas() {
        Processor processor = new Processor();
        Assert.assertTrue(processor.getCandidateSchemas().isEmpty());

        List<Schema> newSchemas = new ArrayList<>();
        Schema schemaOne = new Schema();
        schemaOne.setName("schemaOne");
        Schema schemaTwo = new Schema();
        schemaTwo.setName("schemaTwo");
        newSchemas.add(schemaOne);
        newSchemas.add(schemaTwo);

        Assert.assertFalse(processor.withSchemas(newSchemas).getCandidateSchemas().isEmpty());
        Assert.assertEquals(2, processor.getCandidateSchemas().size());
        Assert.assertTrue(processor.getCandidateSchemas().contains(schemaOne));
        Assert.assertTrue(processor.getCandidateSchemas().contains(schemaTwo));

        newSchemas = new ArrayList<>();
        Schema schemaThree = new Schema();
        schemaThree.setName("schemaThree");
        newSchemas.add(schemaThree);

        Assert.assertFalse(processor.withSchemas(newSchemas).getCandidateSchemas().isEmpty());
        Assert.assertEquals(1, processor.getCandidateSchemas().size());
        Assert.assertEquals(schemaThree, processor.getCandidateSchemas().get(0));
    }

    @Test
    public void testWithSchema() {
        Processor processor = new Processor();
        Assert.assertTrue(processor.getCandidateSchemas().isEmpty());

        Schema schemaOne = new Schema();
        schemaOne.setName("schemaOne");
        Schema schemaTwo = new Schema();
        schemaTwo.setName("schemaTwo");

        processor.withSchema(schemaOne);
        Assert.assertFalse(processor.getCandidateSchemas().isEmpty());
        processor.withSchema(schemaTwo);
        Assert.assertFalse(processor.getCandidateSchemas().isEmpty());
        Assert.assertEquals(1, processor.getCandidateSchemas().size());
    }

    @Test
    public void testAddSchema() {
        Processor processor = new Processor();
        Assert.assertTrue(processor.getCandidateSchemas().isEmpty());

        Schema schemaOne = new Schema();
        schemaOne.setName("schemaOne");
        Schema schemaTwo = new Schema();
        schemaTwo.setName("schemaTwo");

        processor.addSchema(schemaOne);
        Assert.assertFalse(processor.getCandidateSchemas().isEmpty());
        Assert.assertEquals(1, processor.getCandidateSchemas().size());
        processor.addSchema(schemaTwo);
        Assert.assertFalse(processor.getCandidateSchemas().isEmpty());
        Assert.assertEquals(2, processor.getCandidateSchemas().size());
    }

    @Test
    public void testAddAllSchemas() {
        Processor processor = new Processor();
        Assert.assertTrue(processor.getCandidateSchemas().isEmpty());

        List<Schema> newSchemas = new ArrayList<>();
        Schema schemaOne = new Schema();
        schemaOne.setName("schemaOne");
        Schema schemaTwo = new Schema();
        schemaTwo.setName("schemaTwo");
        newSchemas.add(schemaOne);
        newSchemas.add(schemaTwo);

        Assert.assertFalse(processor.addAllSchemas(newSchemas).getCandidateSchemas().isEmpty());
        Assert.assertEquals(2, processor.getCandidateSchemas().size());
        Assert.assertTrue(processor.getCandidateSchemas().contains(schemaOne));
        Assert.assertTrue(processor.getCandidateSchemas().contains(schemaTwo));

        newSchemas = new ArrayList<>();
        Schema schemaThree = new Schema();
        schemaThree.setName("schemaThree");
        newSchemas.add(schemaThree);

        Assert.assertFalse(processor.addAllSchemas(newSchemas).getCandidateSchemas().isEmpty());
        Assert.assertEquals(3, processor.getCandidateSchemas().size());
        Assert.assertTrue(processor.getCandidateSchemas().contains(schemaThree));
    }

    @Test
    public void testGetExternalClassDefinitionDetails() throws MojoExecutionException, IllegalAccessException,
            NoSuchFieldException, NoSuchMethodException, InvocationTargetException {

        Processor processor = new Processor().withSourceAnnotation(FlatBufferTable.class)
                .withSourceAnnotation(FlatBufferEnum.class).withSchema(new Schema());
        Assert.assertEquals(2, processor.getSourceAnnotations().size());

        processor.execute();

        Method getExternalClassDefinitionDetailsMethod = processor.getClass().getDeclaredMethod("getExternalClassDefinitionDetails", Class.class);
        getExternalClassDefinitionDetailsMethod.setAccessible(true);


        Processor.ExternalClassDefinition externalClassDefinition =
                (Processor.ExternalClassDefinition) getExternalClassDefinitionDetailsMethod.invoke(processor, TestAnonymousClass.class);
        Assert.assertFalse(externalClassDefinition.locatedOutside);
        Assert.assertNull(externalClassDefinition.targetNamespace);

        Field currentSchemaField = processor.getClass().getDeclaredField("currentSchema");
        currentSchemaField.setAccessible(true);
        Schema currentSchema = (Schema) currentSchemaField.get(processor);

        //if(currentSchema == null) {
        //  currentSchema = new Schema();
        //}

        currentSchema.setDependency(true);
        currentSchemaField.set(processor, currentSchema);

        externalClassDefinition =
                (Processor.ExternalClassDefinition) getExternalClassDefinitionDetailsMethod.invoke(processor, TestAnonymousClass.class);
        Assert.assertFalse(externalClassDefinition.locatedOutside);
        Assert.assertNull(externalClassDefinition.targetNamespace);
    }

    @Test
    public void testCurrentSchema() throws NoSuchFieldException, MojoExecutionException {
        Processor processor = new Processor();
        Assert.assertNull(processor.getCurrentSchema());

        Schema schema = new Schema();
        schema.setName("testSchemaCurrent");
        processor.withSchema(schema);
        processor.execute();

        Assert.assertEquals(schema, processor.getCurrentSchema());
    }

    @Test
    public void testBuildEnumDelarationMultipleTypes() throws CannotCompileException {
        try {
            Processor processor = new Processor();
            processor.buildEnumDeclaration(createTestEnum("TestEnumMultipleType", true, true));
            Assert.fail("Expected IllegalArgumentException where number of annotations FlatBufferEnumTypeField > 1 not thrown");
        } catch (IllegalArgumentException e) {
            Assert.assertEquals("Can only declare one @FlatBufferEnumTypeField for Enum: TestEnumMultipleType", e.getMessage());
        }
    }

    @Test
    public void testBuildEnumDelarationMissingEnumType() throws CannotCompileException {
        try {
            Processor processor = new Processor();
            processor.buildEnumDeclaration(createTestEnum("TestEnumMissingType", false, true));
            Assert.fail("Expected IllegalArgumentException where missing @FlatBufferEnum(enumType = EnumType.<SELECT>) declaration");
        } catch (IllegalArgumentException e) {
            Assert.assertEquals("Missing @FlatBufferEnum(enumType = EnumType.<SELECT>) declaration or remove @FlatBufferEnumTypeField for: TestEnumMissingType", e.getMessage());
        }
    }

    @Test
    public void testMultipleRoots() throws NoSuchFieldException, NoSuchMethodException, IllegalAccessException, CannotCompileException, InvocationTargetException {

        try {
            Schema schema = new Schema();
            schema.setName("schemaTestMultipleRoots");

            Processor processor = new Processor();

            Field targetClassesField = processor.getClass().getDeclaredField("targetClasses");
            targetClassesField.setAccessible(true);
            targetClassesField.set(processor, null);

            Set<Class<?>> targetClasses = new HashSet<>();
            targetClasses.add(createTestClassRootType("targetClassOne"));
            targetClasses.add(createTestClassRootType("targetClassTwo"));

            targetClassesField.set(processor, targetClasses);

            Method buildSchemaMethod = processor.getClass().getDeclaredMethod("buildSchema", Schema.class);
            buildSchemaMethod.setAccessible(true);
            buildSchemaMethod.invoke(processor, schema);

            Assert.fail("Expected IllegalArgumentException when multiple roots type declarations are made");
        } catch (InvocationTargetException e) {
            Assert.assertEquals("Only one rootType declaration is allowed", e.getCause().getMessage());
        }
    }

    /**
     * Test classes, internal to this test
     */
    @FlatBufferComment(comment = "")
    @FlatBufferEnum
    enum TestEnumCommentEmpty {

    }

    interface TestInterface {
        void someTestMethod();
    }

    class TestAnonymousClass {

        public TestInterface innerAnonymousField;

        public void someClassMethod(TestInterface input) {
            innerAnonymousField = input;
        }
    }
}
