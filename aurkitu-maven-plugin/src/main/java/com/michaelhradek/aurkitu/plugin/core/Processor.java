package com.michaelhradek.aurkitu.plugin.core;

import com.michaelhradek.aurkitu.annotations.*;
import com.michaelhradek.aurkitu.annotations.types.EnumType;
import com.michaelhradek.aurkitu.annotations.types.FieldType;
import com.michaelhradek.aurkitu.plugin.Application;
import com.michaelhradek.aurkitu.plugin.core.output.EnumDeclaration;
import com.michaelhradek.aurkitu.plugin.core.output.Schema;
import com.michaelhradek.aurkitu.plugin.core.output.TypeDeclaration;
import com.michaelhradek.aurkitu.plugin.core.output.TypeDeclaration.Property;
import com.michaelhradek.aurkitu.plugin.core.output.TypeDeclaration.Property.PropertyOptionKey;
import com.michaelhradek.aurkitu.plugin.core.parsing.AnnotationParser;
import com.michaelhradek.aurkitu.plugin.core.parsing.ArtifactReference;
import com.michaelhradek.aurkitu.plugin.core.parsing.ClasspathReference;
import com.michaelhradek.aurkitu.plugin.core.parsing.ClasspathSearchType;
import lombok.Getter;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.Map.Entry;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

/**
 * @author m.hradek
 *
 */
@Getter
public class Processor {

    private List<Class<? extends Annotation>> sourceAnnotations;
    private Set<Class<?>> targetClasses;
    private ArtifactReference artifactReference;
    private Set<String> warnedTypeNames;
    private Map<String, String> namespaceOverrideMap;
    private List<String> specifiedDependencies;
    private boolean consolidatedSchemas = true;
    private List<Schema> candidateSchemas;
    private List<Schema> processedSchemas;
    private boolean validateSchemas = false;

    // Internal member
    private Schema currentSchema;

    /**
     * An unconsolidated schema means that we do not grab and build all the various inherited models into the target
     * schema.
     * Therefore, each schema will need to be aware of which dependencies it has and what models in that schema refer
     * to these dependencies.
     * <p>
     * The schema namespace will be the decider of the dependency/schema ownership. If the artifact &amp; group ids do
     * not match
     * then a new schema is assumed. Then those models will be marked as requiring a full path. Finally, a new schema
     * is added.
     */

    public Processor() {
        sourceAnnotations = new ArrayList<>();
        warnedTypeNames = new HashSet<>();

        // This could be null as the value via Application could be overriden here
        namespaceOverrideMap = new HashMap<>();
        candidateSchemas = new ArrayList<>();
    }

    /**
     * @param targetAnnotation Add Aurkitu annotation to process.
     * @return an instance of the Processor object
     */
    public Processor withSourceAnnotation(Class<? extends Annotation> targetAnnotation) {
        sourceAnnotations.add(targetAnnotation);
        return this;
    }

    /**
     *
     * @param artifactReference The ArtifactReference component
     * @return an instance of the Processor object
     */
    public Processor withArtifactReference(ArtifactReference artifactReference) {
        this.artifactReference = artifactReference;
        return this;
    }

    /**
     * By setting this we override namespaces found while building TypeDeclaration
     *
     * @param namespaceOverrideMap The namespace map
     * @return an instance of the Processor object
     */
    public Processor withNamespaceOverrideMap(Map<String, String> namespaceOverrideMap) {
        if (namespaceOverrideMap == null) {
            return this;
        }

        // Formatting the input so it is consistent
        Map<String, String> temp = new HashMap<>();
        for (Entry<String, String> item : namespaceOverrideMap.entrySet()) {
            Application.getLogger().debug(String.format("Reviewing namespaceOverrideMap item key: %s, value %s",
                    item.getKey(), item.getValue()));

            temp.put(item.getKey().endsWith(".") ? item.getKey() : item.getKey() + ".",
                    item.getValue().endsWith(".") ? item.getValue() : item.getValue() + ".");
        }

        this.namespaceOverrideMap = temp;
        return this;
    }

    /**
     *
     * @param specifiedDependencies Override the default target project dependency search and only search these dependencies with this group id
     * @return an instance of the Processor object
     */
    public Processor withSpecifiedDependencies(List<String> specifiedDependencies) {
        if (specifiedDependencies == null) {
            return this;
        }

        this.specifiedDependencies = specifiedDependencies;
        return this;
    }

    /**
     * @param consolidatedSchemas boolean if we should consolidate the schema
     * @return an instance of the Processor object
     */
    public Processor withConsolidatedSchemas(Boolean consolidatedSchemas) {
        if (consolidatedSchemas == null) {
            return this;
        }

        this.consolidatedSchemas = consolidatedSchemas;
        return this;
    }

