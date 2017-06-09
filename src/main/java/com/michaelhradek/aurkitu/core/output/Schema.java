/**
 * 
 */
package com.michaelhradek.aurkitu.core.output;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
  List<String> includes;
  List<String> attributes;

  public Schema() {
    enums = new ArrayList<EnumDeclaration>();
    types = new ArrayList<TypeDeclaration>();
    includes = new ArrayList<String>();
    attributes = new ArrayList<String>();
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

  /**
   * 
   * @param input
   */
  public void addInclude(String input) {
    includes.add(input);
  }

  /**
   * 
   * @param input
   */
  public void addAttribute(String input) {
    attributes.add(input);
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder(Config.SCHEMA_INTRO_COMMENT);
    builder.append(System.lineSeparator());
    builder.append("// @version: AURKITU-SCHEMA-VERSION-ghjtyu567FHGFD");
    builder.append(System.lineSeparator());
    builder.append(System.lineSeparator());

    if (includes.size() > 0) {
      for (String include : includes) {
        builder.append("include ");
        builder.append(include);
        builder.append(";");
        builder.append(System.lineSeparator());
      }

      builder.append(System.lineSeparator());
    }

    if (attributes.size() > 0) {
      for (String attribute : attributes) {
        builder.append("attribute \"");
        builder.append(attribute);
        builder.append("\"");
        builder.append(";");
        builder.append(System.lineSeparator());
      }

      builder.append(System.lineSeparator());
    }

    if (namespace != null) {
      builder.append("namespace: ");
      builder.append(namespace);
      builder.append(";");
      builder.append(System.lineSeparator());
      builder.append(System.lineSeparator());
    }

    for (EnumDeclaration enumD : enums) {
      builder.append(enumD.toString());
    }

    for (TypeDeclaration typeD : types) {
      builder.append(typeD.toString());
    }

    if (rootType != null) {
      builder.append("root_type: ");
      builder.append(rootType);
      builder.append(";");
      builder.append(System.lineSeparator());
    }

    String result = builder.toString();
    byte[] bytesOfResult = result.getBytes(StandardCharsets.UTF_8);

    MessageDigest md = null;
    try {
      md = MessageDigest.getInstance("MD5");
    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace();
    }
    String digest = String.format("%040x", new BigInteger(1, md.digest(bytesOfResult)));
    return result.replace("AURKITU-SCHEMA-VERSION-ghjtyu567FHGFD", digest);
  }
}
