package com.michaelhradek.aurkitu.plugin.core;

import com.michaelhradek.aurkitu.annotations.*;
import com.michaelhradek.aurkitu.annotations.types.EnumType;
import com.michaelhradek.aurkitu.annotations.types.FieldType;
import com.michaelhradek.aurkitu.plugin.core.output.EnumDeclaration;
import com.michaelhradek.aurkitu.plugin.core.output.Schema;
import com.michaelhradek.aurkitu.plugin.core.output.TypeDeclaration;
import com.michaelhradek.aurkitu.plugin.core.output.TypeDeclaration.Property;
import com.michaelhradek.aurkitu.plugin.core.output.TypeDeclaration.Property.PropertyOptionKey;
import com.michaelhradek.aurkitu.plugin.core.output.components.Namespace;
import com.michaelhradek.aurkitu.plugin.core.parsing.AnnotationParser;
import com.michaelhradek.aurkitu.plugin.core.parsing.ArtifactReference;
import com.michaelhradek.aurkitu.plugin.core.parsing.ClasspathReference;
import com.michaelhradek.aurkitu.plugin.core.processing.ArrayProperties;
import com.michaelhradek.aurkitu.plugin.core.processing.ClassProperties;
import com.michaelhradek.aurkitu.plugin.core.processing.ListProperties;
import com.michaelhradek.aurkitu.plugin.core.processing.MapProperties;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.Map.Entry;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

/**
 * @author m.hradek
 */