    /**
     *
     * @param schema Set the processor to use this schema. Clear any previously added schemas
     * @return an instance of the Processor object
     */
    public Processor withSchema(Schema schema) {
        this.candidateSchemas.clear();
        this.candidateSchemas.add(schema);
        return this;
    }

    /**
     * @param schemas Set the processor to use this list of schemas. Clear any previously added schemas
     * @return an instance of the Processor object
     */
    public Processor withSchemas(List<Schema> schemas) {
        this.candidateSchemas.clear();
        this.candidateSchemas.addAll(schemas);
        return this;
    }

    /**
     * @param schema Adds a schemas to a list of schemas to process
     * @return an instance of the Processor object
     */
    public Processor addSchema(Schema schema) {
        this.candidateSchemas.add(schema);
        return this;
    }

    /**
     * @param schemas Adds a list of schemas to a list of schemas to process
     * @return an instance of the Processor object
     */
    public Processor addAllSchemas(List<Schema> schemas) {
        this.candidateSchemas.addAll(schemas);
        return this;
    }

    /**
     *
     * @param validateSchemas Set whether or not the processor should validate the schemas. Sending null does not alter the setting
     * @return an instance of the Processor object
     */
    public Processor withValidateSchemas(Boolean validateSchemas) {
        if (validateSchemas == null) {
            return this;
        }

        this.validateSchemas = validateSchemas;
        return this;
    }

    /**
     * Executes the processor against the settings and schemas specified
     *
     * @throws MojoExecutionException if anything goes wrong
     */
    public void execute() throws MojoExecutionException {
        Application.getLogger().debug("Processor: Execution commencing...");
        List<Schema> processedSchemas = new ArrayList<>();
        Application.getLogger().debug("    Number of schemas to process: " + candidateSchemas.size());
        for (Schema schema : candidateSchemas) {
            Application.getLogger().debug("      Current schema: " + schema.getNamespace());
            this.currentSchema = schema;
            this.targetClasses = new HashSet<>();

            Schema builtSchema = buildSchema(schema);
            if (!builtSchema.isEmpty()) {
                // Only put schemas that have declarations in them.
                processedSchemas.add(builtSchema);
            }
        }

        if (validateSchemas) {
            Application.getLogger().debug("    Validating schemas");

            for (int i = 0; i < processedSchemas.size(); i++) {
                Schema validationSchema = processedSchemas.get(i);
                Validator validator = new Validator().withSchema(validationSchema);
                validator.validateSchema();
                validationSchema.setIsValid(validator.getErrors().isEmpty());
                validationSchema.setValidator(validator);
                Application.getLogger().info(validator.getErrorComments());
                processedSchemas.set(i, validationSchema);
            }
        }

        this.processedSchemas = processedSchemas;
        Application.getLogger().debug("Processor: Execution complete");
    }

    /**
     * @param schema a preconfigured schema
     * @return a completed schema
     * @throws MojoExecutionException when there is a MalformedURLException in the classpathElements
     */
    private Schema buildSchema(Schema schema) throws MojoExecutionException {
        Application.getLogger().debug("Start building schema: " + schema.getName());
        for (Class<? extends Annotation> source : sourceAnnotations) {
            if (artifactReference == null || artifactReference.getMavenProject() == null) {
                Application.getLogger().debug("MavenProject is null; falling back to built in class scanner");
                targetClasses.addAll(AnnotationParser.findAnnotatedClasses(source));
            } else {
                targetClasses.addAll(AnnotationParser.findAnnotatedClasses(artifactReference,
                        schema.getClasspathReferenceList(), source));
            }
        }

        if (targetClasses.size() < 1) {
            Application.getLogger().debug("  No target classes found; skipping schema creation");
            schema.isEmpty(true);
            return schema;
        }

        // The targetClasses includes ALL annotated classes including those inside dependencies
        int rootTypeCount = 0;
        for (Class<?> clazz : targetClasses) {
            if (isEnumWorkaround(clazz)) {
                schema.addEnumDeclaration(buildEnumDeclaration(clazz));
                continue;
            }

            if (clazz instanceof Class) {
                TypeDeclaration temp = buildTypeDeclaration(schema, clazz);
                if (temp.isRoot()) {
                    Application.getLogger().debug("  Found root: " + temp.getName());
                    rootTypeCount++;
                    if (rootTypeCount > 1) {
                        throw new IllegalArgumentException("Only one rootType declaration is allowed");
                    }

                    schema.setRootType(temp.getName());
                }

                if (consolidatedSchemas || !getExternalClassDefinitionDetails(clazz).locatedOutside) {
                    schema.addTypeDeclaration(temp);
                }

                // Now examine inner classes
                Class<?>[] innerClasses = clazz.getDeclaredClasses();
                for (Class<?> inner : innerClasses) {
                    Application.getLogger().debug("  Processing inner class: " + inner.getSimpleName());
                    if (inner.isSynthetic()) {
                        Application.getLogger().debug("  Found synthetic...");
                        continue;
                    }

                    if (isEnumWorkaround(inner)) {
                        Application.getLogger().debug("  Found enum...");
                        schema.addEnumDeclaration(buildEnumDeclaration(inner));
                        continue;
                    }

                    Application.getLogger().debug("  Found type...");
                    // Inner classes cannot be root type
                    if (consolidatedSchemas || !getExternalClassDefinitionDetails(inner).locatedOutside) {
                        schema.addTypeDeclaration(buildTypeDeclaration(schema, inner));
                    }
                }
            }
        }

        return schema;
    }

