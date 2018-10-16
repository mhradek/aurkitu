package com.michaelhradek.aurkitu.core;

import com.michaelhradek.aurkitu.Application;
import com.michaelhradek.aurkitu.annotations.*;
import com.michaelhradek.aurkitu.annotations.types.EnumType;
import com.michaelhradek.aurkitu.annotations.types.FieldType;
import com.michaelhradek.aurkitu.core.output.EnumDeclaration;
import com.michaelhradek.aurkitu.core.output.Schema;
import com.michaelhradek.aurkitu.core.output.TypeDeclaration;
import com.michaelhradek.aurkitu.core.output.TypeDeclaration.Property;
import com.michaelhradek.aurkitu.core.output.TypeDeclaration.Property.PropertyOptionKey;
import lombok.Getter;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.Map.Entry;

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
    private boolean consolidatedSchemas;
    private Schema schema;
    private Map<String, Schema> depedencySchemas;

    public Processor() {
        sourceAnnotations = new ArrayList<Class<? extends Annotation>>();
        targetClasses = new HashSet<Class<?>>();
        warnedTypeNames = new HashSet<String>();

        // This could be null as the value via Application could be overriden here
        namespaceOverrideMap = new HashMap<String, String>();
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
        Map<String, String> temp = new HashMap<String, String>();
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
     * @param consolidatedSchemas
     * @return
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
     *  @return a completed schema
     * @throws MojoExecutionException when there is a MalformedURLException in the classpathElements
     */
    public Schema buildSchema() throws MojoExecutionException {
        return buildSchema(new Schema());
    }

    /**
     * @param schema a preconfigured schema
     * @return a completed schema
     * @throws MojoExecutionException when there is a MalformedURLException in the classpathElements
     */
    public Schema buildSchema(Schema schema) throws MojoExecutionException {
        this.schema = schema;

        for (Class<? extends Annotation> source : sourceAnnotations) {
            if (artifactReference == null || artifactReference.getMavenProject() == null) {
                Application.getLogger().debug("MavenProject is null; falling back to built in class scanner");
                targetClasses.addAll(AnnotationParser.findAnnotatedClasses(source));
            } else {
                targetClasses.addAll(AnnotationParser.findAnnotatedClasses(artifactReference, source));
            }
        }

        // The targetClasses includes ALL annotated classes including those inside dependnecies
        int rootTypeCount = 0;
        for (Class<?> clazz : targetClasses) {
            if (isEnumWorkaround(clazz)) {
                schema.addEnumDeclaration(buildEnumDeclaration(clazz));
                continue;
            }

            TypeDeclaration temp = buildTypeDeclaration(clazz);
            if (temp.isRoot()) {
                Application.getLogger().debug("  Found root: " + temp.getName());
                rootTypeCount++;
                if (rootTypeCount > 1) {
                    throw new IllegalArgumentException("Only one rootType declaration is allowed");
                }

                schema.setRootType(temp.getName());
            }

            schema.addTypeDeclaration(temp);

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
                schema.addTypeDeclaration(buildTypeDeclaration(inner));
            }
        }

        return schema;
    }

    /**
     *
     * @param enumClass Class to test if it is an Enum
     * @return boolean
     */
    private boolean isEnumWorkaround(Class<?> enumClass) {
        if (enumClass.isAnonymousClass()) {
            enumClass = enumClass.getSuperclass();
        }

        return enumClass.isEnum();
    }

    /**
     * @param clazz Class which is being considered for an EnumDeclaration
     * @return an EnumDeclaration
     */
    EnumDeclaration buildEnumDeclaration(Class<?> clazz) {
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
     * @param clazz Class which is being considered for an TypeDeclaration
     * @return a TypeDeclaration
     */
    TypeDeclaration buildTypeDeclaration(Class<?> clazz) {
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
            if (comment != null && !comment.isEmpty()) {
                Application.getLogger().debug("Found a comment assign to type: " + comment);
                type.setComment(comment);
            }
        }

        List<Field> fields = getDeclaredAndInheritedPrivateFields(clazz);
        for (Field field : fields) {
            Application.getLogger().debug("Number of annotations found: " + field.getDeclaredAnnotations().length);

            if (field.getAnnotation(FlatBufferIgnore.class) != null) {
                Application.getLogger().debug("Ignoring property: " + field.getName());
                continue;
            }

            type.addProperty(getPropertyForField(field));
            Application.getLogger().debug("Adding property to Type: " + field.getName());
        }

        return type;
    }

    /**
     * @param type Class which needs to be travered up to determine which fields are to be
     *        considered as candidates for declaration
     * @return A list of valid fields
     */
    private List<Field> getDeclaredAndInheritedPrivateFields(Class<?> type) {
        List<Field> result = new ArrayList<Field>();

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
     * @param field A class field
     * @return A type declaration Property. This contains the name of the field and type
     *         {@link FieldType}. When encountering an array or ident (Indentifier) the options
     *         property is used to store additional information.
     */
    Property getPropertyForField(final Field field) {
        Property property = new Property();

        // Some uses in which we reference other namespaces require us to declare the entirety of
        // the name
        Annotation annotation = field.getAnnotation(FlatBufferFieldOptions.class);
        boolean useFullName = false;
        String defaultValue = null;

        if (annotation != null) {
            useFullName = ((FlatBufferFieldOptions) annotation).useFullName();
            defaultValue = ((FlatBufferFieldOptions) annotation).defaultValue();
        }

        annotation = field.getAnnotation(FlatBufferComment.class);
        if (annotation != null) {
            String comment = ((FlatBufferComment) annotation).comment();
            if (!comment.isEmpty()) {
                Application.getLogger().debug("Found a comment assign to field: " + comment);
                property.options.put(PropertyOptionKey.COMMENT, comment);
            }
        }

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
            property.name = field.getName();
            property.type = FieldType.ARRAY;

            // Determine type of the array
            String name = field.getType().getComponentType().getSimpleName();
            if (Utilities.isLowerCaseType(field.getType().getComponentType())) {
                Application.getLogger().debug("Array parameter is primative, wrapper, or String: " + field.getName());
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

        // EX: Map<String, Object>
        if (field.getType().isAssignableFrom(Map.class)) {
            property.name = field.getName();
            property.type = FieldType.MAP;

            // Stuff the types into this list
            List<Property> properties = new ArrayList<Property>();

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
                            .newInstance(Utilities.buildProjectClasspathList(artifactReference), urlClassLoader);
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

                String name = mapTypeClass.getSimpleName();
                if (useFullName) {
                    name = mapTypeClass.getName();

                    String simpleName = mapTypeClass.getName()
                        .substring(mapTypeClass.getName().lastIndexOf(".") + 1);
                    String packageName = mapTypeClass.getName()
                        .substring(0, mapTypeClass.getName().lastIndexOf(".") + 1);
                    Application.getLogger().debug(String
                        .format("Using full name; reviewing simpleName: %s and package: %s", simpleName,
                            packageName));

                    if (namespaceOverrideMap != null && namespaceOverrideMap.containsKey(packageName)) {
                        name = namespaceOverrideMap.get(packageName) + simpleName;
                        Application.getLogger().debug("Override located; using it: " + name);
                    }
                }

                if (Utilities.isLowerCaseType(mapTypeClass)) {
                    Application.getLogger()
                        .debug("Array parameter is primative, wrapper, or String: " + field.getName());
                    name = name.toLowerCase();
                }

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

        // EX: List<String>, List<SomeClass>
        if (field.getType().isAssignableFrom(List.class)) {
            property.name = field.getName();
            property.type = FieldType.ARRAY;

            Class<?> listTypeClass;

            try {
                // Load all paths into custom classloader
                ClassLoader urlClassLoader = Thread.currentThread().getContextClassLoader();
                if (artifactReference != null && artifactReference.getMavenProject() != null)
                    urlClassLoader = URLClassLoader.newInstance(Utilities.buildProjectClasspathList(artifactReference), urlClassLoader);

                // Parse Field signature
                String parametrizedTypeString = parseFieldSignatureForParametrizedTypeStringOnList(field);
                listTypeClass = urlClassLoader.loadClass(parametrizedTypeString);
            } catch (Exception e) {
                Application.getLogger().warn("Unable to find and load class for List<?> parameter, using String instead: ", e);
                listTypeClass = String.class;
            }

            String name = listTypeClass.getSimpleName();
            if (useFullName) {
                name = listTypeClass.getName();

                String simpleName = listTypeClass.getName().substring(listTypeClass.getName().lastIndexOf(".") + 1);
                String packageName = listTypeClass.getName().substring(0, listTypeClass.getName().lastIndexOf(".") + 1);
                Application.getLogger().debug(String
                    .format("Using full name; reviewing simpleName: %s and package: %s", simpleName, packageName));

                if (namespaceOverrideMap != null && namespaceOverrideMap.containsKey(packageName)) {
                    name = namespaceOverrideMap.get(packageName) + simpleName;
                    Application.getLogger().debug("Override located; using it: " + name);
                }
            }

            if (Utilities.isLowerCaseType(listTypeClass)) {
                Application.getLogger().debug("Array parameter is primative, wrapper, or String: " + field.getName());
                name = name.toLowerCase();
            }

            property.options.put(PropertyOptionKey.ARRAY, name);
            return property;
        }

        // Anything else
        String name = field.getName();
        Application.getLogger().debug("Found unrecognized type; assuming Type.IDENT(IFIER): " + name);
        property.name = name;
        property.type = FieldType.IDENT;

        Type fieldType = field.getGenericType();
        String identName;

        try {
            if (artifactReference != null && artifactReference.getMavenProject() != null) {
                Class<?> clazz = getClassForClassName(artifactReference.getMavenProject(), fieldType.getTypeName());
                identName = useFullName ? clazz.getName() : clazz.getSimpleName();

                if (useFullName) {
                    String simpleName = clazz.getName().substring(clazz.getName().lastIndexOf(".") + 1);
                    String packageName = clazz.getName().substring(0, clazz.getName().lastIndexOf(".") + 1);
                    if (namespaceOverrideMap != null && namespaceOverrideMap.containsKey(packageName)) {
                        identName = namespaceOverrideMap.get(packageName) + simpleName;
                        Application.getLogger().debug("Override located; using it: " + identName);
                    }
                }
            } else {
                Class<?> clazz = Thread.currentThread().getContextClassLoader().loadClass(fieldType.getTypeName());
                identName = useFullName ? clazz.getName() : clazz.getSimpleName();

                if (useFullName) {
                    String simpleName = clazz.getName().substring(clazz.getName().lastIndexOf(".") + 1);
                    String packageName = clazz.getName().substring(0, clazz.getName().lastIndexOf(".") + 1);
                    if (namespaceOverrideMap != null && namespaceOverrideMap.containsKey(packageName)) {
                        identName = namespaceOverrideMap.get(packageName) + simpleName;
                        Application.getLogger().debug("Override located; using it: " + identName);
                    }
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
     *
     * @param className The name of the class we need to locate
     * @return The class we located
     * @throws ClassNotFoundException if the class cannot be located
     * @throws IOException if one of the classpathElements are a malformed URL
     * @throws DependencyResolutionRequiredException if MavenProject is unable to resolve the
     *         compiled classpath elements
     */
    static Class<?> getClassForClassName(MavenProject mavenProject, String className)
        throws ClassNotFoundException, DependencyResolutionRequiredException, IOException {

        List<String> classpathElements = mavenProject.getCompileClasspathElements();
        List<URL> projectClasspathList = new ArrayList<URL>();
        for (String element : classpathElements) {
            Application.getLogger().debug("Adding compiled classpath element (via MavenProject): " + element);
            projectClasspathList.add(new File(element).toURI().toURL());
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
     *         Ljava/util/List<Lcom/company/team/service/model/Person;>;
     * @throws NoSuchFieldException if the Field does not exist in the class
     * @throws IllegalAccessException if the Field is inaccessible
     */
    static String parseFieldSignatureForParametrizedTypeStringOnList(Field input)
        throws NoSuchFieldException, IllegalAccessException {
        Field privateField = input.getClass().getDeclaredField("signature");
        privateField.setAccessible(true);
        String signature = (String) privateField.get(input);
        Application.getLogger().debug("Examining signature: " + signature);

        String typeSignature = signature.substring(signature.indexOf("<") + 1, signature.indexOf(">"));
        typeSignature = typeSignature.replaceFirst("[a-zA-Z]{1}", "");
        typeSignature = typeSignature.replaceAll("/", ".");
        typeSignature = typeSignature.replaceAll(";", "");
        Application.getLogger().debug("Derived class: " + typeSignature);

        return typeSignature;
    }

    /**
     * @param input The Map with a set of type declarations
     * @return a list of String which parses {"java.lang.String", "java.lang.Object"} from the following:
     * Ljava/util/Map<Ljava/lang/String;Ljava/lang/Object;>;
     * @throws NoSuchFieldException if the Field does not exist in the class
     * @throws IllegalAccessException if the Field is inaccessible
     */
    static String[] parseFieldSignatureForParametrizedTypeStringsOnMap(Field input)
        throws NoSuchFieldException, IllegalAccessException {
        Field privateField = input.getClass().getDeclaredField("signature");
        privateField.setAccessible(true);
        String signature = (String) privateField.get(input);
        Application.getLogger().debug("Examining signature: " + signature);

        String typeSignature = signature.substring(signature.indexOf("<") + 1, signature.indexOf(">"));
        typeSignature = typeSignature.replaceAll("/", ".");
        String[] listOfTypes = typeSignature.split(";");
        for (int i = 0; i < listOfTypes.length; i++) {
            listOfTypes[i] = listOfTypes[i].replaceAll(";", "");
            listOfTypes[i] = listOfTypes[i].replaceFirst("[a-zA-Z]{1}", "");
            Application.getLogger().debug(String.format("Derived class #%d: %s", i, listOfTypes[i]));
        }

        return listOfTypes;
    }
}