@Getter
@Slf4j
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
    private Validator validator;
    private boolean ignoreStaticMembers;

    // Internal member
    private Schema currentSchema;

    /**
     * An unconsolidated schema means that we do not grab and build all the various inherited models into the target
     * schema.
     * Therefore, each schema will need to be aware of which dependencies it has and what models in that schema refer
     * to these dependencies.
     * <p>
     * The schema namespace will be the decider of the base/schema ownership. If the artifact &amp; group ids do
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
     * @param mavenProject The project details for class loader functionality
     * @param schema       The schema currently being considered while reviewing this class
     * @param className    The name of the class we need to locate
     * @return The class we located
     * @throws ClassNotFoundException                if the class cannot be located
     * @throws IOException                           if one of the classpathElements are a malformed URL
     * @throws DependencyResolutionRequiredException if MavenProject is unable to resolve the
     *                                               compiled classpath elements
     */
    public static Class<?> getClassForClassName(MavenProject mavenProject, Schema schema, String className)
            throws ClassNotFoundException, DependencyResolutionRequiredException, IOException {

        List<String> classpathElements = mavenProject.getCompileClasspathElements();
        List<URL> projectClasspathList = new ArrayList<>();
        for (String element : classpathElements) {
            log.debug("Adding compiled classpath element (via MavenProject): " + element);
            projectClasspathList.add(new File(element).toURI().toURL());
        }

        for (ClasspathReference reference : schema.getClasspathReferenceList()) {
            log.debug("Adding classpath reference (via currentSchema): " + reference.getUrl());
            projectClasspathList.add(reference.getUrl());
        }

        URLClassLoader urlClassLoader = new URLClassLoader(projectClasspathList.toArray(new URL[]{}), Thread.currentThread().getContextClassLoader());

        Class<?> result = urlClassLoader.loadClass(className);
        urlClassLoader.close();
        return result;
    }

    /**
     * Get a list of the names of all type classes from a field which is a parameterized type.
     * If the type classes themselves are also parameterized types, only the first sub-type class
     * from those type classes will be included.
     *
     * @param field The field of a ParameterizedType for which we want the actual type class names
     * @return A list of the names of all actual type classes in the ParameterizedType
     * @throws IllegalArgumentException if the type of field is not a ParameterizedType
     */
    public static String[] getTypeClassNamesFromParameterizedType(Field field) {
        Type type = field.getGenericType();
        if (!(type instanceof ParameterizedType)) {
            throw new IllegalArgumentException(
                "field " + field.getType() + " is not a parameterized type");
        }

        Type[] typeClasses = ((ParameterizedType) type).getActualTypeArguments();
        List<String> typeNames = new ArrayList<>();
        for (Type actualTypeArgument : typeClasses) {
            if (actualTypeArgument instanceof ParameterizedType) {
                // we only descend one level and only grab the first type class from there.
                // (we aren't set up to use any more than that currently...)
                ParameterizedType parameterizedTypeClass = (ParameterizedType) actualTypeArgument;
                typeNames.add(parameterizedTypeClass.getRawType().getTypeName());
                typeNames.add(parameterizedTypeClass.getActualTypeArguments()[0].getTypeName());
            } else {
                typeNames.add(actualTypeArgument.getTypeName());
            }
        }

        return typeNames.toArray(new String[0]);
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
            log.debug("Reviewing namespaceOverrideMap item key: {}, value {}",
                    item.getKey(), item.getValue());

            temp.put(item.getKey().endsWith(".") ? item.getKey() : item.getKey() + ".",
                    item.getValue().endsWith(".") ? item.getValue() : item.getValue() + ".");
        }

        this.namespaceOverrideMap = temp;
        return this;
    }

    /**
     * @param specifiedDependencies Override the default target project base search and only search these dependencies with this group id
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
     * @param ignoreStaticMembers boolean if we should ignore static member variables
     * @return an instance of the Processor object
     */
    public Processor withIgnoreStaticMembers(Boolean ignoreStaticMembers) {
        if (ignoreStaticMembers == null) {
            return this;
        }

        this.ignoreStaticMembers = ignoreStaticMembers;
        return this;
    }

    /**
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
        log.debug("Processor: Execution commencing...");
        List<Schema> processedSchemas = new ArrayList<>();
        log.debug("    Number of schemas to process: " + candidateSchemas.size());
        for (Schema schema : candidateSchemas) {
            log.debug("      Current schema: " + schema.getNamespace());
            this.currentSchema = schema;
            this.targetClasses = new HashSet<>();

            Schema builtSchema = buildSchema(schema);
            if (!builtSchema.isEmpty()) {
                // Only put schemas that have declarations in them.
                processedSchemas.add(builtSchema);
            }
        }

        if (validateSchemas) {
            log.debug("    Validating schemas");

            for (int i = 0; i < processedSchemas.size(); i++) {
                Schema validationSchema = processedSchemas.get(i);
                validator = new Validator().withSchema(validationSchema);
                validator.validateSchema();
                validationSchema.setIsValid(validator.getErrors().isEmpty());
                validationSchema.setValidator(validator);
                log.info("Validation result for schema: " + validationSchema.getName());
                log.info(validator.getErrorComments());
                processedSchemas.set(i, validationSchema);
            }
        }

        this.processedSchemas = processedSchemas;
        log.debug("Processor: Execution complete");
    }

    /**
     * @param schema a preconfigured schema
     * @return a completed schema
     * @throws MojoExecutionException when there is a MalformedURLException in the classpathElements
     */
    private Schema buildSchema(Schema schema) throws MojoExecutionException {
        log.debug("Start building schema: " + schema.getName());
        for (Class<? extends Annotation> source : sourceAnnotations) {
            if (artifactReference == null || artifactReference.getMavenProject() == null) {
                log.debug("MavenProject is null; falling back to built in class scanner");
                targetClasses.addAll(AnnotationParser.findAnnotatedClasses(source));
            } else {
                targetClasses.addAll(AnnotationParser.findAnnotatedClasses(artifactReference,
                        schema.getClasspathReferenceList(), source));
            }
        }

        log.debug("   Got target [{}] classes for schema: {}", targetClasses.size(), schema.getName());
        for (Class<?> targetClass : targetClasses) {
            log.debug("  Target class to use in schema: " + targetClass.getName());
        }

        if (targetClasses.size() < 1) {
            log.debug("  No target classes found; skipping schema creation");
            schema.isEmpty(true);
            return schema;
        }

        // The targetClasses includes ALL annotated classes including those inside dependencies
        int rootTypeCount = 0;
        for (Class<?> clazz : targetClasses) {
            if (isEnumWorkaround(clazz) && (consolidatedSchemas || !getExternalClassDefinitionDetails(clazz).locatedOutside)) {
                schema.addEnumDeclaration(buildEnumDeclaration(clazz));
                continue;
            }

            if (clazz instanceof Class) {
                TypeDeclaration temp = buildTypeDeclaration(schema, clazz);
                if (temp.isRoot()) {
                    log.debug("  Found root: " + temp.getName());
                    rootTypeCount++;
                    if (rootTypeCount > 1) {
                        throw new IllegalArgumentException("Only one rootType declaration is allowed");
                    }

                    schema.setRootType(temp.getName());
                }

                if (consolidatedSchemas || !getExternalClassDefinitionDetails(clazz).locatedOutside) {
                    schema.addTypeDeclaration(temp);
                } else {
                    // Don't get the inner stuff of this class if we're not consolidated.
                    continue;
                }

                // Now examine inner classes
                Class<?>[] innerClasses = clazz.getDeclaredClasses();
                for (Class<?> inner : innerClasses) {
                    log.debug("  Processing inner class: " + inner.getSimpleName());
                    if (inner.isSynthetic()) {
                        log.debug("  Found synthetic...");
                        continue;
                    }

                    if (isEnumWorkaround(inner)) {
                        log.debug("  Found enum...");
                        schema.addEnumDeclaration(buildEnumDeclaration(inner));
                        continue;
                    }

                    log.debug("  Found type...");
                    // Inner classes cannot be root type
                    schema.addTypeDeclaration(buildTypeDeclaration(schema, inner));
                }
            }
        }

        return schema;
    }

    /**
     * @param enumClass Class to test if it is an Enum
     * @return boolean
     */
    private boolean isEnumWorkaround(Class<?> enumClass) {
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
        log.debug("Building Enum: " + clazz.getName());

        EnumDeclaration enumD = new EnumDeclaration();
        enumD.setName(clazz.getSimpleName());

        Annotation annotation = clazz.getAnnotation(FlatBufferEnum.class);
        if (annotation instanceof FlatBufferEnum) {
            FlatBufferEnum myFlatBufferEnum = (FlatBufferEnum) annotation;
            log.debug("Enum structure: " + myFlatBufferEnum.value());
            enumD.setStructure(myFlatBufferEnum.value());
            log.debug("Enum type: " + myFlatBufferEnum.enumType());
            enumD.setType(myFlatBufferEnum.enumType());
        } else {
            log.debug("Not FlatBufferEnum (likely inner class); Generic enum created");
        }

        annotation = clazz.getAnnotation(FlatBufferComment.class);
        if (annotation != null) {
            String comment = ((FlatBufferComment) annotation).comment();
            if (!comment.isEmpty()) {
                log.debug("Found a comment assign to enum: " + comment);
                enumD.setComment(comment);
            }
        }

        Field[] fields = clazz.getDeclaredFields();

        // Find what field was annotated as the value we need to use for the declared type
        boolean setValues = false;
        Field valueField = null;
        int numFlatBufferEnumTypeFieldAnnotations = 0;
        if (fields != null && fields.length > 0) {
            log.debug("Enum with declared fields detected");
            for (Field field : fields) {
                log.debug("  Field: " + field.getName() + " type:" + field.getType().getSimpleName());
                if (field.getAnnotation(FlatBufferEnumTypeField.class) != null) {
                    log.debug("    Annotated field (aforementioned field)");

                    // Verify the declaration on the enum matches the declaration of the field
                    if (enumD.getType() == null) {
                        throw new IllegalArgumentException(
                                "Missing @FlatBufferEnum(enumType = EnumType.<SELECT>) declaration or remove @FlatBufferEnumTypeField for: "
                                        + clazz.getName());
                    }

                    if (field.getType().isAssignableFrom(enumD.getType().targetClass)) {
                        setValues = true;
                        valueField = field;
                    }

                    numFlatBufferEnumTypeFieldAnnotations++;
                }
            }
        }

        if (numFlatBufferEnumTypeFieldAnnotations > 1) {
            throw new IllegalArgumentException(
                    "Can only declare one @FlatBufferEnumTypeField for Enum: " + clazz.getName());
        }

        Object[] constants = clazz.getEnumConstants();

        // If we want the value then try and grab it
        for (Object constant : constants) {
            log.debug("Adding value to Enum: " + constant.toString());

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
                    log.error("Not allowed to grab Enum field value: ", e);
                }
            } else {
                // Otherwise, just use the name of the constant
                enumD.addValue(constant.toString());
            }
        }

        return enumD;
    }

    /**
     * @param schema The schema currently being considered while reviewing this class
     * @param clazz  Class which is being considered for an TypeDeclaration
     * @return a TypeDeclaration
     */
    public TypeDeclaration buildTypeDeclaration(Schema schema, Class<?> clazz) {
        log.debug("Building Type: " + clazz.getName());

        TypeDeclaration type = new TypeDeclaration();
        type.setName(clazz.getSimpleName());

        Annotation annotation = clazz.getAnnotation(FlatBufferTable.class);
        log.debug("Number of annotations of clazz: " + clazz.getDeclaredAnnotations().length);
        if (annotation instanceof FlatBufferTable) {
            FlatBufferTable myFlatBufferTable = (FlatBufferTable) annotation;
            log.debug("Declared root: " + myFlatBufferTable.rootType());
            type.setRoot(myFlatBufferTable.rootType());
            log.debug("Table structure: " + myFlatBufferTable.value());
            type.setStructure(myFlatBufferTable.value());
        } else {
            log.debug("Not FlatBufferTable (likely inner class); Generic table created");
        }

        annotation = clazz.getAnnotation(FlatBufferComment.class);
        if (annotation != null) {
            String comment = ((FlatBufferComment) annotation).comment();
            if (!comment.isEmpty()) {
                log.debug("Found a comment assign to type: " + comment);
                type.setComment(comment);
            }
        }

        // TODO: If we aren't separated schemas then check to see if the fields are a class which lives in a base.
        //  This will be challenging for things like a List or Map of base types
        List<Field> fields = getDeclaredAndInheritedPrivateFields(clazz);
        for (Field field : fields) {
            log.debug("Number of annotations found: " + field.getDeclaredAnnotations().length);

            if (field.getAnnotation(FlatBufferIgnore.class) != null) {
                log.debug("Ignoring property marked FlatBufferIgnore: " + field.getName());
                continue;
            }

            // Skip static fields in classes
            if (ignoreStaticMembers && Modifier.isStatic(field.getModifiers())) {
                log.debug("Ignoring property marked static: " + field.getName());
                continue;
            }

            log.debug("Adding property to Type: " + field.getName());
            type.addProperty(getPropertyForField(schema, field));
        }

        return type;
    }

    /**
     * @param type Class which needs to be traversed up to determine which fields are to be
     *             considered as candidates for declaration
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
     * @param schema The schema currently being considered while reviewing this field
     * @param field  A class field
     * @return A type declaration Property. This contains the name of the field and type
     * {@link FieldType}. When encountering an array or ident (Indentifier) the options
     * property is used to store additional information.
     */
    public Property getPropertyForField(Schema schema, final Field field) {
        Property property = new Property();

        // Some uses in which we reference other namespaces require us to declare the entirety of
        // the name
        Annotation annotation = field.getAnnotation(FlatBufferFieldOptions.class);
        boolean useFullName = false;
        String defaultValue = null;
        FieldType fieldTypeOverride = null;

        // Process overrides for the field
        if (annotation != null) {
            useFullName = ((FlatBufferFieldOptions) annotation).useFullName();
            defaultValue = ((FlatBufferFieldOptions) annotation).defaultValue();
            fieldTypeOverride = ((FlatBufferFieldOptions) annotation).fieldType();
        }

        // Apply a comment if the annotation exists
        annotation = field.getAnnotation(FlatBufferComment.class);
        if (annotation != null) {
            String comment = ((FlatBufferComment) annotation).comment();
            if (!comment.isEmpty()) {
                log.debug("Found a comment assign to field: " + comment);
                property.options.put(PropertyOptionKey.COMMENT, comment);
            }
        }

        // Apply the default value if it was set
        if (!StringUtils.isEmpty(defaultValue)) {
            log.debug("Found a default value to assign to field: " + defaultValue);
            property.options.put(PropertyOptionKey.DEFAULT_VALUE, defaultValue);
        }

        if (field.getType().isAssignableFrom(int.class) || field.getType().isAssignableFrom(Integer.class)) {
            property.name = field.getName();
            property.type = fieldTypeOverride == null ? FieldType.INT : fieldTypeOverride;
            return property;
        }

        if (field.getType().isAssignableFrom(String.class)) {
            property.name = field.getName();
            property.type = fieldTypeOverride == null ? FieldType.STRING : fieldTypeOverride;
            return property;
        }

        if (field.getType().isAssignableFrom(long.class) || field.getType().isAssignableFrom(Long.class)) {
            property.name = field.getName();
            property.type = fieldTypeOverride == null ? FieldType.LONG : fieldTypeOverride;
            return property;
        }

        if (field.getType().isAssignableFrom(short.class) || field.getType().isAssignableFrom(Short.class)) {
            property.name = field.getName();
            property.type = fieldTypeOverride == null ? FieldType.SHORT : fieldTypeOverride;
            return property;
        }

        if (field.getType().isAssignableFrom(boolean.class) || field.getType().isAssignableFrom(Boolean.class)) {
            property.name = field.getName();
            property.type = fieldTypeOverride == null ? FieldType.BOOL : fieldTypeOverride;
            return property;
        }

        if (field.getType().isAssignableFrom(byte.class) || field.getType().isAssignableFrom(Byte.class)) {
            property.name = field.getName();
            property.type = fieldTypeOverride == null ? FieldType.BYTE : fieldTypeOverride;
            return property;
        }

        if (field.getType().isAssignableFrom(float.class) || field.getType().isAssignableFrom(Float.class)) {
            property.name = field.getName();
            property.type = fieldTypeOverride == null ? FieldType.FLOAT : fieldTypeOverride;
            return property;
        }

        if (field.getType().isAssignableFrom(double.class) || field.getType().isAssignableFrom(Double.class)) {
            property.name = field.getName();
            property.type = fieldTypeOverride == null ? FieldType.DOUBLE : fieldTypeOverride;
            return property;
        }

        // EX: String[], SomeClass[]
        if (field.getType().isArray() && !field.getType().isAssignableFrom(List.class)) {
            return new ArrayProperties(this).process(property, field, useFullName);
        }

        // EX: List<String>, List<SomeClass>, and Sets<E>
        if (field.getType().isAssignableFrom(List.class) || field.getType().isAssignableFrom(Set.class)) {
            return new ListProperties(this).process(property, field, useFullName);
        }

        // EX: Map<String, Object>
        if (field.getType().isAssignableFrom(Map.class)) {
            return new MapProperties(this).process(property, field, useFullName);
        }

        // Anything else - enum, class, etc.
        return new ClassProperties(this).process(property, field, useFullName);
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
            log.debug(String
                    .format("Using full name; reviewing simpleName: %s and package: %s", simpleName,
                            packageName));

            if (namespaceOverrideMap != null && namespaceOverrideMap.containsKey(packageName)) {
                name = namespaceOverrideMap.get(packageName) + simpleName;
                log.debug("Override located; using it: " + name);
            }
        }

        if (Utilities.isPrimitiveOrWrapperType(clazz)) {
            log.debug("Array parameter is primitive, wrapper, or String: " + field.getName());
            name = Utilities.getPrimitiveNameForWrapperType(clazz);
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
        ExternalClassDefinition externalClassDefinition = new ExternalClassDefinition();

        if (currentSchema.isDependency()) {
            log.debug("This [{}] is a dependency. Therefor, skipping external class check for [{}]. This assumes that a base schema is the sum of its self and its dependencies.", currentSchema.getName(), clazz.getName());
            // Consider if we want to recursively add more schemas as we find them.
            return externalClassDefinition;
        }

        log.debug("Determining if class was defined outside this schema");
        final String currentClazzPackageName = clazz
                .getPackage()
                .getName();
        log.debug("  Class namespace for review: " + currentClazzPackageName);
        boolean classLocatedOutside = false;

        for (Schema oneOfSchemas : candidateSchemas) {
            log.debug(" Iterating schemas: " + oneOfSchemas.getNamespace());

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
                log.debug("  Found location of class which is outside current schema");
                externalClassDefinition.targetNamespace = oneOfSchemas.getNamespace();
                currentSchema.addInclude(oneOfSchemas.getName());

                classLocatedOutside = true;
                break;
            }
        }

        if (!classLocatedOutside) {
            log.debug("  Did not find class outside schema; continue processing");
        }

        externalClassDefinition.locatedOutside = classLocatedOutside;
        return externalClassDefinition;
    }

    /**
     * Enables multi-schemas support
     * <p>
     * Internal Processor class
     */
    @ToString
    public class ExternalClassDefinition {
        public Namespace targetNamespace;
        public boolean locatedOutside;
    }
}