    /**
     *
     * @param enumClass Class to test if it is an Enum
     * @return boolean
     */
    public boolean isEnumWorkaround(Class<?> enumClass) {
        Class<?> temp = enumClass;
        if (temp.isAnonymousClass()) {
            temp = temp.getSuperclass();
        }

        return temp.isEnum();
    }

    /**
     * @param clazz Class which is being considered for an EnumDeclaration
     * @return an EnumDeclaration
     */
    public EnumDeclaration buildEnumDeclaration(Class<?> clazz) {
        Application.getLogger().debug("Building Enum: " + clazz.getName());

        EnumDeclaration enumD = new EnumDeclaration();
        enumD.setName(clazz.getSimpleName());

        Annotation annotation = clazz.getAnnotation(FlatBufferEnum.class);
        if (annotation instanceof FlatBufferEnum) {
            FlatBufferEnum myFlatBufferEnum = (FlatBufferEnum) annotation;
            Application.getLogger().debug("Enum structure: " + myFlatBufferEnum.value());
            enumD.setStructure(myFlatBufferEnum.value());
            Application.getLogger().debug("Enum type: " + myFlatBufferEnum.enumType());
            enumD.setType(myFlatBufferEnum.enumType());
        } else {
            Application.getLogger().debug("Not FlatBufferEnum (likely inner class); Generic enum created");
        }

        annotation = clazz.getAnnotation(FlatBufferComment.class);
        if (annotation != null) {
            String comment = ((FlatBufferComment) annotation).comment();
            if (!comment.isEmpty()) {
                Application.getLogger().debug("Found a comment assign to enum: " + comment);
                enumD.setComment(comment);
            }
        }

        Field[] fields = clazz.getDeclaredFields();

        // Find what field was annotated as the value we need to use for the declared type
        boolean setValues = false;
        Field valueField = null;
        int numAnnotations = 0;
        if (fields != null && fields.length > 0) {
            Application.getLogger().debug("Enum with declared fields detected");
            for (Field field : fields) {
                Application.getLogger().debug("  Field: " + field.getName() + " type:" + field.getType().getSimpleName());
                if (field.getAnnotation(FlatBufferEnumTypeField.class) != null) {
                    Application.getLogger().debug("    Annotated field");

                    // Verify the declaration on the enum matches the declaration of the field
                    if (enumD.getType() == null) {
                        throw new IllegalArgumentException(
                                "Missing @FlatBufferEnum(enumType = EnumType.<SELECT>) declaration or remove @FlatBufferEnumTypeField for: "
                                        + clazz.getName());
                    }

                    if (field.getType().isAssignableFrom(enumD.getType().targetClass)) {
                        setValues = true;
                        valueField = field;
                        numAnnotations++;
                    }
                }
            }
        }

        if (numAnnotations > 1) {
            throw new IllegalArgumentException(
                    "Can only declare one @FlatBufferEnumTypeField for Enum: " + clazz.getName());
        }

        Object[] constants = clazz.getEnumConstants();

        // If we want the value then try and grab it
        for (Object constant : constants) {
            Application.getLogger().debug("Adding value to Enum: " + constant.toString());

            if (setValues) {
                valueField.setAccessible(true);
                try {
                    final String temp = constant.toString() + " = ";

                    if (enumD.getType() == EnumType.BYTE || enumD.getType() == EnumType.UBYTE) {
                        enumD.addValue(temp + valueField.getByte(constant));
                        continue;
                    }

                    if (enumD.getType() == EnumType.SHORT || enumD.getType() == EnumType.USHORT) {
                        enumD.addValue(temp + valueField.getShort(constant));
                        continue;
                    }

                    if (enumD.getType() == EnumType.LONG || enumD.getType() == EnumType.ULONG) {
                        enumD.addValue(temp + valueField.getLong(constant));
                        continue;
                    }

                    if (enumD.getType() == EnumType.INT || enumD.getType() == EnumType.UINT) {
                        enumD.addValue(temp + valueField.getInt(constant));
                        continue;
                    }

                    throw new IllegalArgumentException(
                            "Enum type must be integral (i.e. byte, ubyte, short, ushort, int, unint, long, or ulong");
                } catch (IllegalAccessException e) {
                    Application.getLogger().error("Not allowed to grab Enum field value: ", e);
                }
            } else {
                // Otherwise, just use the name of the constant
                enumD.addValue(constant.toString());
            }
        }

        return enumD;
    }

