/**
 * 
 */
package com.michaelhradek.aurkitu.core;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.michaelhradek.aurkitu.Config;
import com.michaelhradek.aurkitu.annotations.FlatBufferEnum;
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

  private Logger logger = Config.getLogger(getClass());

  private List<Class<? extends Annotation>> sources;
  private Set<Class<?>> classes;

  Processor() {
    sources = new ArrayList<Class<? extends Annotation>>();
    classes = new HashSet<Class<?>>();
  }

  /**
   * 
   * @param targetAnnotation
   * @return
   */
  public Processor withSource(Class<? extends Annotation> targetAnnotation) {
    sources.add(targetAnnotation);
    return this;
  }

  /**
   * 
   * @return
   */
  public Schema buildSchema() {
    Schema schema = new Schema();

    for (Class<? extends Annotation> source : sources) {
      classes.addAll(AnnotationParser.findAnnotatedClasses(source));
    }

    for (Class<?> clazz : classes) {
      if (clazz.isEnum()) {
        schema.addEnumDeclaration(buildEnumDeclaration(clazz));
        continue;
      }

      if (clazz instanceof Class) {
        TypeDeclaration temp = buildTypeDeclaration(clazz);
        if (temp.isRoot()) {
          schema.setRootType(temp.getName());
        }

        schema.addTypeDeclaration(temp);
        continue;
      }
    }

    return schema;
  }

  /**
   * 
   * @param clazz
   * @return
   */
  EnumDeclaration buildEnumDeclaration(Class<?> clazz) {
    logger.log(Level.FINE, "Building Enum: " + clazz.getName());

    EnumDeclaration enumD = new EnumDeclaration();
    enumD.setName(clazz.getSimpleName());

    Annotation annotation = clazz.getAnnotation(FlatBufferEnum.class);
    FlatBufferEnum myAnnotation = (FlatBufferEnum) annotation;
    if (annotation instanceof FlatBufferEnum) {
      logger.log(Level.FINE, "Enum structure: " + myAnnotation.value());
      enumD.setStructure(myAnnotation.value());
      logger.log(Level.FINE, "Enum type: " + myAnnotation.enumType());
      if (myAnnotation.enumType() != FieldType.STRING) {
        enumD.setType(myAnnotation.enumType());
      }
    } else {
      logger.log(Level.FINE, "Not FlatBufferEnum (likely inner class); Generic enum created");
    }

    Object[] constants = clazz.getEnumConstants();
    for (Object constant : constants) {
      logger.log(Level.FINE, "Adding value to Enum: " + constant.toString());
      enumD.addValue(constant.toString());

      /**
       * Field[] fields = constant.getClass().getDeclaredFields(); for (Field field : fields) { if
       * (!field.isEnumConstant()) { field.setAccessible(true); try { Object value =
       * field.get(clazz); System.out.println(value); } catch (Exception e) { e.printStackTrace(); }
       * } } /
       **/
    }

    return enumD;
  }

  /**
   * 
   * @param clazz
   * @return
   */
  TypeDeclaration buildTypeDeclaration(Class<?> clazz) {
    logger.log(Level.FINE, "Building Type: " + clazz.getName());

    TypeDeclaration type = new TypeDeclaration();
    type.setName(clazz.getSimpleName());

    Annotation annotation = clazz.getAnnotation(FlatBufferTable.class);
    FlatBufferTable myAnnotation = (FlatBufferTable) annotation;
    if (annotation instanceof FlatBufferTable) {
      logger.log(Level.FINE, "Declared root: " + myAnnotation.rootType());
      type.setRoot(myAnnotation.rootType());
      logger.log(Level.FINE, "Table structure: " + myAnnotation.value());
      type.setStructure(myAnnotation.value());
    } else {
      logger.log(Level.FINE, "Not FlatBufferTable (likely inner class); Generic table created");
    }

    Field[] fields = clazz.getDeclaredFields();
    for (Field field : fields) {
      if (field.getAnnotation(FlatBufferIgnore.class) != null) {
        logger.log(Level.FINE, "Ignoring property: " + field.getName());
        continue;
      }

      type.addProperty(getPropertyForField(field));
      logger.log(Level.FINE, "Adding property to Type: " + field.getName());
    }

    return type;
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
        logger.log(Level.FINE,
            "Array paramter is primative, wrapper, or String: " + field.getName());
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

    throw new IllegalArgumentException("Unable to parse: " + field.getType().getSimpleName());
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
