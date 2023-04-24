package com.michaelhradek.aurkitu.plugin.core;

import com.michaelhradek.aurkitu.annotations.FlatBufferEnum;
import com.michaelhradek.aurkitu.annotations.FlatBufferTable;
import com.michaelhradek.aurkitu.annotations.types.FieldType;
import com.michaelhradek.aurkitu.plugin.Config;
import com.michaelhradek.aurkitu.plugin.core.output.EnumDeclaration;
import com.michaelhradek.aurkitu.plugin.core.output.Schema;
import com.michaelhradek.aurkitu.plugin.core.output.TypeDeclaration;
import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class ValidatorTest {

    @Test
    public void testValidateSchema() throws MojoExecutionException {
        Processor processor = new Processor().withSourceAnnotation(FlatBufferTable.class)
                .withSourceAnnotation(FlatBufferEnum.class).withSchema(new Schema());

        processor.execute();

        Schema schema = processor.getProcessedSchemas().get(0);

        Validator validator = new Validator().withSchema(schema);
        validator.validateSchema();
        schema.setIsValid(validator.getErrors().isEmpty());

        Assert.assertEquals(false, schema.getIsValid());

        // Issue : TYPE_DEFINITION_NOT_DEFINED, Location: SampleClassTable, Name: anomalousSamples
        // Issue : TYPE_DEFINITION_NOT_DEFINED, Location: SampleClassTable, Name: dataMap
        // Issue : INVALID_PATH, Location: SampleClassTable, Name: definedInnerEnumArray, Comment: Array type name
        // contains '$'; using '@FlatBufferOptions(useFullName = true)' on inner not recommended:
        // SampleClassReferenced$SampleClassTableInnerEnumInt
        // Issue : TYPE_DEFINITION_NOT_DEFINED, Location: SampleClassTableWithUndefined, Name: awesomeUndefinedClass
        // Issue : MISCONFIGURED_DEFINITION, Location: SampleClassTableInnerEnumInt, Name: null
        // Issue : MISCONFIGURED_DEFINITION, Location: Option, Name: null

        for (Validator.Error error : validator.getErrors()) {
            if (error.getLocation().equals("Option")) {
                Assert.assertEquals(Validator.ErrorType.MISCONFIGURED_DEFINITION, error.getType());
                Assert.assertEquals(null, error.getProperty());
            }

            if (error.getType().equals(Validator.ErrorType.INVALID_PATH)) {
                Assert.assertEquals("SampleClassTable", error.getLocation());
                Assert.assertEquals("definedInnerEnumArray", error.getProperty().name);
                Assert.assertEquals("Array type name contains '$'; using '@FlatBufferOptions(useFullName = true)' on " +
                        "inner not recommended: com.michaelhradek.aurkitu.plugin.test" +
                        ".SampleClassReferenced$SampleClassTableInnerEnumInt", error.getComment());

                Assert.assertEquals("// Issue : INVALID_PATH, Location: SampleClassTable, Name: definedInnerEnumArray, Comment: Array type name contains '$'; using '@FlatBufferOptions(useFullName = true)' on inner not recommended: com.michaelhradek.aurkitu.plugin.test.SampleClassReferenced$SampleClassTableInnerEnumInt\n", error.toString());
            }
        }

        if (Config.DEBUG) {
            System.out.println(validator.getErrorComments());
        }

        final String errorComments = validator.getErrorComments();
        Assert.assertTrue(errorComments.contains("// Schema failed validation (i.e. flatc will likely fail): \n"));
        Assert.assertTrue(errorComments.contains("// Issue : TYPE_DEFINITION_NOT_DEFINED, Location: SampleClassReferenced, Name: samples\n"));
        Assert.assertTrue(errorComments.contains("// Issue : TYPE_DEFINITION_NOT_DEFINED, Location: SampleClassTableWithUndefined, Name: awesomeUndefinedClass\n"));
        Assert.assertTrue(errorComments.contains("// Issue : TYPE_DEFINITION_NOT_DEFINED, Location: SampleClassTable, Name: anomalousSamples\n"));
        Assert.assertTrue(errorComments.contains("// Issue : INVALID_PATH, Location: SampleClassTable, Name: definedInnerEnumArray, Comment: Array type name contains '$'; using '@FlatBufferOptions(useFullName = true)' on inner not recommended: com.michaelhradek.aurkitu.plugin.test.SampleClassReferenced$SampleClassTableInnerEnumInt\n"));
        Assert.assertTrue(errorComments.contains("// Issue : TYPE_DEFINITION_NOT_DEFINED, Location: SampleClassTable, Name: definedInnerEnumArray\n"));
        Assert.assertTrue(errorComments.contains("// Issue : TYPE_DEFINITION_NOT_DEFINED, Location: SampleClassTable, Name: fullnameClass\n"));
        Assert.assertTrue(errorComments.contains("// Issue : MISCONFIGURED_DEFINITION, Location: SampleClassTableInnerEnumInt, Name: null\n"));
        Assert.assertTrue(errorComments.contains("// Issue : ENUM_DEFINITION_NOT_DEFINED, Location: TestEnumCommentEmpty, Name: null, Comment: The enum contains no values.\n"));
        Assert.assertTrue(errorComments.contains("// Issue : MISCONFIGURED_DEFINITION, Location: Option, Name: null\n"));
    }

    @Test
    public void testValidateSchemaNullSchema() {
        Validator validator = new Validator();
        Assert.assertNull(validator.getSchema());
        validator.validateSchema();
        Assert.assertNull(validator.getSchema());
        validator.withSchema(null).validateSchema();
        Assert.assertNull(validator.getSchema());
    }

    @Test
    public void testGetErrorComments() {
        Validator validator = new Validator();
        Assert.assertTrue(validator.getErrors().isEmpty());
        Assert.assertTrue(validator.getErrorComments().equalsIgnoreCase("// Schema passed validation"));
    }

    @Test
    public void testValidateCheckTables() throws MojoExecutionException {
        Processor processor = new Processor().withSourceAnnotation(FlatBufferTable.class)
                .withSourceAnnotation(FlatBufferEnum.class).withSchema(new Schema());

        processor.execute();

        Schema schema = processor.getProcessedSchemas().get(0);

        Validator validator = new Validator().withSchema(schema);

        Assert.assertTrue(validator.isCheckTables());
        Assert.assertTrue(validator.isCheckEnums());
        Assert.assertEquals(0, validator.getErrors().size());

        validator.setCheckTables(false);
        validator.setCheckEnums(false);

        Assert.assertFalse(validator.isCheckTables());
        Assert.assertFalse(validator.isCheckEnums());
        validator.validateSchema();

        Assert.assertEquals(0, validator.getErrors().size());

        List<TypeDeclaration> types = schema.getTypeDeclarations();
        TypeDeclaration type = types.remove(0);

        TypeDeclaration.Property property = new TypeDeclaration.Property();
        property.name = "test-bad-ident-property";
        property.type = FieldType.IDENT;
        property.options.put(TypeDeclaration.Property.PropertyOptionKey.IDENT, "bad$ident-option");
        type.addProperty(property);

        types.add(type);
        schema.setTypeDeclarations(types);

        validator.withSchema(schema);
        validator.setCheckTables(true);
        validator.validateSchema();
        List<Validator.Error> errors = validator.getErrors();

        Assert.assertNotNull(errors);
        Assert.assertEquals(9, errors.size());

        boolean foundError = false;
        for (Validator.Error error : errors) {
            if ("test-bad-ident-property".equals(error.getProperty().name)) {
                Assert.assertEquals(Validator.ErrorType.INVALID_PATH, error.getType());
                foundError = true;
                break;
            }
        }

        Assert.assertTrue(foundError);
    }

    @Test
    public void testValidateNamespace() {
        Schema schema = new Schema();
        Validator validator = new Validator().withSchema(schema).withCheckEnums(false).withCheckTables(false);

        validator.validateSchema();

        Assert.assertEquals(0, validator.getErrors().size());

        schema.setNamespace("");
        validator.setSchema(schema);
        validator.validateSchema();
        Assert.assertEquals(0, validator.getErrors().size());

        schema.setNamespace("test-valid-namespace.com.michaelhradek");
        validator.setSchema(schema);
        validator.validateSchema();

        Assert.assertEquals(0, validator.getErrors().size());

        schema.setNamespace("test-invalid-nam3space.com.michaelhradek");
        validator.setSchema(schema);
        validator.validateSchema();
        Assert.assertEquals(1, validator.getErrors().size());
        Assert.assertEquals(Validator.ErrorType.INVALID_NAMESPACE, validator.getErrors().get(0).getType());

        validator.setErrors(new ArrayList<>());
        validator.setCheckNamespace(false);
        validator.validateSchema();
        Assert.assertEquals(0, validator.getErrors().size());
    }

    @Test
    public void testValidationFlags() {
        Validator validator = new Validator();
        Assert.assertTrue(validator.isCheckTables());
        Assert.assertTrue(validator.isCheckEnums());
        Assert.assertTrue(validator.isCheckNamespace());

        validator.setCheckTables(false);
        validator.setCheckEnums(false);
        validator.setCheckNamespace(false);

        Assert.assertFalse(validator.isCheckTables());
        Assert.assertFalse(validator.isCheckEnums());
        Assert.assertFalse(validator.isCheckNamespace());

        Assert.assertTrue(validator.getErrors().isEmpty());
        Validator.Error error = new Validator().new Error();
        List<Validator.Error> errors = new ArrayList<>();
        errors.add(error);
        validator.setErrors(errors);
        Assert.assertFalse(validator.getErrors().isEmpty());
        Assert.assertEquals(1, validator.getErrors().size());
    }

    @Test
    public void testDefinitionExists() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        Validator validator = new Validator();

        Method definitionExistsMethod = validator.getClass().getDeclaredMethod("definitionExists", TypeDeclaration.Property.class);
        definitionExistsMethod.setAccessible(true);

        TypeDeclaration.Property property = new TypeDeclaration.Property();
        property.type = FieldType.IDENT;
        property.name = "test-missing-ident-type";
        property.options.put(TypeDeclaration.Property.PropertyOptionKey.IDENT, null);

        Boolean exists = (Boolean) definitionExistsMethod.invoke(validator, property);
        Assert.assertFalse(exists);
        Validator.Error error = validator.getErrors().get(0);

        Assert.assertNotNull(error);
        Assert.assertEquals(Validator.ErrorType.MISSING_OR_INVALID_TYPE, error.getType());
        Assert.assertEquals(property.name, error.getLocation());

        // Test other cases
        EnumDeclaration enumDeclaration = new EnumDeclaration();
        enumDeclaration.setName("test-enum-declaration");
        Schema schema = new Schema();
        schema.addEnumDeclaration(enumDeclaration);

        TypeDeclaration.Property propertyTest = new TypeDeclaration.Property();
        propertyTest.type = FieldType.MAP;
        propertyTest.name = "test-map-property";
        propertyTest.options.put(TypeDeclaration.Property.PropertyOptionKey.MAP, "test-map-option-name");

        validator.setSchema(schema);

        exists = (Boolean) definitionExistsMethod.invoke(validator, propertyTest);
        Assert.assertFalse(exists);

        EnumDeclaration enumDeclarationTwo = new EnumDeclaration();
        enumDeclarationTwo.setName("Test-enum-declaration-two");
        schema.addEnumDeclaration(enumDeclarationTwo);

        TypeDeclaration.Property propertyTestTwo = new TypeDeclaration.Property();
        propertyTestTwo.type = FieldType.MAP;
        propertyTestTwo.name = "test-map-property-two";
        propertyTestTwo.options.put(TypeDeclaration.Property.PropertyOptionKey.MAP, "test-map-option-name-two");

        validator.setSchema(schema);

        exists = (Boolean) definitionExistsMethod.invoke(validator, propertyTestTwo);
        Assert.assertFalse(exists);

        EnumDeclaration enumDeclarationThree = new EnumDeclaration();
        enumDeclarationThree.setName("Test-enum-declaration-three");
        schema.addEnumDeclaration(enumDeclarationThree);

        TypeDeclaration.Property propertyTestThree = new TypeDeclaration.Property();
        propertyTestThree.type = FieldType.MAP;
        propertyTestThree.name = "test-map-property-three";
        propertyTestThree.options.put(TypeDeclaration.Property.PropertyOptionKey.MAP, "Test-enum-declaration-three");

        validator.setSchema(schema);

        exists = (Boolean) definitionExistsMethod.invoke(validator, propertyTestThree);
        Assert.assertTrue(exists);
    }

    @Test
    public void testDefinitionExistsPrimitiveArrays() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        Validator validator = new Validator();
        Schema schema = new Schema();
        validator.setSchema(schema);

        Method definitionExistsMethod = validator.getClass().getDeclaredMethod("definitionExists", TypeDeclaration.Property.class);
        definitionExistsMethod.setAccessible(true);

        TypeDeclaration.Property property = new TypeDeclaration.Property();
        property.type = FieldType.ARRAY;
        property.name = "test-array-property-primitive";
        property.options.put(TypeDeclaration.Property.PropertyOptionKey.ARRAY, "string");

        Boolean exists = (Boolean) definitionExistsMethod.invoke(validator, property);
        Assert.assertTrue(exists);

        property.options.put(TypeDeclaration.Property.PropertyOptionKey.ARRAY, "String");

        exists = (Boolean) definitionExistsMethod.invoke(validator, property);
        Assert.assertFalse(exists);

        property.options.put(TypeDeclaration.Property.PropertyOptionKey.ARRAY, "com.some.package.String");

        exists = (Boolean) definitionExistsMethod.invoke(validator, property);
        Assert.assertFalse(exists);

        property.options.put(TypeDeclaration.Property.PropertyOptionKey.ARRAY, "com.some.package.string");

        exists = (Boolean) definitionExistsMethod.invoke(validator, property);
        Assert.assertFalse(exists);
    }


    @Test
    public void testIsFlatbufferTypeByName() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        Validator validator = new Validator();

        // Get the private method that we are testing
        Method method = validator.getClass().getDeclaredMethod("isFlatbufferTypeByName", String.class);
        method.setAccessible(true);

        Assert.assertTrue((Boolean) method.invoke(validator, "byte"));
        Assert.assertTrue((Boolean) method.invoke(validator, "int"));
        Assert.assertTrue((Boolean) method.invoke(validator, "short"));
        Assert.assertTrue((Boolean) method.invoke(validator, "long"));
        Assert.assertTrue((Boolean) method.invoke(validator, "string"));
        Assert.assertTrue((Boolean) method.invoke(validator, "float"));
        Assert.assertTrue((Boolean) method.invoke(validator, "double"));
        Assert.assertTrue((Boolean) method.invoke(validator, "bool"));

        Assert.assertFalse((Boolean) method.invoke(validator, "com.package.string"));
        Assert.assertFalse((Boolean) method.invoke(validator, "com.package.some.String"));
        Assert.assertFalse((Boolean) method.invoke(validator, "Boolean"));
        Assert.assertFalse((Boolean) method.invoke(validator, "Bool"));
        Assert.assertFalse((Boolean) method.invoke(validator, "String"));
    }
}