    /**
     *
     * @param schema The schema currently being considered while reviewing this class
     * @param clazz Class which is being considered for an TypeDeclaration
     * @return a TypeDeclaration
     */
    public TypeDeclaration buildTypeDeclaration(Schema schema, Class<?> clazz) {
        Application.getLogger().debug("Building Type: " + clazz.getName());

        TypeDeclaration type = new TypeDeclaration();
        type.setName(clazz.getSimpleName());

        Annotation annotation = clazz.getAnnotation(FlatBufferTable.class);
        Application.getLogger().debug("Number of annotations of clazz: " + clazz.getDeclaredAnnotations().length);
        if (annotation instanceof FlatBufferTable) {
            FlatBufferTable myFlatBufferTable = (FlatBufferTable) annotation;
            Application.getLogger().debug("Declared root: " + myFlatBufferTable.rootType());
            type.setRoot(myFlatBufferTable.rootType());
            Application.getLogger().debug("Table structure: " + myFlatBufferTable.value());
            type.setStructure(myFlatBufferTable.value());
        } else {
            Application.getLogger().debug("Not FlatBufferTable (likely inner class); Generic table created");
        }

        annotation = clazz.getAnnotation(FlatBufferComment.class);
        if (annotation != null) {
            String comment = ((FlatBufferComment) annotation).comment();
            if (!comment.isEmpty()) {
                Application.getLogger().debug("Found a comment assign to type: " + comment);
                type.setComment(comment);
            }
        }

        // TODO: If we aren't separated schemas then check to see if the fields are a class which lives im a dependency.
        //  This will be challenging for things like a List or Map of dependency types
        List<Field> fields = getDeclaredAndInheritedPrivateFields(clazz);
        for (Field field : fields) {
            Application.getLogger().debug("Number of annotations found: " + field.getDeclaredAnnotations().length);

            if (field.getAnnotation(FlatBufferIgnore.class) != null) {
                Application.getLogger().debug("Ignoring property: " + field.getName());
                continue;
            }

            Application.getLogger().debug("Adding property to Type: " + field.getName());
            type.addProperty(getPropertyForField(schema, field));
        }

        return type;
    }

    /**
     * @param type Class which needs to be traversed up to determine which fields are to be
     *        considered as candidates for declaration
     * @return A list of valid fields
     */
    private List<Field> getDeclaredAndInheritedPrivateFields(Class<?> type) {
        List<Field> result = new ArrayList<>();

        Class<?> clazz = type;
        while (clazz != null && clazz != Object.class) {
            for (Field field : clazz.getDeclaredFields()) {
                if (!field.isSynthetic()) {
                    Collections.addAll(result, field);
                }
            }
            clazz = clazz.getSuperclass();
        }

        return result;
    }

    /**
     *
     * @param schema The schema currently being considered while reviewing this field
     * @param field A class field
     * @return A type declaration Property. This contains the name of the field and type
     *         {@link FieldType}. When encountering an array or ident (Indentifier) the options
     *         property is used to store additional information.
     */
    public Property getPropertyForField(Schema schema, final Field field) {
        Property property = new Property();

        // Some uses in which we reference other namespaces require us to declare the entirety of
        // the name
        Annotation annotation = field.getAnnotation(FlatBufferFieldOptions.class);
        boolean useFullName = false;
        String defaultValue = null;

        // Process overrides for the field
        if (annotation != null) {
            useFullName = ((FlatBufferFieldOptions) annotation).useFullName();
            defaultValue = ((FlatBufferFieldOptions) annotation).defaultValue();
        }

        // Apply a comment if the annotation exists
        annotation = field.getAnnotation(FlatBufferComment.class);
        if (annotation != null) {
            String comment = ((FlatBufferComment) annotation).comment();
            if (!comment.isEmpty()) {
                Application.getLogger().debug("Found a comment assign to field: " + comment);
                property.options.put(PropertyOptionKey.COMMENT, comment);
            }
        }

        // Apply the default value if it was set
        if (defaultValue != null && !defaultValue.isEmpty()) {
            Application.getLogger().debug("Found a default value to assign to field: " + defaultValue);
            property.options.put(PropertyOptionKey.DEFAULT_VALUE, defaultValue);
        }

        if (field.getType().isAssignableFrom(int.class) || field.getType().isAssignableFrom(Integer.class)) {
            property.name = field.getName();
            property.type = FieldType.INT;
            return property;
        }

        if (field.getType().isAssignableFrom(String.class)) {
            property.name = field.getName();
            property.type = FieldType.STRING;
            return property;
        }

        if (field.getType().isAssignableFrom(long.class) || field.getType().isAssignableFrom(Long.class)) {
            property.name = field.getName();
            property.type = FieldType.LONG;
            return property;
        }

        if (field.getType().isAssignableFrom(short.class) || field.getType().isAssignableFrom(Short.class)) {
            property.name = field.getName();
            property.type = FieldType.SHORT;
            return property;
        }

        if (field.getType().isAssignableFrom(boolean.class) || field.getType().isAssignableFrom(Boolean.class)) {
            property.name = field.getName();
            property.type = FieldType.BOOL;
            return property;
        }

        if (field.getType().isAssignableFrom(byte.class) || field.getType().isAssignableFrom(Byte.class)) {
            property.name = field.getName();
            property.type = FieldType.BYTE;
            return property;
        }

        if (field.getType().isAssignableFrom(float.class) || field.getType().isAssignableFrom(Float.class)) {
            property.name = field.getName();
            property.type = FieldType.FLOAT;
            return property;
        }

        if (field.getType().isAssignableFrom(double.class) || field.getType().isAssignableFrom(Double.class)) {
            property.name = field.getName();
            property.type = FieldType.DOUBLE;
            return property;
        }

        // EX: String[], SomeClass[]
        if (field.getType().isArray()) {
            return processArray(property, field, useFullName);
        }

        // EX: Map<String, Object>
        if (field.getType().isAssignableFrom(Map.class)) {
            return processMap(property, schema, field, useFullName);
        }

        // EX: List<String>, List<SomeClass>
        if (field.getType().isAssignableFrom(List.class)) {
            return processList(property, field, useFullName);
        }

        // Anything else
        return processClass(property, field, useFullName);
    }

