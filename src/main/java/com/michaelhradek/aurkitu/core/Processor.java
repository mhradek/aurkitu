/**
 * 
 */
package com.michaelhradek.aurkitu.core;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
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

/**
 * @author m.hradek
 * @date May 22, 2017
 * 
 */
@Getter
public class Processor {

  private List<Class<? extends Annotation>> sourceAnnotations;
  private Set<Class<?>> targetClasses;

  public Processor() {
    sourceAnnotations = new ArrayList<Class<? extends Annotation>>();
    targetClasses = new HashSet<Class<?>>();
  }

  /**
   * 
   * @param targetAnnotation
   * @return
   */
  public Processor withSourceAnnotation(Class<? extends Annotation> targetAnnotation) {
    sourceAnnotations.add(targetAnnotation);
    return this;
  }

  /**
   * 
   * @return
   */
  public Schema buildSchema() {
    Schema schema = new Schema();

    for (Class<? extends Annotation> source : sourceAnnotations) {
      targetClasses.addAll(AnnotationParser.findAnnotatedClasses(source));
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

  boolean isEnumWorkaround(Class<?> enumClass) {
    if (enumClass.isAnonymousClass()) {
      enumClass = enumClass.getSuperclass();
    }

    return enumClass.isEnum();
  }

  /**
   * 
   * @param clazz
   * @return
   */
  EnumDeclaration buildEnumDeclaration(Class<?> clazz) {
    Application.getLogger().debug("Building Enum: " + clazz.getName());

    EnumDeclaration enumD = new EnumDeclaration();
    enumD.setName(clazz.getSimpleName());

    Annotation annotation = clazz.getAnnotation(FlatBufferEnum.class);
    FlatBufferEnum myAnnotation = (FlatBufferEnum) annotation;
    if (annotation instanceof FlatBufferEnum) {
      Application.getLogger().debug("Enum structure: " + myAnnotation.value());
      enumD.setStructure(myAnnotation.value());
      Application.getLogger().debug("Enum type: " + myAnnotation.enumType());
      if (myAnnotation.enumType() != FieldType.STRING) {
        enumD.setType(myAnnotation.enumType());
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
   * 
   * @param clazz
   * @return
   */
  TypeDeclaration buildTypeDeclaration(Class<?> clazz) {
    Application.getLogger().debug("Building Type: " + clazz.getName());

    TypeDeclaration type = new TypeDeclaration();
    type.setName(clazz.getSimpleName());

    Annotation annotation = clazz.getAnnotation(FlatBufferTable.class);
    FlatBufferTable myAnnotation = (FlatBufferTable) annotation;
    if (annotation instanceof FlatBufferTable) {
      Application.getLogger().debug("Declared root: " + myAnnotation.rootType());
      type.setRoot(myAnnotation.rootType());
      Application.getLogger().debug("Table structure: " + myAnnotation.value());
      type.setStructure(myAnnotation.value());
    } else {
      Application.getLogger()
          .debug("Not FlatBufferTable (likely inner class); Generic table created");
    }

    List<Field> fields = getDeclaredAndInheritedPrivateFields(clazz);
    for (Field field : fields) {
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
   * 
   * @param type
   * @return
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
   * 
   * @param field
   * @return
   */
  TypeDeclaration.Property getPropertyForField(Field field) {
    TypeDeclaration.Property property = new TypeDeclaration.Property();

    if (field.getType().isAssignableFrom(int.class)) {
      property.name = field.getName();
      property.type = FieldType.INT;
      return property;
    }

    if (field.getType().isAssignableFrom(String.class)) {
      property.name = field.getName();
      property.type = FieldType.STRING;
      return property;
    }

    if (field.getType().isAssignableFrom(long.class)) {
      property.name = field.getName();
      property.type = FieldType.LONG;
      return property;
    }

    if (field.getType().isAssignableFrom(short.class)) {
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
      if (isLowerCaseType(listTypeClass)) {
        Application.getLogger()
            .debug("Array parameter is primative, wrapper, or String: " + field.getName());
        name = name.toLowerCase();
      }

      property.options.put(FieldType.ARRAY.toString(), name);
      return property;
    }

    if (field.getType().isAssignableFrom(boolean.class)) {
      property.name = field.getName();
      property.type = FieldType.BOOL;
      return property;
    }

    if (field.getType().isAssignableFrom(byte.class)) {
      property.name = field.getName();
      property.type = FieldType.BYTE;
      return property;
    }

    if (field.getType().isAssignableFrom(float.class)) {
      property.name = field.getName();
      property.type = FieldType.FLOAT;
      return property;
    }

    String name = field.getName();
    Application.getLogger().debug("Found unrecognized type; assuming Type.IDENT(IFIER): " + name);
    property.name = name;
    property.type = FieldType.IDENT;

    Type fieldType = field.getGenericType();
    String identName = fieldType.getTypeName();
    try {
      identName = Class.forName(fieldType.getTypeName()).getSimpleName();
    } catch (ClassNotFoundException e) {
      Application.getLogger().error("Unable to get class for name: " + fieldType.getTypeName(), e);
      identName = fieldType.getTypeName().substring(fieldType.getTypeName().lastIndexOf("."));
      identName = identName.substring(identName.lastIndexOf("$"));
      Application.getLogger().debug("Trimmed: " + fieldType.getTypeName() + " to " + identName);
    }

    property.options.put(FieldType.IDENT.toString(), identName);
    return property;
  }

  /**
   * 
   * @param type
   * @return
   */
  private boolean isLowerCaseType(Class<?> type) {
    return (type.isPrimitive() && type != void.class) || type == Double.class || type == Float.class
        || type == Long.class || type == Integer.class || type == Short.class
        || type == Character.class || type == Byte.class || type == Boolean.class
        || type == String.class;
  }
}
