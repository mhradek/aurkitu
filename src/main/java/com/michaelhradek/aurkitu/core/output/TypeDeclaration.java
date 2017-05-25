/**
 * 
 */
package com.michaelhradek.aurkitu.core.output;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.michaelhradek.aurkitu.annotations.FlatBufferTable.TableStructureType;

import lombok.Getter;
import lombok.Setter;

/**
 * @author m.hradek
 * @date May 22, 2017
 * 
 */
@Getter
@Setter
public class TypeDeclaration {

  public String name;
  public boolean isRoot;
  public TableStructureType structure;
  public List<Property> properties = new ArrayList<Property>();

  public TypeDeclaration(TableStructureType structure) {
    this.structure = structure;
  }

  public TypeDeclaration() {
    this.structure = TableStructureType.TABLE;
  }

  /**
   * 
   * @param input
   */
  public void addProperty(Property input) {
    properties.add(input);
  }

  /**
   * 
   *
   */
  public static class Property {
    public String name;
    public FieldType type;
    public Map<String, String> options = new HashMap<String, String>();
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder(structure.name().toLowerCase());
    builder.append(" ");
    builder.append(name);
    builder.append(" {");
    builder.append(System.lineSeparator());

    for (Property property : properties) {
      builder.append("  ");
      builder.append(property.name);
      builder.append(":");
      if (property.type == FieldType.ARRAY) {
        builder.append("[");
        builder.append(property.options.get(FieldType.ARRAY.toString()));
        builder.append("]");
      } else {
        builder.append(property.type.toString());
      }

      if (!property.options.isEmpty()) {
        for (Entry<String, String> option : property.options.entrySet()) {
          if (option.getKey().equalsIgnoreCase(FieldType.ARRAY.toString())) {
            // Already grabbed this if we handled arrays above
            continue;
          }

          builder.append(" ");
          builder.append(option.getValue());
          builder.append(" ");
        }
      }

      builder.append(";");
      builder.append(System.lineSeparator());
    }

    builder.append("}");
    builder.append(System.lineSeparator());
    builder.append(System.lineSeparator());

    return builder.toString();
  }
}