    /**
     * @param property    The property to populate with additional data about a field
     * @param field       The field to examine. In this case the field is a class
     * @param useFullName Whether or not to use the full class name including the package or just the name
     * @return The completed property struct
     */
    public Property processClass(Property property, Field field, boolean useFullName) {
        String name = field.getName();
        Application.getLogger().debug("Found unrecognized type; assuming Type.IDENT(IFIER): " + name);
        property.name = name;
        property.type = FieldType.IDENT;

        Type fieldType = field.getGenericType();
        String identName = null;

        try {
            Class<?> clazz;
            if (artifactReference != null && artifactReference.getMavenProject() != null) {
                clazz = getClassForClassName(artifactReference.getMavenProject(), currentSchema, fieldType.getTypeName());
            } else {
                clazz = Thread.currentThread().getContextClassLoader().loadClass(fieldType.getTypeName());
            }

            // Consolidated versus separated schema check
            if (!consolidatedSchemas) {
                Application.getLogger().debug("Separated schemas requested; reviewing class");
                ExternalClassDefinition externalClassDefinition = getExternalClassDefinitionDetails(clazz);
                if (externalClassDefinition.locatedOutside) {
                    identName = externalClassDefinition.targetNamespace + "." + clazz.getSimpleName();
                }
            } else {
                identName = useFullName ? clazz.getName() : clazz.getSimpleName();
            }

            if (useFullName) {
                String simpleName = clazz.getName().substring(clazz.getName().lastIndexOf(".") + 1);
                String packageName = clazz.getName().substring(0, clazz.getName().lastIndexOf(".") + 1);
                if (namespaceOverrideMap != null && namespaceOverrideMap.containsKey(packageName)) {
                    identName = namespaceOverrideMap.get(packageName) + simpleName;
                    Application.getLogger().debug("Override located; using it: " + identName);
                }
            }
        } catch (Exception e) {
            if (!warnedTypeNames.contains(fieldType.getTypeName())) {
                if (e instanceof ClassNotFoundException) {
                    Application.getLogger().warn("Class not found for type name: " + fieldType.getTypeName());
                } else {
                    Application.getLogger().warn("Unable to get class for name: " + fieldType.getTypeName(), e);
                }
                warnedTypeNames.add(fieldType.getTypeName());
            }

            if (useFullName) {
                identName = fieldType.getTypeName();
                String simpleName = identName.substring(identName.lastIndexOf(".") + 1);
                String packageName = identName.substring(0, identName.lastIndexOf(".") + 1);
                if (namespaceOverrideMap != null && namespaceOverrideMap.containsKey(packageName)) {
                    identName = namespaceOverrideMap.get(packageName) + simpleName;
                }
            } else {
                identName = fieldType.getTypeName().substring(fieldType.getTypeName().lastIndexOf(".") + 1);

                if (identName.contains("$")) {
                    identName = identName.substring(identName.lastIndexOf("$"));
                }

                Application.getLogger().debug("Trimmed: " + fieldType.getTypeName() + " to " + identName);
            }
        }

        property.options.put(Property.PropertyOptionKey.IDENT, identName);
        return property;
    }

