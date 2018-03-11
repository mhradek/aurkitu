/**
 *
 */
package com.michaelhradek.aurkitu.core;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.michaelhradek.aurkitu.Application;
import com.michaelhradek.aurkitu.annotations.FlatBufferEnum;
import com.michaelhradek.aurkitu.annotations.FlatBufferEnumTypeField;
import com.michaelhradek.aurkitu.annotations.FlatBufferIgnore;
import com.michaelhradek.aurkitu.annotations.FlatBufferTable;
import com.michaelhradek.aurkitu.core.output.EnumDeclaration;
import com.michaelhradek.aurkitu.core.output.FieldType;
import com.michaelhradek.aurkitu.core.output.Schema;
import com.michaelhradek.aurkitu.core.output.TypeDeclaration;

import lombok.Getter;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResult;

/**
 * @author m.hradek
 */
@Getter
public class Processor {

    private List<Class<? extends Annotation>> sourceAnnotations;
    private Set<Class<?>> targetClasses;
    private ArtifactReference artifactReference;

    public Processor() {
        sourceAnnotations = new ArrayList<Class<? extends Annotation>>();
        targetClasses = new HashSet<Class<?>>();
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
     *
     * @return a completed schema
     * @throws MojoExecutionException when there is a MalformedURLException in the classpathElements
     */
    public Schema buildSchema() throws MojoExecutionException {
        Schema schema = new Schema();

        for (Class<? extends Annotation> source : sourceAnnotations) {
            if(artifactReference == null || artifactReference.getMavenProject() == null) {
                Application.getLogger().debug("MavenProject is null; falling back to built in class scanner");
                targetClasses.addAll(AnnotationParser.findAnnotatedClasses(source));
            } else {
                targetClasses.addAll(AnnotationParser.findAnnotatedClasses(artifactReference.getMavenProject(), source));
            }
        }

        int rootTypeCount = 0;
        for (Class<?> clazz : targetClasses) {
            if (isEnumWorkaround(clazz)) {
                schema.addEnumDeclaration(buildEnumDeclaration(clazz));
                continue;
            }

            if (clazz instanceof Class) {
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
        }

        return schema;
    }

    /**
     *
     * @param enumClass Class to test if it is an Enum
     * @return boolean
     */
    boolean isEnumWorkaround(Class<?> enumClass) {
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
            if (myFlatBufferEnum.enumType() != FieldType.STRING) {
                enumD.setType(myFlatBufferEnum.enumType());
            }
        } else {
            Application.getLogger()
                    .debug("Not FlatBufferEnum (likely inner class); Generic enum created");
        }

        Field[] fields = clazz.getDeclaredFields();

        // Find what field was annotated as the value we need to use for the declared type
        boolean setValues = false;
        Field valueField = null;
        int numAnnotations = 0;
        if (fields != null && fields.length > 0) {
            Application.getLogger().debug("Enum with declared fields detected");
            for (Field field : fields) {
                Application.getLogger()
                        .debug("  Field: " + field.getName() + " type:" + field.getType().getSimpleName());
                if (field.getAnnotation(FlatBufferEnumTypeField.class) != null) {
                    Application.getLogger().debug("    Annotated field");

                    // Verify the declaration on the enum matches the declaration of the field
                    if (field.getType().isAssignableFrom(enumD.getType().targetClass)) {
                        setValues = true;
                        valueField = field;
                        numAnnotations++;
                    }
                }
            }
        }

        if (numAnnotations > 1) {
            throw new IllegalArgumentException("Can only declare one @FlatBufferEnumTypeField for Enum");
        }

        Object[] constants = clazz.getEnumConstants();

        // If we want the value then try and grab it
        for (Object constant : constants) {
            Application.getLogger().debug("Adding value to Enum: " + constant.toString());

            if (setValues) {
                valueField.setAccessible(true);
                try {
                    final String temp = constant.toString() + " = ";

                    if (enumD.getType() == FieldType.BYTE || enumD.getType() == FieldType.UBYTE) {
                        enumD.addValue(temp + valueField.getByte(constant));
                        continue;
                    }

                    if (enumD.getType() == FieldType.SHORT || enumD.getType() == FieldType.USHORT) {
                        enumD.addValue(temp + valueField.getShort(constant));
                        continue;
                    }

                    if (enumD.getType() == FieldType.LONG || enumD.getType() == FieldType.ULONG) {
                        enumD.addValue(temp + valueField.getLong(constant));
                        continue;
                    }

                    if (enumD.getType() == FieldType.INT || enumD.getType() == FieldType.UINT) {
                        enumD.addValue(temp + valueField.getInt(constant));
                        continue;
                    }

                    if (enumD.getType() == FieldType.FLOAT) {
                        enumD.addValue(temp + valueField.getFloat(constant));
                        continue;
                    }

                    if (enumD.getType() == FieldType.DOUBLE) {
                        enumD.addValue(temp + valueField.getDouble(constant));
                        continue;
                    }

                    enumD.addValue(temp + valueField.get(constant));
                } catch (Exception e) {
                    Application.getLogger().error("Error attempting to grab Enum field value", e);
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
            Application.getLogger()
                    .debug("Not FlatBufferTable (likely inner class); Generic table created");
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
     * @param type Class which needs to be travered up to determine which fields are to be considered as candidates for declaration
     * @return A list of valid fields
     */
    List<Field> getDeclaredAndInheritedPrivateFields(Class<?> type) {
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
     * @return A type declaration Property. This contains the name of the field and type {@link FieldType}. When
     * encountering an array or ident (Indentifier) the options property is used to store additional information.
     */
    TypeDeclaration.Property getPropertyForField(final Field field) {
        TypeDeclaration.Property property = new TypeDeclaration.Property();

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

        if (field.getType().isAssignableFrom(List.class)) {
            property.name = field.getName();
            property.type = FieldType.ARRAY;

            List<String> classpathElements;
            Class<?> listTypeClass;

            try {
                // Load build class path
                classpathElements = artifactReference.getMavenProject().getCompileClasspathElements();
                List<URL> projectClasspathList = new ArrayList<URL>();
                for (String element : classpathElements) {
                    Application.getLogger().debug("Looking at compile classpath element (via MavenProject): " + element);
                    projectClasspathList.add(new File(element).toURI().toURL());
                }

                // Load artifact(s) jars using resolver
                Application.getLogger().debug("Number of artifacts to resolve: "
                        + artifactReference.getMavenProject().getDependencyArtifacts().size());
                for (Artifact unresolvedArtifact : artifactReference.getMavenProject().getDependencyArtifacts()) {
                    String artifactId = unresolvedArtifact.getArtifactId();
                    org.eclipse.aether.artifact.Artifact aetherArtifact = new DefaultArtifact(
                            unresolvedArtifact.getGroupId(),
                            unresolvedArtifact.getArtifactId(),
                            unresolvedArtifact.getClassifier(),
                            unresolvedArtifact.getType(),
                            unresolvedArtifact.getVersion());

                    ArtifactRequest artifactRequest = new ArtifactRequest()
                            .setRepositories(artifactReference.getRepositories())
                            .setArtifact(aetherArtifact);
                    ArtifactResult resolutionResult = artifactReference.getRepoSystem()
                            .resolveArtifact(artifactReference.getRepoSession(), artifactRequest);

                    // The file should exists, but we never know.
                    File file = resolutionResult.getArtifact().getFile();
                    if (file == null || !file.exists()) {
                        Application.getLogger().warn("Artifact " + artifactId +
                                " has no attached file. Its content will not be copied in the target model directory.");
                        continue;
                    }

                    String jarPath = "jar:file:" + file.getAbsolutePath() + "!/";
                    Application.getLogger().debug("Adding resolved artifact: " + file.getAbsolutePath());
                    projectClasspathList.add(new URL(jarPath));
                }

                // Load all paths into custom classloader
                ClassLoader urlClassLoader = URLClassLoader.newInstance(projectClasspathList.toArray(new URL[]{}),
                        Thread.currentThread().getContextClassLoader());

                // Parse Field signature
                String parametrizedTypeString = parseFieldSignatureForParametrizedTypeString(field);
                listTypeClass = urlClassLoader.loadClass(parametrizedTypeString);
            } catch (Exception e) {
                Application.getLogger().debug("Unable to find and load class for List<?> parameter", e);
                listTypeClass = String.class;
            }

            String name = listTypeClass.getSimpleName();
            if (Utilities.isLowerCaseType(listTypeClass)) {
                Application.getLogger()
                        .debug("Array parameter is primative, wrapper, or String: " + field.getName());
                name = name.toLowerCase();
            }

            property.options.put(FieldType.ARRAY.toString(), name);
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

        String name = field.getName();
        Application.getLogger().debug("Found unrecognized type; assuming Type.IDENT(IFIER): " + name);
        property.name = name;
        property.type = FieldType.IDENT;

        Type fieldType = field.getGenericType();
        String identName = fieldType.getTypeName();
        try {
            if(artifactReference != null && artifactReference.getMavenProject() != null)
                identName = getClassForClassName(artifactReference.getMavenProject(), fieldType.getTypeName()).getSimpleName();
            else
                identName = Thread.currentThread().getContextClassLoader().loadClass(fieldType.getTypeName()).getSimpleName();
            //identName = Class.forName(fieldType.getTypeName(), false,Thread.currentThread().getContextClassLoader()).getSimpleName();
        } catch (Exception e) {
            Application.getLogger().warn("Unable to get class for name: " + fieldType.getTypeName(), e);
            identName = fieldType.getTypeName().substring(fieldType.getTypeName().lastIndexOf("."));
            identName = identName.substring(identName.lastIndexOf("$"));
            Application.getLogger().debug("Trimmed: " + fieldType.getTypeName() + " to " + identName);
        }

        property.options.put(FieldType.IDENT.toString(), identName);
        return property;
    }

    /**
     *
     * @param className The name of the class we need to locate
     * @return The class we located
     * @throws ClassNotFoundException if the class cannot be located
     * @throws MalformedURLException if one of the classpathElements are a malformed URL
     * @throws DependencyResolutionRequiredException if MavenProject is unable to resolve the compiled classpath elements
     */
    Class<?> getClassForClassName(MavenProject mavenProject, String className) throws ClassNotFoundException,
            MalformedURLException, DependencyResolutionRequiredException, IOException {
        List<String> classpathElements;

        classpathElements = mavenProject.getCompileClasspathElements();
        List<URL> projectClasspathList = new ArrayList<URL>();
        for (String element : classpathElements) {
            Application.getLogger().debug("Adding compiled classpath element (via MavenProject): " + element);
            projectClasspathList.add(new File(element).toURI().toURL());
        }

        URLClassLoader urlClassLoader = new URLClassLoader(projectClasspathList.toArray(new URL[]{}),
                Thread.currentThread().getContextClassLoader());

        Class<?> result = urlClassLoader.loadClass(className);
        urlClassLoader.close();
        return result;
    }

    /**
     * Ljava/util/List<Lcom/company/team/service/model/Person;>;
     *
     * @param input
     * @return
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     */
    String parseFieldSignatureForParametrizedTypeString(Field input) throws NoSuchFieldException, IllegalAccessException {
        Field privateField = input.getClass().getDeclaredField("signature");
        privateField.setAccessible(true);
        String signature = (String) privateField.get(input);
        Application.getLogger().debug("Examining signature: " + signature);

        String typeSignature = signature.substring(signature.indexOf("<") + 1, signature.indexOf(">"));
        typeSignature = typeSignature.replaceFirst("[a-zA-Z]{1}","");
        typeSignature = typeSignature.replaceAll("/", ".");
        typeSignature = typeSignature.replaceAll(";", "");
        Application.getLogger().debug("Derived class: " + typeSignature);

        return typeSignature;
    }

    /**
     *
     * @param classLoaderToSwitchTo
     * @param actionToPerformOnProvidedClassLoader
     * @param <T>
     * @return
     */
    public static synchronized  <T> T executeActionOnSpecifiedClassLoader(
            final ClassLoader classLoaderToSwitchTo,
            final ExecutableAction<T> actionToPerformOnProvidedClassLoader) {

        Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
        final ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();

        try {
            Application.getLogger().debug("THREAD COUNT: " + Thread.activeCount());
            Thread.currentThread().setContextClassLoader(classLoaderToSwitchTo);
            for(URL url : ((URLClassLoader) (Thread.currentThread().getContextClassLoader())).getURLs()) {
                Application.getLogger().debug("Classloader loaded with: " + url.toString());
            }

            return actionToPerformOnProvidedClassLoader.run();
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }

    /**
     * Encapsulates action to be executed.
     *
     */
    public interface ExecutableAction<T> {
        /**
         * Execute the operation.
         *
         * @return Optional value returned by this operation;
         *    implementations should document what, if anything,
         *    is returned by implementations of this method.
         */
        T run();
    }
}
