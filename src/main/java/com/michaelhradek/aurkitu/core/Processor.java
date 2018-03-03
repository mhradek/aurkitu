/**
 *
 */
package com.michaelhradek.aurkitu.core;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
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
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

/**
 * @author m.hradek
 */
@Getter
public class Processor {

    private List<Class<? extends Annotation>> sourceAnnotations;
    private Set<Class<?>> targetClasses;
    private MavenProject mavenProject;

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
     * @param mavenProject The Maven Project component
     * @return an instance of the Processor object
     */
    public Processor withMavenProject(MavenProject mavenProject) {
        this.mavenProject = mavenProject;
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
            if(mavenProject == null) {
                Application.getLogger().debug("MavenProject is null; falling back to built in class scanner");
                targetClasses.addAll(AnnotationParser.findAnnotatedClasses(source));
            } else {
                targetClasses.addAll(AnnotationParser.findAnnotatedClasses(mavenProject, source));
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
    TypeDeclaration.Property getPropertyForField(Field field) {
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

            ParameterizedType listType = (ParameterizedType) field.getGenericType();
            Class<?> listTypeClass = (Class<?>) listType.getActualTypeArguments()[0];

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
            if(mavenProject != null)
                identName = getClassForClassName(mavenProject, fieldType.getTypeName()).getSimpleName();
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
    Class<?> getClassForClassName(MavenProject mavenProject, String className) throws ClassNotFoundException, MalformedURLException, DependencyResolutionRequiredException {
        List<String> classpathElements;

        classpathElements = mavenProject.getCompileClasspathElements();
        List<URL> projectClasspathList = new ArrayList<URL>();
        for (String element : classpathElements) {
            Application.getLogger().debug("Considering compile classpath element (via MavenProject): " + element);
            projectClasspathList.add(new File(element).toURI().toURL());
        }

        URLClassLoader urlClassLoader = new URLClassLoader(projectClasspathList.toArray(new URL[]{}),
                Thread.currentThread().getContextClassLoader());

        return urlClassLoader.loadClass(className);
    }
}