    /**
     * @param property    The property to populate with additional data about a field
     * @param field       The field to examine. In this case the field is an array
     * @param useFullName Whether or not to use the full class name including the package or just the name
     * @return The completed property struct
     */
    public Property processArray(Property property, Field field, boolean useFullName) {
        property.name = field.getName();
        property.type = FieldType.ARRAY;

        // Determine type of the array
        String name = field.getType().getComponentType().getSimpleName();
        if (Utilities.isLowerCaseType(field.getType().getComponentType())) {
            Application.getLogger().debug("Array parameter is primitive, wrapper, or String: " + field.getName());
            name = name.toLowerCase();
        } else {
            // It may be a Class<?> which isn't a primative (i.e. lowerCaseType)
            if (useFullName) {
                name = field.getType().getComponentType().getName();

                String simpleName = field.getType().getComponentType().getName()
                        .substring(field.getType().getComponentType().getName().lastIndexOf(".") + 1);
                String packageName = field.getType().getComponentType().getName()
                        .substring(0, field.getType().getComponentType().getName().lastIndexOf(".") + 1);
                Application.getLogger().debug(String
                        .format("Using full name; reviewing simpleName: %s and package: %s", simpleName, packageName));

                if (namespaceOverrideMap != null && namespaceOverrideMap.containsKey(packageName)) {
                    name = namespaceOverrideMap.get(packageName) + simpleName;
                    Application.getLogger().debug("Override located; using it: " + name);
                }
            } else {
                name = field.getType().getComponentType().getSimpleName();
            }
        }

        // In the end Array[] and List<?> are represented the same way.
        property.options.put(PropertyOptionKey.ARRAY, name);
        return property;
    }

    /**
     * @param property    The property to populate with additional data about a field
     * @param schema The schema currently being considered while reviewing this field
     * @param field       The field to examine. In this case the field is a map
     * @param useFullName Whether or not to use the full class name including the package or just the name
     * @return The completed property struct
     */
    public Property processMap(Property property, Schema schema, Field field, boolean useFullName) {
        property.name = field.getName();
        property.type = FieldType.MAP;

        // Stuff the types into this list
        List<Property> properties = new ArrayList<>();

        // Get the type for the key and value
        String[] parametrizedTypeStrings = new String[]{String.class.getName(), String.class.getName()};
        try {
            parametrizedTypeStrings = parseFieldSignatureForParametrizedTypeStringsOnMap(field);
        } catch (Exception e) {
            Application.getLogger().warn("Unable to determine classes for Map<?, ?> parameter types", e);
        }

        // Attempt to load each type
        for (int i = 0; i < parametrizedTypeStrings.length; i++) {
            Class<?> mapTypeClass;
            Property mapTypeProperty = new Property();

            try {
                // Load all paths into custom classloader
                ClassLoader urlClassLoader = Thread.currentThread().getContextClassLoader();
                if (artifactReference != null && artifactReference.getMavenProject() != null) {
                    urlClassLoader = URLClassLoader

                            // TODO This needs thought
                            .newInstance(Utilities.arrayForClasspathReferenceList(
                                    Utilities.buildProjectClasspathList(artifactReference, ClasspathSearchType.BOTH)), urlClassLoader);
                }

                // Parse Field signature
                mapTypeClass = urlClassLoader.loadClass(parametrizedTypeStrings[i]);

                if (mapTypeClass.getName().equals(Object.class.getName())) {
                    Application.getLogger().warn(
                            "Using Map<?, ?> where either `?` is `java.lang.Object` is not permitted; using `java.lang.String`");
                    mapTypeClass = String.class;
                }
            } catch (Exception e) {
                Application.getLogger()
                        .warn("Unable to find and load class for Map<?, ?> parameter, using <String, String> instead: ",
                                e);
                mapTypeClass = String.class;
            }

            String name = getName(mapTypeClass, field, useFullName);

            // Stuffing...
            if (i == 0) {
                mapTypeProperty.name = "key";
            } else {
                mapTypeProperty.name = "value";
            }

            mapTypeProperty.type = FieldType.IDENT;
            mapTypeProperty.options.put(PropertyOptionKey.IDENT, name);
            properties.add(mapTypeProperty);
        }

        // Create a new type and add it to the list of types
        TypeDeclaration mapType = new TypeDeclaration();
        final String mapTypeName = TypeDeclaration.MapValueSet.class.getSimpleName() + "_"
                + field.getDeclaringClass().getSimpleName() + "_" + field.getName();
        mapType.setName(mapTypeName);
        mapType.setComment("Auto-generated type for use with Map<?, ?>");

        // Set in this type the various types for the K/Vs used in this map
        mapType.setProperties(properties);
        schema.addTypeDeclaration(mapType);

        // Need a way to reference back to the new generated type
        property.options.put(PropertyOptionKey.MAP, mapTypeName);
        return property;
    }

