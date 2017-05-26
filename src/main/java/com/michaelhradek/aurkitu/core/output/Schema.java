/**
 * 
 */
package com.michaelhradek.aurkitu.core.output;

import java.util.ArrayList;
import java.util.List;

import com.michaelhradek.aurkitu.Config;

import lombok.Getter;
import lombok.Setter;

/**
 * @author m.hradek
 * @date May 23, 2017
 * 
 */
@Getter
@Setter
public class Schema {

  String name;
  String namespace;
  String rootType;
  List<EnumDeclaration> enums;
  List<TypeDeclaration> types;

  public Schema() {
    enums = new ArrayList<EnumDeclaration>();
    types = new ArrayList<TypeDeclaration>();
  }

  /**
   * 
   * @param input
   */
  public void addEnumDeclaration(EnumDeclaration input) {
    enums.add(input);
  }

  /**
   * 
   * @param input
   */
  public void addTypeDeclaration(TypeDeclaration input) {
    types.add(input);
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder(Config.SCHEMA_INTRO_COMMENT);
    builder.append(System.lineSeparator());
    builder.append(System.lineSeparator());

    builder.append("namespace: ");
    builder.append(namespace);
    builder.append(";");
    builder.append(System.lineSeparator());
    builder.append(System.lineSeparator());

    for (EnumDeclaration enumD : enums) {
      builder.append(enumD.toString());
    }

    for (TypeDeclaration typeD : types) {
      builder.append(typeD.toString());
    }

    builder.append("root_type: ");
    builder.append(rootType);
    builder.append(";");
    builder.append(System.lineSeparator());

    return builder.toString();
  }
}