    /**
     * @param property    The property to populate with additional data about a field
     * @param field       The field to examine. In this case the field is a list
     * @param useFullName Whether or not to use the full class name including the package or just the name
     * @return The completed property struct
     */
    public Property processList(Property property, Field field, boolean useFullName) {
        property.name = field.getName();
        property.type = FieldType.ARRAY;

        Class<?> listTypeClass;

        try {
            // Load all paths into custom classloader
            ClassLoader urlClassLoader = Thread.currentThread().getContextClassLoader();
            if (artifactReference != null && artifactReference.getMavenProject() != null)
                if (consolidatedSchemas) {
                    urlClassLoader =
                            URLClassLoader.newInstance(Utilities.buildProjectClasspathList(artifactReference,
                                    ClasspathSearchType.BOTH).toArray(new URL[]{}), urlClassLoader);
                } else {
                    // TODO This needs thought
                }

            // Parse Field signature
            String parametrizedTypeString = parseFieldSignatureForParametrizedTypeStringOnList(field);
            listTypeClass = urlClassLoader.loadClass(parametrizedTypeString);
        } catch (Exception e) {
            Application.getLogger().warn("Unable to find and load class for List<?> parameter, using String instead: ", e);
            listTypeClass = String.class;
        }

        String name = getName(listTypeClass, field, useFullName);

        property.options.put(PropertyOptionKey.ARRAY, name);
        return property;
    }

    /**
     *
     * @param mavenProject The project details for class loader functionality
     * @param schema The schema currently being considered while reviewing this class
     * @param className The name of the class we need to locate
     * @return The class we located
     * @throws ClassNotFoundException if the class cannot be located
     * @throws IOException if one of the classpathElements are a malformed URL
     * @throws DependencyResolutionRequiredException if MavenProject is unable to resolve the
     *         compiled classpath elements
     */
    public static Class<?> getClassForClassName(MavenProject mavenProject, Schema schema, String className)
            throws ClassNotFoundException, DependencyResolutionRequiredException, IOException {

        List<String> classpathElements = mavenProject.getCompileClasspathElements();
        List<URL> projectClasspathList = new ArrayList<>();
        for (String element : classpathElements) {
            Application.getLogger().debug("Adding compiled classpath element (via MavenProject): " + element);
            projectClasspathList.add(new File(element).toURI().toURL());
        }

        for (ClasspathReference reference : schema.getClasspathReferenceList()) {
            Application.getLogger().debug("Adding classpath reference (via currentSchema): " + reference.getUrl());
            projectClasspathList.add(reference.getUrl());
        }

        URLClassLoader urlClassLoader = new URLClassLoader(projectClasspathList.toArray(new URL[] {}), Thread.currentThread().getContextClassLoader());

        Class<?> result = urlClassLoader.loadClass(className);
        urlClassLoader.close();
        return result;
    }

    /**
     *
     * @param input The List with a type declaration
     * @return a String which parses: com.company.team.service.model.Person from the following:
     *         Ljava/util/List&lt;Lcom/company/team/service/model/Person;&gt;;
     * @throws NoSuchFieldException if the Field does not exist in the class
     * @throws IllegalAccessException if the Field is inaccessible
     */
    public static String parseFieldSignatureForParametrizedTypeStringOnList(Field input)
            throws NoSuchFieldException, IllegalAccessException {
        String typeSignature = getFieldTypeSignature(input);

        typeSignature = typeSignature.replaceFirst("[a-zA-Z]{1}", "");
        typeSignature = typeSignature.replaceAll("/", ".");
        typeSignature = typeSignature.replaceAll(";", "");
        Application.getLogger().debug("Derived class: " + typeSignature);

        return typeSignature;
    }

    /**
     * @param input The Map with a set of type declarations
     * @return a list of String which parses {"java.lang.String", "java.lang.Object"} from the following:
     * Ljava/util/Map&lt;Ljava/lang/String;Ljava/lang/Object;&gt;;
     * @throws NoSuchFieldException if the Field does not exist in the class
     * @throws IllegalAccessException if the Field is inaccessible
     */
    public static String[] parseFieldSignatureForParametrizedTypeStringsOnMap(Field input)
            throws NoSuchFieldException, IllegalAccessException {
        String typeSignature = getFieldTypeSignature(input);

        typeSignature = typeSignature.replaceAll("/", ".");
        String[] listOfTypes = typeSignature.split(";");
        for (int i = 0; i < listOfTypes.length; i++) {
            listOfTypes[i] = listOfTypes[i].replaceAll(";", "");
            listOfTypes[i] = listOfTypes[i].replaceFirst("[a-zA-Z]{1}", "");
            Application.getLogger().debug(String.format("Derived class #%d: %s", i, listOfTypes[i]));
        }

        return listOfTypes;
    }

    /**
     * @param input The field for which we want the signature String.
     * @return The field type signature which is a private field in classes called "signature"
     * @throws NoSuchFieldException   if the field doesn't exist
     * @throws IllegalAccessException if access to the field is unavailable
     */
    public static String getFieldTypeSignature(Field input) throws NoSuchFieldException, IllegalAccessException {
        Field privateField = input.getClass().getDeclaredField("signature");
        privateField.setAccessible(true);
        String signature = (String) privateField.get(input);
        Application.getLogger().debug("Examining signature: " + signature);

        return signature.substring(signature.indexOf("<") + 1, signature.indexOf(">"));
    }

    /**
     * @param clazz       The target class which we are determining the name for
     * @param field       The current field being examined for context on how the class is being used
     * @param useFullName To use the full name orr not (i.e. include namespace or not)
     * @return The derived name for the class
     */
    public String getName(Class<?> clazz, Field field, boolean useFullName) {
        String name = clazz.getSimpleName();
        if (useFullName) {
            name = clazz.getName();

            String simpleName = clazz.getName()
                    .substring(clazz.getName().lastIndexOf(".") + 1);
            String packageName = clazz.getName()
                    .substring(0, clazz.getName().lastIndexOf(".") + 1);
            Application.getLogger().debug(String
                    .format("Using full name; reviewing simpleName: %s and package: %s", simpleName,
                            packageName));

            if (namespaceOverrideMap != null && namespaceOverrideMap.containsKey(packageName)) {
                name = namespaceOverrideMap.get(packageName) + simpleName;
                Application.getLogger().debug("Override located; using it: " + name);
            }
        }

        if (Utilities.isLowerCaseType(clazz)) {
            Application.getLogger()
                    .debug("Array parameter is primative, wrapper, or String: " + field.getName());
            name = name.toLowerCase();
        }

        return name;
    }

    /**
     * Enables multi-schemas support
     *
     * @param clazz the class we want to examine and determine where it is defined
     * @return ExternalClassDefinition which is populated with the target schema namespace and if it is externally defined
     * @throws MojoExecutionException if something goes wrong
     */
    public ExternalClassDefinition getExternalClassDefinitionDetails(Class<?> clazz) throws MojoExecutionException {
        ExternalClassDefinition externalClassDefintion = new ExternalClassDefinition();

        Application.getLogger().debug("This is a dependency. Therefor, skipping external class check. This assumes that a dependency schema is the sum of its sef and its dependencies. ");
        if (currentSchema.isDependency()) {
            // Consider if we want to recursivesly add more schemas as we find them.
            return externalClassDefintion;
        }

        Application.getLogger().debug("Determining if class was defined outside this schema");
        final String currentClazzPackageName = clazz.getPackage().getName();
        Application.getLogger().debug("  Class namespace for review: " + currentClazzPackageName);
        boolean classLocatedOutside = false;

        for (Schema oneOfSchemas : candidateSchemas) {
            Application.getLogger().debug(" Iterating schemas: " + oneOfSchemas.getNamespace());

            List<String> classNames = new ArrayList<>();
            for (ClasspathReference reference : oneOfSchemas.getClasspathReferenceList()) {

                final String jarPath = reference.getUrl().toString();
                if (!jarPath.endsWith(".jar!/")) {
                    continue;
                }

                try {
                    JarInputStream jarInputStream = new JarInputStream(new FileInputStream(jarPath.substring("jar:file:".length(), jarPath.indexOf("!/"))));
                    JarEntry jarEntry;

                    while (true) {
                        jarEntry = jarInputStream.getNextJarEntry();
                        if (jarEntry == null) {
                            break;
                        }

                        if ((jarEntry.getName().endsWith(".class"))) {
                            String classNameWithExtension = jarEntry.getName().replaceAll("/", "\\.");
                            String className = classNameWithExtension.substring(0, classNameWithExtension.lastIndexOf('.'));
                            classNames.add(className);
                        }
                    }
                } catch (IOException e) {
                    throw new MojoExecutionException(e.getMessage());
                }
            }

            if (classNames.contains(clazz.getName()) && !oneOfSchemas.equals(currentSchema)) {
                Application.getLogger().debug("  Found location of class which is outside current schema");
                externalClassDefintion.targetNamespace = oneOfSchemas.getNamespace();
                currentSchema.addInclude(externalClassDefintion.targetNamespace);

                classLocatedOutside = true;
                break;
            }
        }

        if (!classLocatedOutside) {
            Application.getLogger().debug("  Did not find class outside schema; continue processing");
        }

        externalClassDefintion.locatedOutside = classLocatedOutside;
        return externalClassDefintion;
    }

    /**
     * Enables multi-schemas support
     * <p>
     * Internal Processor class
     */
    private class ExternalClassDefinition {
        public String targetNamespace;
        public boolean locatedOutside;
    }
}